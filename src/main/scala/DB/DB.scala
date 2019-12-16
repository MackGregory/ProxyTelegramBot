package DB

import cats.Show
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import doobie._
import doobie.implicits._
import fs2.Stream


object DB extends IOApp {

  // Our data model
  final case class Proxy(localPort: Int, targetHost: String, targetPort: Int)
  object Proxy {
    implicit val show: Show[Proxy] = Show.fromToString
  }
  final case class ProxyStats(port: Int, sent: Int, received: Int)
  object ProxyStats {
    implicit val show: Show[ProxyStats] = Show.fromToString
  }

  val proxies = List(
    Proxy(7766, "www.google.com", 80)
  )


  // Our example database action
  def initDBAction: ConnectionIO[String] =
    for {
      // Create and populate
      _  <- DAO.create
      //np <- DAO.insertProxies(proxies)
      //_  <- putStrLn(show"Inserted $np proxies.")

      // Select and stream the coffees to stdout
      //_ <- DAO.allProxies.evalMap(c => putStrLn(show"$c")).compile.drain
    } yield "Init DB done."

  // Entry point for SafeApp
  def run(args: List[String]): IO[ExitCode] = {
    val db = Transactor.fromDriverManager[IO](
      "org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", ""
    )
    for {
      a <- initDBAction.transact(db).attempt
      _ <- IO(println(a))
    } yield ExitCode.Success
  }

  /** DAO module provides ConnectionIO constructors for end users. */
  object DAO {
    def insertProxies(ps: List[Proxy]): ConnectionIO[Int] =
      Queries.insertProxy.updateMany(ps)

    def insertProxyStats(ps: List[ProxyStats]): ConnectionIO[Int] =
      Queries.insertProxyStats.updateMany(ps)

    def allProxies: Stream[ConnectionIO, Proxy] =
      Queries.allProxies.stream

    def create: ConnectionIO[Unit] =
      Queries.create.run.void

    def delete(port: Int): ConnectionIO[Unit] =
      Queries.delete(port).run.void

    def get(port: Int): Stream[ConnectionIO, Proxy] =
      Queries.get(port).stream

    def stats(port: Int): Stream[ConnectionIO, ProxyStats] =
      Queries.stats(port).stream
  }

  /** Queries module contains "raw" Query/Update values. */
  object Queries {
    val insertProxy: Update[Proxy] =
      Update[Proxy]("INSERT INTO proxy VALUES (?, ?, ?)", None)

    def insertProxyStats =
      Update[ProxyStats]("INSERT INTO proxy_stats VALUES (?, ?, ?);", None)

    def allProxies: Query0[Proxy] =
      sql"SELECT local_port, target_host, target_port FROM proxy".query[Proxy]

    def create: Update0 =
      sql"""
        CREATE TABLE proxy (
          local_port  INTEGER NOT NULL PRIMARY KEY,
          target_host VARCHAR NOT NULL,
          target_port INTEGER NOT NULL
        );
        CREATE TABLE proxy_stats (
          port        INTEGER NOT NULL,
          sent        INTEGER NOT NULL,
          received    INTEGER NOT NULL
        );
        ALTER TABLE proxy_stats
        ADD CONSTRAINT port_fk FOREIGN KEY (port) REFERENCES proxy(local_port) ON DELETE CASCADE;
      """.update

    def delete(port: Int): Update0 =
      sql"""
        DELETE FROM proxy WHERE local_port = $port;
        """.update

    def get(port: Int): Query0[Proxy] =
      sql"""
        SELECT local_port, target_host, target_port FROM proxy WHERE local_port = $port;
        """.query[Proxy]

    def stats(port: Int): Query0[ProxyStats] =
      sql"""
        SELECT port, sent, received FROM proxy_stats WHERE port = $port;
        """.query[ProxyStats]
  }

  // Lifted println
  def putStrLn(s: => String): ConnectionIO[Unit] =
    FC.delay(println(s))
}
