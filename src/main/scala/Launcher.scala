import cats.effect.ExitCode
import monix.eval.{Task, TaskApp}
import monix.execution.Scheduler
import services._
import db._
import doobie.util.transactor.Transactor
import monix.execution.schedulers.SchedulerService

import scala.concurrent.duration.SECONDS
import scala.concurrent.duration.Duration

object Launcher extends TaskApp {
  implicit val sc: SchedulerService = Scheduler.io("proxy")

  def runProxies(proxyService: ProxyServiceImpl[Task], proxies: List[Proxy]): Unit =
    proxies.foreach { proxy =>
      proxyService.startProxy(proxy.localPort, proxy.targetHost, proxy.targetPort).runAsyncAndForget
    }

  def run(args: List[String]): Task[ExitCode] = {
    for {
      _ <- Task.pure(println("Starting..."))

      dbService = new DBServiceImpl[Task]
      proxyService = new ProxyServiceImpl[Task]
      botService = new BotServiceImpl[Task](System.getProperties.getProperty("proxy.bot.token"), proxyService, dbService)

      _ <- dbService.create
      _ <- dbService.insertProxies(Proxy(7766, "www.google.com", 80) :: Nil)

      proxies <- dbService.getProxies
      _ = runProxies(proxyService, proxies)
      _ <- Task.sleep(Duration(3, SECONDS))
      _ <- botService.run()
    } yield ExitCode.Success
  }
}
