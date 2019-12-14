package Bot

import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.cats.Polling
import com.bot4s.telegram.methods._
import com.bot4s.telegram.models._
import monix.eval.Task

import scala.util.Try

class EchoBot(token: String) extends ExampleBot(token)
  with Polling[Task]
  with Commands[Task]
{
  // Extractor
  object Int {
    def unapply(s: String): Option[Int] = Try(s.toInt).toOption
  }

  // String commands.
  onCommand("/hello") { implicit msg =>
    reply("Hello America!").void
  }

  // '/' prefix is optional
  onCommand("hola") { implicit msg =>
    reply("Hola Mundo!").void
  }

  // Several commands can share the same handler.
  // Shows the 'using' extension to extract information from messages.
  onCommand("/hallo" | "/bonjour" | "/ciao" | "/hola") {
    implicit msg =>
      using(_.from) { // sender
        user =>
          reply(s"Hello ${user.firstName} from Europe?").void
      }
  }

  // withArgs with pattern matching.
  onCommand("/inc") { implicit msg =>
    withArgs {
      case Seq(Int(i)) =>
        reply("" + (i + 1)).void

      // Conveniently avoid MatchError, providing hints on usage.
      case _ =>
        reply("Invalid argument. Usage: /inc 123").void
    }
  }
}