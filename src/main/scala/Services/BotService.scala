package Services

import Bot.BotBase
import cats.effect.{Async, Concurrent, ContextShift, IO, Sync}
import cats.implicits._
import Services.DBService
import Services.ProxyService
import DB.Proxy
import com.bot4s.telegram.api.declarative.{Commands, RegexCommands}
import com.bot4s.telegram.cats.Polling
import monix.eval.Task

import scala.util.{Failure, Success, Try}

trait BotService[F[_]] {
  def run(): F[Unit]
}

final class BotServiceImpl[F[_] : Concurrent : ContextShift]
(token: String)
(proxyService: ProxyServiceImpl[F], dbService: DBServiceImpl[F])
  extends BotBase[F](token)
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

  onCommand("/test") {implicit msg => {
    reply("Test").void
  }}

  onCommand("/proxies") {implicit msg => {
    for {
      p <- dbService.getProxies
      _ <- reply(p.toString).void
    } yield ()
  }}

  onRegex("""/openport ([0-9]+) (.+?):([0-9]+)""".r) { implicit msg => {
    case Seq(Int(localPort), targetHost, Int(targetPort)) =>
      if (proxyService.socks.contains(localPort))
        reply(s"Server on port $localPort is already running.").void
      else {
        openPort(localPort, targetHost, targetPort)
        reply(s"Started on port $localPort - target $targetHost:$targetPort").void
      }
    case _ =>
      reply("Wrong command format:\nUse /openport <local_port> <target_host>:<target_port>.").void
  } }

  onRegex("""/close ([0-9]+)""".r) { implicit msg => {
    case Seq(Int(port)) =>
      for {
        _ <- closePort(port)
        _ <- reply(s"Stopped server on port $port").void
      } yield ()
    case _ =>
      reply("Wrong number of arguments:\nUse /close <port>").void
  } }
}