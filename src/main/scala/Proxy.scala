import java.net.{InetAddress, InetSocketAddress}

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.implicits._
import fs2.Stream
import fs2.io.tcp.SocketGroup

object Proxy extends IOApp {

  val mkSocketGroup: Stream[IO, SocketGroup] =
    Stream.resource(Blocker[IO].flatMap(blocker => SocketGroup[IO](blocker)))

  val proxy: SocketGroup => Stream[IO, Unit] = socketGroup => {
    val client = socketGroup.client[IO](new InetSocketAddress(InetAddress.getByName("localhost"), 8080))

    val server = socketGroup
      .serverWithLocalAddress[IO](new InetSocketAddress(7766))
      .flatMap {
        case Left(local) =>
          Stream.eval_(IO.pure(println(s"Started proxy server on $local")))
        case Right(s) =>
          Stream.resource(s).zip(Stream.resource(client)).map { case (serverSocket, clientSocket) =>
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

  def run(args: List[String]): IO[ExitCode] =
    mkSocketGroup.flatMap(proxy).compile.toVector.as(ExitCode.Success)
}
