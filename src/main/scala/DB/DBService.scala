package DB

import DB.DAO
import cats.Monad
import cats.effect.Bracket
import cats.implicits._
import doobie.implicits._
import doobie.util.transactor.Transactor

trait DBService[F[_]] {
  def getProxies: F[List[DB.Proxy]]
  def get(port: Int): F[DB.Proxy]
  def insertProxies(ps: List[DB.Proxy]): F[Unit]
  def create: F[Unit]
  def delete(port: Int): F[Unit]
}

class DBServiceImpl[F[_]](db: Transactor.Aux[F, Unit])(implicit F: Monad[F], b: Bracket[F, Throwable]) extends DBService[F] {
  override def getProxies: F[List[DB.Proxy]] =
    DAO.allProxies.transact(db).compile.toList

  override def get(port: Int): F[DB.Proxy] =
    DAO.get(port).transact(db).compile.toList.map((x: List[DB.Proxy]) => x.head)

  override def insertProxies(ps: List[DB.Proxy]): F[Unit] =
    DAO.insertProxies(ps).transact(db).void

  override def create: F[Unit] =
    DAO.create.transact(db).void

  override def delete(port: Int): F[Unit] =
    DAO.delete(port).transact(db).void
}
