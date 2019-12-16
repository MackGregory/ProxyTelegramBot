package Bot

import DB.DB.DAO
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
      //TODO check DB if server is already running on <localPort>
      DAO.get(localPort).compile.toList.transact(db).attempt.map {
        case Right(proxy) => proxy.headOption
      }.runCancelable{
        case Right(value) => value match {
          case Some(_) =>
            IO(reply(s"Server on port $localPort is already running.").void)
          case None =>
            //???
            val serv = new ProxyServer.Proxy
            serv.run(List(localPort.toString, targetHost, targetPort.toString)).void.runAsync {
              case Left(e) =>
                IO(reply(s"${e.toString}").void)
              case Right(_) =>
                IO(reply(s"Server started on $localPort.").void)
            }.toIO
        }
        case Left(e) =>
          IO(reply(s"${e.toString}").void)
      }

      //TODO start server on localPort and add to DB


      reply("Some response...").void
    case _ =>
      reply("Wrong command format:\nUse /openport <local_port> <target_host>:<target_port>.").void
  } }

  onCommand("/openport") { implicit msg =>
    withArgs {
      case Seq(localPort, targetHostPort) =>
        Try {
          targetHostPort.split(":")
        } match {
          case Success(Array(targetHost, targetPort)) =>
//            DAO.get(localPort.toInt).compile.toList.map(_.headOption).transact(db).map {
//              case Some(_) =>
//                reply(s"Port $localPort is already running.").void
//              case None =>
//                //TODO start proxy-server concurrently
//                val server = new ProxyServer.Proxy
//                server.run(List(localPort, targetHost, targetPort))
//                //reply something according to result of proxy-server start attempt
//                reply("[TEMP] Some kind of reply.").void
//            }.unsafeRunSync()
            reply("TEMP MESSAGE.").void
          case Failure(_) =>
            reply("Wrong command format:\nUse /openport <local_port> <target_host>:<target_port>.").void
        }
      case _ =>
        reply("Wrong number of arguments:\nUse /openport <local_port> <target_host>:<target_port>.").void
      }
    }

  onRegex("""/close ([0-9]+)""".r) { implicit msg => {
    case Seq(Int(port)) =>
      //TODO stop server on port

      //and delete from DB
      Try {
        DAO.delete(port).transact(db).attempt.runAsync {
          case Right(v) => v match {
            case Right(_) =>
              IO(reply(s"Stopped server on port $port").void)
            case Left(e) =>
              IO(reply(s"${e.toString}").void)
          }
          case Left(e) =>
            IO(reply(s"${e.toString}").void)
        }

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

