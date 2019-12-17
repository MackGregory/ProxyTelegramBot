import Bot.TelegramProxyBot
import DB.{DBService, DBServiceImpl}
import ProxyServer.ProxyServiceImpl
import cats.effect.{ExitCode, IO, IOApp}
import doobie.Transactor
import cats.implicits._

object Launcher extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    val db = Transactor.fromDriverManager[IO](
      "org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", ""
    )
//    for {
//      _ <- IO.pure(println("Starting ..."))
//      dbService = new DBServiceImpl[IO](db)
//      _ <- dbService.create
//      proxyService = new ProxyServiceImpl[IO]
//      botService = new TelegramProxyBot[IO]("887543781:AAGyGT0HW-Xr0wWNAKwhdMuPNw90tud1bNE")(proxyService, dbService)
//      proxies <- dbService.getProxies
//      _ = proxies.foreach( p => proxyService.runProxy(p.localPort, p.targetHost, p.targetPort).unsafeRunAsyncAndForget())
//      _ <- IO.never
//      _ = botService.startPolling().map(_=>ExitCode.Success).unsafeRunAsyncAndForget()
//    } yield ExitCode.Success
    val dbService = new DBServiceImpl[IO](db)
    dbService.create.void.unsafeRunSync()
    dbService.insertProxies(List(DB.DB.Proxy(7766, "www.google.com", 80))).void.unsafeRunSync()
    println(dbService.getProxies.unsafeRunSync())
    val proxyService = new ProxyServiceImpl[IO]
    dbService.getProxies.unsafeRunSync().foreach(p => proxyService.runProxy(p.localPort, p.targetHost, p.targetPort).void.unsafeRunAsyncAndForget())
    new TelegramProxyBot[IO]("887543781:AAGyGT0HW-Xr0wWNAKwhdMuPNw90tud1bNE")(proxyService, dbService).startPolling().map(_=>ExitCode.Success)
  }
}
