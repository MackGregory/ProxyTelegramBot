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

trait BotService[F[_]]

final class BotServiceImpl[F[_] : Sync : Async : ContextShift : Concurrent : Task]
(token: String)
(proxyService: ProxyServiceImpl[F], dbService: DBServiceImpl[F])
  extends BotBase[Task](token)
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

  onCommand("/hello") {implicit msg => {
    reply("HELLO").void
  }}

  onRegex("""/openport ([0-9]+) (.+?):([0-9]+)""".r) { implicit msg => {
    case Seq(Int(localPort), targetHost, Int(targetPort)) =>
      if (proxyService.socks.contains(localPort))
        reply(s"Server on port $localPort is already running.").void
      else {
        reply(s"Started on port $localPort - target $targetHost:$targetPort").void
        openPort(localPort, targetHost, targetPort)
      }
    case _ =>
      reply("Wrong command format:\nUse /openport <local_port> <target_host>:<target_port>.").void
  } }

  onRegex("""/close ([0-9]+)""".r) { implicit msg => {
    case Seq(Int(port)) =>
      reply(s"Stopped server on port $port").void
      closePort(port)
    case _ =>
      reply("Wrong number of arguments:\nUse /close <port>").void
  } }
}