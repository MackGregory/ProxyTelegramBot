package Bot

import cats.effect.{Async, ContextShift, Timer}
import cats.syntax.functor._
import com.bot4s.telegram.api.declarative.{Commands, RegexCommands}
import com.bot4s.telegram.cats.Polling

import scala.util.Try

class TelegramProxyBot[F[_]: Async: Timer : ContextShift](token: String) extends BotBase[F](token)
  with Polling[F]
  with Commands[F]
  with RegexCommands[F] {

  // Extractor
  object Int {
    def unapply(s: String): Option[Int] = Try(s.toInt).toOption
  }

  println("bot started")

  onCommand("/hello") {implicit msg => {
    println("hello")
    reply("HELLO").void
  }}

//  onRegex("""/open ([0-9]+) (.+?):([0-9]+)""".r) { implicit msg => {
//    case Seq(Int(localPort), targetHost, Int(targetPort)) =>
//      if (proxyService.socks.contains(localPort))
//        reply(s"Server on port $localPort is already running.").void
//      else {
//        //proxyService.runProxy(localPort, targetHost, targetPort).void
//        reply(s"Started on port $localPort.").void
//      }
//    case _ =>
//      reply("Wrong command format:\nUse /openport <local_port> <target_host>:<target_port>.").void
//  } }

//  onRegex("""/close ([0-9]+)""".r) { implicit msg => {
//    case Seq(Int(port)) =>
//      proxyService.socks.get(port).foreach(_.close)
//      proxyService.socks -= port
//      Try {
//        dbService.delete(port).unsafeRunAsyncAndForget()
//      } match {
//        case Success(_) =>
//          reply(s"Stopped server on port $port").void
//        case Failure(e) =>
//          reply(s"${e.toString}").void
//      }
//    case _ =>
//      reply("Wrong number of arguments:\nUse /close <port>").void
//  } }

}

