package db

import cats.Show

final case class Proxy(localPort: Int, targetHost: String, targetPort: Int)
object Proxy {
  implicit val show: Show[Proxy] = Show.fromToString
}