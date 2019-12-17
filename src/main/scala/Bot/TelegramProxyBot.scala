//package Bot
//
//import DB.Proxy
//import Services.{DBServiceImpl, ProxyServiceImpl}
//import cats.effect.{Async, ContextShift, IO, Timer}
//import cats.syntax.functor._
//import com.bot4s.telegram.api.declarative.{Commands, RegexCommands}
//import com.bot4s.telegram.cats.Polling
//
//import scala.util.{Failure, Success, Try}
//
//class TelegramProxyBot[F[_] : Async : Timer : ContextShift]
//(token: String)
//(proxyService: ProxyServiceImpl[F],
// dbService: DBServiceImpl[F])
//  extends BotBase[F](token)
//    with Polling[F]
//    with Commands[F]
//    with RegexCommands[F] {
//
//  // Extractor
//  object Int {
//    def unapply(s: String): Option[Int] = Try(s.toInt).toOption
//  }
//
//  private def openPort(port: Int, targetHost: String, targetPort: Int): F[Unit] = {
//    for {
//      _ <- proxyService.runProxy(port, targetHost, targetPort)
//      _ <- dbService.insertProxies(List(Proxy(port, targetHost, targetPort)))
//    } yield ()
//    ???
//  }
//
//  private def closePort(port: Int): F[Unit] = {
//    for {
//      _ <- proxyService.stopProxy(port)
//      _ <- dbService.delete(port)
//    } yield ()
//  }
//
//  onCommand("/hello") {implicit msg => {
//    reply("HELLO").void
//  }}
//
//  onRegex("""/openport ([0-9]+) (.+?):([0-9]+)""".r) { implicit msg => {
//    case Seq(Int(localPort), targetHost, Int(targetPort)) =>
//      if (proxyService.socks.contains(localPort))
//        reply(s"Server on port $localPort is already running.").void
//      else {
//        proxyService.runProxy(localPort, targetHost, targetPort).void.unsafeRunAsyncAndForget()
//        reply(s"Started on port $localPort - target $targetHost:$targetPort").void
//      }
//    case _ =>
//      reply("Wrong command format:\nUse /openport <local_port> <target_host>:<target_port>.").void
//  } }
//
//  onRegex("""/close ([0-9]+)""".r) { implicit msg => {
//    case Seq(Int(port)) =>
//      proxyService.stopProxy(port)
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
//
//}
//
