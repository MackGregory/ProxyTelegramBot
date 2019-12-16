package ProxyServer

import java.net.{InetAddress, InetSocketAddress}

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.implicits._
import fs2.Stream
import fs2.io.tcp.SocketGroup

class Proxy extends IOApp {

  val mkSocketGroup: Stream[IO, SocketGroup] =
    Stream.resource(Blocker[IO].flatMap(blocker => SocketGroup[IO](blocker)))

  def proxy(localPort: Int, targetHost: String, targetPort: Int): SocketGroup => Stream[IO, Unit] = socketGroup => {
    val target = socketGroup.client[IO](new InetSocketAddress(InetAddress.getByName(targetHost), targetPort))

    val server = socketGroup
      .serverWithLocalAddress[IO](new InetSocketAddress(localPort))
      .flatMap {
        case Left(local) =>
          Stream.eval_(IO.pure(println(s"Started proxy server on $local")))
        case Right(s) =>
          Stream.resource(s).zip(Stream.resource(target)).map { case (serverSocket, clientSocket) =>
            serverSocket
              .reads(1024)
              .through(clientSocket.writes())
              .flatMap(_ => clientSocket.reads(1024))
              .through(serverSocket.writes())
              .onFinalize(serverSocket.endOfOutput >> clientSocket.endOfOutput)
          }
      }
      .parJoinUnbounded

    server
  }

  def run(args: List[String]): IO[ExitCode] = {
    args match {
      case List(localPort, targetHost, targetPort) =>
        mkSocketGroup.flatMap(proxy(localPort.toInt, targetHost, targetPort.toInt)).compile.toVector.as(ExitCode.Success)
      case _ =>
        IO(ExitCode.Error)
    }
  }
}
