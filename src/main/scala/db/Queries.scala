package db

import doobie._
import doobie.implicits._

object Queries {
  val insertProxy: Update[Proxy] =
    Update[Proxy]("INSERT INTO proxy VALUES (?, ?, ?)", None)

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
}

