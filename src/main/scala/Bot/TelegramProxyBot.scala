package Bot

import DB.DB.DAO
import ProxyServer._
import cats.effect.{Async, ContextShift, IO, Timer}
import cats.syntax.functor._
import com.bot4s.telegram.api.declarative.{Commands, RegexCommands}
import com.bot4s.telegram.cats.Polling
import doobie.implicits._
import doobie.util.transactor.Transactor

import scala.util.{Failure, Success, Try}

class TelegramProxyBot[F[_]: Async: Timer : ContextShift](token: String)(db: Transactor.Aux[IO, Unit]) extends BotBase[F](token)
  with Polling[F]
  with Commands[F]
  with RegexCommands[F] {

  // Extractor
  object Int {
    def unapply(s: String): Option[Int] = Try(s.toInt).toOption
  }

  onRegex("""/open ([0-9]+) (\.+):([0-9]+)""".r) { implicit msg => {
    case Seq(Int(localPort), targetHost, Int(targetPort)) =>
      if (Running.socks.contains(localPort))
        reply(s"Server on port $localPort is already running.").void
      else {
        Proxy.run(List(localPort.toString, targetHost, targetPort.toString)).unsafeRunAsync(_ => ())
        reply(s"Started on port $localPort.").void
      }
    case _ =>
      reply("Wrong command format:\nUse /openport <local_port> <target_host>:<target_port>.").void
  } }

  onRegex("""/close ([0-9]+)""".r) { implicit msg => {
    case Seq(Int(port)) =>
      Running.socks -= port
      Try {
        DAO.delete(port).transact(db).attempt.unsafeRunAsync(_=>())
      } match {
        case Success(_) =>
          reply(s"Stopped server on port $port").void
        case Failure(e) =>
          reply(s"${e.toString}").void
      }
    case _ =>
      reply("Wrong number of arguments:\nUse /close <port>").void
  } }

  //  onCommand("/stat") { implicit msg =>
  //    withArgs { args =>
  //
  //    }
  //    reply("Some response...").void
  //  }
}

