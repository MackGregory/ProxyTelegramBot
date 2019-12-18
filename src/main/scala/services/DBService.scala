package services

import db._
import cats.Monad
import cats.effect.{Concurrent, ContextShift}
import doobie.util.transactor.Transactor
import doobie.implicits._
import cats.implicits._
import config.DBConfig
import doobie.util.transactor.Transactor.Aux

trait DBService[F[_]] {
  def getProxies: F[List[Proxy]]
  def get(port: Int): F[Proxy]
  def insertProxy(p: Proxy): F[Unit]
  def insertProxies(ps: List[Proxy]): F[Unit]
  def create: F[Unit]
  def delete(port: Int): F[Unit]
}

class DBServiceImpl[F[_]: Concurrent: ContextShift](dbConfig: DBConfig)(implicit F: Monad[F]) extends DBService[F] {
  val db: Aux[F, Unit] = Transactor.fromDriverManager[F](
    dbConfig.dbDriver,
    dbConfig.dbUrl,
    dbConfig.dbUser,
    dbConfig.dbPass
  )

  override def getProxies: F[List[Proxy]] =
    Queries.allProxies.stream.transact(db).compile.toList

  override def get(port: Int): F[Proxy] =
    Queries.get(port).stream.transact(db).compile.toList.map(_.head)

  override def insertProxy(p: Proxy): F[Unit] =
    Queries.insertProxy.run(p).transact(db).void

  override def insertProxies(ps: List[Proxy]): F[Unit] =
    Queries.insertProxy.updateMany(ps).transact(db).void

  override def create: F[Unit] =
    Queries.create.run.void.transact(db).void

  override def delete(port: Int): F[Unit] =
    Queries.delete(port).run.void.transact(db).void
}
