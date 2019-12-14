import Bot.EchoBot
import Proxy.{mkSocketGroup, proxy}
import cats.effect.{ExitCode, IO, IOApp}
import doobie.Transactor
import cats.implicits._

object Main extends IOApp{
  def run(args: List[String]): IO[ExitCode] = {
    //DB
    val db = Transactor.fromDriverManager[IO](
      "org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", ""
    )
    //Telegram-Bot
    new EchoBot("887543781:AAGyGT0HW-Xr0wWNAKwhdMuPNw90tud1bNE").run.as(ExitCode.Success)
    //Proxy-Server
    mkSocketGroup.flatMap(proxy).compile.toVector.as(ExitCode.Success)
  }
}
