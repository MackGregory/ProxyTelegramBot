import Bot.TelegramProxyBot
import cats.effect.{ExitCode, IO, IOApp}
import doobie.Transactor

object Launcher extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    //DB
    val db = Transactor.fromDriverManager[IO](
      "org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", ""
    )
//    for {
//      _ <- IO.pure(println("Starting ..."))
//      dbService = new DBService[IO](db)
//      _ <- dbService.create
//      proxyService = new ProxyServiceImpl[IO]
//      //botService = new TelegramProxyBot[IO]("887543781:AAGyGT0HW-Xr0wWNAKwhdMuPNw90tud1bNE", dbService, proxyService).startPolling().map(_=>ExitCode.Success)
//      proxies <- dbService.getProxies
//      _ = proxies.foreach( p => proxyService.runProxy(p.localPort, p.targetHost, p.targetPort).unsafeRunAsyncAndForget())
//      _ <- IO.never
//    } yield ExitCode.Success
//    val dbService = new DBService[IO](db)
//    val proxyService = new ProxyServiceImpl[IO]
    new TelegramProxyBot[IO]("887543781:AAGyGT0HW-Xr0wWNAKwhdMuPNw90tud1bNE").startPolling().map(_=>ExitCode.Success)
  }
}
