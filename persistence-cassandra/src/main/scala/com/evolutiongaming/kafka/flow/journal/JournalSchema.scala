package com.evolutiongaming.kafka.flow.journal

import cats.Monad
import cats.syntax.all._
import com.evolutiongaming.cassandra.sync.CassandraSync
import com.evolutiongaming.kafka.journal.eventual.cassandra.CassandraSession
import com.evolutiongaming.scassandra

private[journal] trait JournalSchema[F[_]] {
  def create: F[Unit]

  def truncate: F[Unit]
}

private[journal] object JournalSchema {
  def apply[F[_]: Monad](
    session: CassandraSession[F],
    synchronize: CassandraSync[F]
  ): JournalSchema[F] = of(session.unsafe, synchronize)

  def of[F[_]: Monad](
    session: scassandra.CassandraSession[F],
    synchronize: CassandraSync[F]
  ): JournalSchema[F] = new JournalSchema[F] {
    def create: F[Unit] = synchronize("JournalSchema") {
      session
        .execute(
          """CREATE TABLE IF NOT EXISTS records(
          |application_id TEXT,
          |group_id TEXT,
          |topic TEXT,
          |partition INT,
          |key TEXT,
          |offset BIGINT,
          |created TIMESTAMP,
          |timestamp TIMESTAMP,
          |timestamp_type TEXT,
          |headers MAP<TEXT, TEXT>,
          |metadata TEXT,
          |value BLOB,
          |PRIMARY KEY((application_id, group_id, topic, partition, key), offset)
          |)
          |""".stripMargin
        )
        .void
    }

    def truncate: F[Unit] = synchronize("JournalSchema") {
      session.execute("TRUNCATE records").void
    }
  }

}
