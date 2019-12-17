package ProxyServer

import java.net.{InetAddress, InetSocketAddress}

import cats.Monad
import cats.effect.{Blocker, Concurrent, ContextShift, Sync}
import cats.implicits._
import fs2.Stream
import fs2.io.tcp.{Socket, SocketGroup}

trait ProxyService[F[_]] {
  def runProxy(port: Int, targetHost: String, targetPort: Int): F[Unit]
}

class ProxyServiceImpl[F[_] : Sync : ContextShift : Concurrent]()(implicit F: Monad[F]) extends ProxyService[F] {

  val socks: scala.collection.mutable.Map[Int, Socket[F]] = scala.collection.mutable.Map()

  override def runProxy(port: Int, targetHost: String, targetPort: Int): F[Unit] = {
    val mkSocketGroup: Stream[F, SocketGroup] =
      Stream.resource(Blocker[F].flatMap(blocker => SocketGroup[F](blocker)))

    val proxy: SocketGroup => Stream[F, Unit] = socketGroup => {
      val client = socketGroup.client[F](new InetSocketAddress(InetAddress.getByName(targetHost), targetPort))

      val server = socketGroup
        .serverWithLocalAddress[F](new InetSocketAddress(port))
        .flatMap {
          case Left(local) =>
            Stream.eval_(F.pure(println(s"Started proxy server on $local")))
          case Right(s) =>
            Stream.resource(s).zip(Stream.resource(client)).map { case (serverSocket, clientSocket) =>
              socks += (port -> serverSocket)
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

    mkSocketGroup.flatMap(proxy).compile.drain
  }
}