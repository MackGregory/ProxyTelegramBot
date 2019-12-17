package DB

import doobie.implicits._
import DB._
import cats.Monad
import cats.effect.{Bracket, IO}
import cats.implicits._
import doobie.util.transactor.Transactor

trait DBService[F[_]] {
  def getProxies: IO[List[Proxy]]
  def get(port: Int): IO[Proxy]
  def insertProxies(ps: List[Proxy]): IO[Unit]
  def create: IO[Unit]
  def delete(port: Int): IO[Unit]
}

class DBServiceImpl[F[_]](db: Transactor.Aux[IO, Unit])(implicit F: Monad[F]) extends DBService[F] {
  override def getProxies: IO[List[Proxy]] =
    DAO.allProxies.transact(db).compile.toList

  override def get(port: Int): IO[Proxy] =
    DAO.get(port).transact(db).compile.toList.map(_.head)

  override def insertProxies(ps: List[Proxy]): IO[Unit] =
    DAO.insertProxies(ps).transact(db).void

  override def create: IO[Unit] =
    DAO.create.transact(db).void

  override def delete(port: Int): IO[Unit] =
    DAO.delete(port).transact(db).void
}
