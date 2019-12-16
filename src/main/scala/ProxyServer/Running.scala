package ProxyServer

import cats.effect.IO
import fs2.io.tcp.Socket

object Running {
  val socks: scala.collection.mutable.Map[Int, Socket[IO]] = scala.collection.mutable.Map.empty
}
