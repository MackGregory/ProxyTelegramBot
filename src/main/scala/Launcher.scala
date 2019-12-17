import cats.effect.ExitCode
import monix.eval.{Task, TaskApp}
import monix.execution.Scheduler
import Services._
import DB._
import doobie.util.transactor.Transactor

import scala.concurrent.duration.SECONDS
import scala.concurrent.duration.Duration

object Launcher extends TaskApp {
  implicit val sc = Scheduler.io("proxy")

  def runProxies(proxyService: ProxyServiceImpl[Task], proxies: List[Proxy]): Unit =
    proxies.foreach { proxy =>
      proxyService.startProxy(proxy.localPort, proxy.targetHost, proxy.targetPort).runAsyncAndForget
    }

  val token = "887543781:AAGyGT0HW-Xr0wWNAKwhdMuPNw90tud1bNE"

  def run(args: List[String]): Task[ExitCode] = {
    val db = Transactor.fromDriverManager[Task](
      "org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", ""
    )
    for {
      _ <- Task.pure(println("Starting app..."))
      dbService = new DBServiceImpl[Task](db)
      proxyService = new ProxyServiceImpl[Task]
      botService = new BotServiceImpl[Task](token)(proxyService, dbService)

      _ <- dbService.create
      _ <- dbService.insertProxies(Proxy(7766, "www.google.com", 80) :: Nil)
      proxies <- dbService.getProxies
      _ = runProxies(proxyService, proxies)
      _ <- Task.sleep(Duration(3, SECONDS))
      _ = println("123")
      _ <- Task.sleep(Duration(3, SECONDS))
      _ <- Task.never[Unit]
      _ <- botService.run()
    } yield ExitCode.Success

//    val dbService = new DBServiceImpl[IO](db)
//    dbService.create.void.unsafeRunSync()
//    dbService.insertProxies(List(DB.Proxy(7766, "www.google.com", 80))).void.unsafeRunSync()
//    println(dbService.getProxies.unsafeRunSync())
//    val proxyService = new ProxyServiceImpl[IO]
//    dbService.getProxies.unsafeRunSync().foreach(p => proxyService.runProxy(p.localPort, p.targetHost, p.targetPort).void.unsafeRunAsyncAndForget())
//    new TelegramProxyBot[IO]("887543781:AAGyGT0HW-Xr0wWNAKwhdMuPNw90tud1bNE")(proxyService, dbService).startPolling().map(_=>ExitCode.Success)
//
  }
}
