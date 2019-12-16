import DB.DB._
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
      p <- DAO.allProxies.compile.toList.transact(db).attempt
    } yield ExitCode.Success

    //TODO check DB for existing ports to run servers on

    //Telegram-Bot
    //new TelegramProxyBot[IO]("887543781:AAGyGT0HW-Xr0wWNAKwhdMuPNw90tud1bNE")(db).startPolling.map(_ => ExitCode.Success)
  }
}
