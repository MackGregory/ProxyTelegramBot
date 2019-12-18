package services

import db.Proxy
import cats.effect.{Concurrent, ContextShift}
import cats.implicits._
import com.bot4s.telegram.api.declarative.{Commands, RegexCommands}
import com.bot4s.telegram.cats.{Polling, TelegramBot}
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import scala.util.Try

trait BotService[F[_]] {
  def run(): F[Unit]
}

class BotServiceImpl[F[_] : Concurrent : ContextShift]
(token: String,
 proxyService: ProxyService[F],
 dbService: DBService[F])
  extends TelegramBot[F](token, AsyncHttpClientCatsBackend())
    with Polling[F]
    with Commands[F]
    with RegexCommands[F]
    with BotService[F] {

  private def openPort(port: Int, targetHost: String, targetPort: Int): F[Unit] = {
    for {
      _ <- proxyService.startProxy(port, targetHost, targetPort)
      _ <- dbService.insertProxies(List(Proxy(port, targetHost, targetPort)))
    } yield ()
  }

  private def closePort(port: Int): F[Unit] = {
    for {
      _ <- proxyService.stopProxy(port)
      _ <- dbService.delete(port)
    } yield ()
  }

  // Extractor
  object Int {
    def unapply(s: String): Option[Int] = Try(s.toInt).toOption
  }

  onCommand("/help") {implicit msg => {
    reply("/proxies\n/openport <port> <target>:<target_port>\n/close <port>").void
  }}

  onCommand("/proxies") { implicit msg =>
    for {
      p <- dbService.getProxies
      _ <- reply(p.toString).void
    } yield ()
  }

  onRegex("""/openport ([0-9]+) (.+?):([0-9]+)""".r) ( implicit msg => {
    case Seq(Int(localPort), targetHost, Int(targetPort)) =>
      if (!proxyService.isOpen(localPort)) {
        for {
          _ <- openPort(localPort, targetHost, targetPort)
          _ <- reply(s"Started on port $localPort - target $targetHost:$targetPort.").void
        } yield ()
      }
      else
        reply(s"Port $localPort is already running.").void
    case _ =>
      reply("Wrong command format:\nUse /openport <local_port> <target_host>:<target_port>.").void
  })

  onRegex("""/close ([0-9]+)""".r) ( implicit msg => {
    case Seq(Int(port)) =>
      for {
        _ <- closePort(port)
        _ <- reply(s"Stopped server on port $port.").void
      } yield ()
    case _ =>
      reply("Wrong command format:\nUse /close <port>.").void
  })
}