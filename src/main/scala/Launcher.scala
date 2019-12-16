import Bot.TelegramProxyBot
import DB.DB._
import ProxyServer.Proxy
import cats.effect.{ExitCode, IO, IOApp}
import doobie.Transactor
import doobie.implicits._

object Launcher extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    //DB
    val db = Transactor.fromDriverManager[IO](
      "org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", ""
    )
    for {
      a <- initDBAction.transact(db).attempt
      _ <- IO(println(a))
      proxies <- DAO.allProxies.compile.toList.transact(db)
      test = proxies.map{
        proxy =>
          Proxy.run(List(proxy.localPort.toString, proxy.targetHost, proxy.targetPort.toString)).unsafeRunAsync(_ => ())
      }
      bot = new TelegramProxyBot[IO]("887543781:AAGyGT0HW-Xr0wWNAKwhdMuPNw90tud1bNE")(db).startPolling.map(_ => ExitCode.Success)
    } yield ExitCode.Success
  }
}
