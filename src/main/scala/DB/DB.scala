package DB

import cats.Show
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import doobie._
import doobie.implicits._
import fs2.Stream


object DB extends IOApp {

  // Our data model
  final case class Proxy(id: Int, localPort: String, targetHost: String, targetPort: String)
  object Proxy {
    implicit val show: Show[Proxy] = Show.fromToString
  }
  final case class ProxyStats(id: Int, sent: Int, received: Int)
  object ProxyStats {
    implicit val show: Show[ProxyStats] = Show.fromToString
  }

  val proxies = List(
    Proxy(1, "5000", "https://www.google.com/", "5100"),
    Proxy(2, "5001", "192.168.0.40",            "5200")
  )

  val stats = List(
    ProxyStats(1, 0, 0),
    ProxyStats(2, 0, 0)
  )

  // Our example database action
  def exampleDBAction: ConnectionIO[String] =
    for {
      // Create and populate
      _  <- DAO.create
      np <- DAO.insertProxies(proxies)
      ns <- DAO.insertProxyStats(stats)
      _  <- putStrLn(show"Inserted $np proxies.")

      // Select and stream the coffees to stdout
      _ <- DAO.allProxies.evalMap(c => putStrLn(show"$c")).compile.drain

      //delete
      _ <- DAO.delete(1)

      _ <- DAO.allProxies.evalMap(c => putStrLn(show"$c")).compile.drain

      //stats id=1
      _ <- DAO.stats(2).evalMap(stat => putStrLn(show"$stat")).compile.drain
    } yield "All done!"

  // Entry point for SafeApp
  def run(args: List[String]): IO[ExitCode] = {
    val db = Transactor.fromDriverManager[IO](
      "org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", ""
    )
    for {
      a <- exampleDBAction.transact(db).attempt
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

    def delete(id: Int): ConnectionIO[Unit] =
      Queries.delete(id).run.void

    def stats(id: Int): Stream[ConnectionIO, ProxyStats] =
      Queries.stats(id).stream
  }

  /** Queries module contains "raw" Query/Update values. */
  object Queries {
    val insertProxy: Update[Proxy] =
      Update[Proxy]("INSERT INTO proxy VALUES (?, ?, ?, ?)", None)

    def insertProxyStats =
      Update[ProxyStats]("INSERT INTO proxy_stats VALUES (?, ?, ?);", None)

    def allProxies: Query0[Proxy] =
      sql"SELECT proxy_id, local_port, target_host, target_port FROM proxy".query[Proxy]

    def create: Update0 =
      sql"""
        CREATE TABLE proxy (
          proxy_id    INTEGER NOT NULL PRIMARY KEY,
          local_port  INTEGER NOT NULL,
          target_host VARCHAR NOT NULL,
          target_port INTEGER NOT NULL
        );
        CREATE TABLE proxy_stats (
          proxy_id    INTEGER NOT NULL UNIQUE,
          sent        INTEGER NOT NULL,
          received    INTEGER NOT NULL
        );
        ALTER TABLE proxy_stats
        ADD CONSTRAINT proxy_id_fk FOREIGN KEY (proxy_id) REFERENCES proxy(proxy_id) ON DELETE CASCADE;
      """.update

    def delete(id: Int): Update0 =
      sql"""
        DELETE FROM proxy WHERE proxy_id = $id;
        """.update

    def stats(id: Int): Query0[ProxyStats] =
      sql"""
        SELECT proxy_id, sent, received FROM proxy_stats WHERE proxy_id = $id;
        """.query[ProxyStats]
  }

  // Lifted println
  def putStrLn(s: => String): ConnectionIO[Unit] =
    FC.delay(println(s))
}
