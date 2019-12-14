package Bot

import cats.effect._
import cats.implicits._
import monix.eval._

object Launcher extends TaskApp {

  def run(args: List[String]): Task[ExitCode] = {
    new EchoBot("887543781:AAGyGT0HW-Xr0wWNAKwhdMuPNw90tud1bNE").run.as(ExitCode.Success)
  }
}
