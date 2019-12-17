package Services

import DB.DB._
import DB.Proxy
import cats.Monad
import cats.effect.{Async, Bracket, Concurrent, IO, Sync}
import cats.implicits._
import cats.effect.implicits._
import doobie.util.transactor.Transactor
import doobie.implicits._
import monix.eval.Task

trait DBService[F[_]] {
  def getProxies: F[List[Proxy]]
  def get(port: Int): F[Proxy]
  def insertProxies(ps: List[Proxy]): F[Unit]
  def create: F[Unit]
  def delete(port: Int): F[Unit]
}

class DBServiceImpl[F[_]: Task : Concurrent : Sync : Async](db: Transactor.Aux[F, Unit])(implicit F: Monad[F]) extends DBService[F] {
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
