package ProxyServer

import java.net.{InetAddress, InetSocketAddress}

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.implicits._
import fs2.Stream
import fs2.io.tcp.SocketGroup

object Proxy extends IOApp {
  def proxyRun(port: Int, targetHost: String, targetPort: Int) = {
    val mkSocketGroup: Stream[IO, SocketGroup] =
      Stream.resource(Blocker[IO].flatMap(blocker => SocketGroup[IO](blocker)))

    val proxy: SocketGroup => Stream[IO, Unit] = socketGroup => {
      val client = socketGroup.client[IO](new InetSocketAddress(InetAddress.getByName(targetHost), targetPort))

      val server = socketGroup
        .serverWithLocalAddress[IO](new InetSocketAddress(port))
        .flatMap {
          case Left(local) =>
            Stream.eval_(IO.pure(println(s"Started proxy server on $local")))
          case Right(s) =>
            Stream.resource(s).zip(Stream.resource(client)).map { case (serverSocket, clientSocket) =>
              Running.socks += (port -> serverSocket)
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

    mkSocketGroup.flatMap(proxy).compile.toVector
  }

  def run(args: List[String]): IO[ExitCode] = {
    val List(localPort, tHost, tPort) = args
    for {
      proxy <- proxyRun(localPort.toInt, tHost, tPort.toInt).start
      _ <- IO.never.flatMap(_ => IO(println("done")))
    } yield ExitCode.Success
  }
}