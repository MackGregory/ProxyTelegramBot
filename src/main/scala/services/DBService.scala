package services

import db._
import cats.Monad
import cats.effect.{Concurrent, ContextShift}
import doobie.util.transactor.Transactor
import doobie.implicits._
import cats.implicits._
import doobie.util.transactor.Transactor.Aux

trait DBService[F[_]] {
  def getProxies: F[List[Proxy]]
  def get(port: Int): F[Proxy]
  def insertProxies(ps: List[Proxy]): F[Unit]
  def create: F[Unit]
  def delete(port: Int): F[Unit]
}

final class DBServiceImpl[F[_]: Concurrent: ContextShift](implicit F: Monad[F]) extends DBService[F] {
  val db: Aux[F, Unit] = Transactor.fromDriverManager[F](
    System.getProperties.getProperty("db.driver"),
    System.getProperties.getProperty("db.url"),
    System.getProperties.getProperty("db.user"),
    System.getProperties.getProperty("db.pass")
  )

  override def getProxies: F[List[Proxy]] =
    Queries.allProxies.stream.transact(db).compile.toList

  override def get(port: Int): F[Proxy] =
    Queries.get(port).stream.transact(db).compile.toList.map(_.head)

  override def insertProxies(ps: List[Proxy]): F[Unit] =
    Queries.insertProxy.updateMany(ps).transact(db).void

  override def create: F[Unit] =
    Queries.create.run.void.transact(db).void

  override def delete(port: Int): F[Unit] =
    Queries.delete(port).run.void.transact(db).void
}
