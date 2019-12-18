package services

import java.net.{InetAddress, InetSocketAddress}


import cats.Monad
import cats.effect.concurrent.Deferred
import cats.effect.syntax.all._
import cats.effect.{Blocker, Concurrent, ContextShift}
import cats.implicits._
import fs2.Stream
import fs2.io.tcp.SocketGroup

import scala.collection.mutable

trait ProxyService[F[_]] {
  def startProxy(port: Int, targetHost: String, targetPort: Int): F[Unit]
  def stopProxy(port: Int): F[Unit]
}

final class ProxyServiceImpl[F[_] : ContextShift : Concurrent]()(implicit F: Monad[F]) extends ProxyService[F] {
  @volatile var socks: mutable.Map[Int, Deferred[F, Unit]] = mutable.Map.empty

  override def startProxy(port: Int, targetHost: String, targetPort: Int): F[Unit] = {
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
              serverSocket
                .reads(1024)
                .through(clientSocket.writes())
                .flatMap(_ => clientSocket.reads(1024))
                .through(serverSocket.writes())
                .onFinalize(serverSocket.endOfOutput >> clientSocket.endOfOutput)
            }
        }
        .parJoinUnbounded

      Stream.eval(Deferred[F, Unit]).flatMap { switch =>
        socks += port -> switch
        server.interruptWhen(switch.get.attempt)
      }
    }

    mkSocketGroup
      .flatMap(proxy)
      .compile
      .drain
      .start
      .void
  }

  override def stopProxy(port: Int): F[Unit] =
    socks.get(port) match {
      case Some(switch) =>
        socks -= port
        switch.complete(())
      case None =>
        F.unit
    }
}