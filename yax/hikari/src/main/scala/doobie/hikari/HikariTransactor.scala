package doobie.hikari

import com.zaxxer.hikari.HikariDataSource
import doobie.imports._

#+scalaz
import scalaz.{ Catchable, Monad }
import scalaz.syntax.monad._
#-scalaz
#+cats
import cats.implicits._
import fs2.interop.cats._
#-cats
#+fs2
import fs2.util.{ Catchable, Suspendable }
#-fs2

object hikaritransactor {

  /** A `Transactor` backed by a `HikariDataSource`. */
#+scalaz
  final class HikariTransactor[M[_]: Monad : Catchable : Capture] private (ds: HikariDataSource) extends Transactor[M] {

    protected[doobie] val connect = Capture[M].apply(ds.getConnection)

    /** A program that shuts down this `HikariTransactor`. */
    val shutdown: M[Unit] = Capture[M].apply(ds.close)
#-scalaz
#+fs2
  final class HikariTransactor[M[_]: Catchable: Suspendable] private (ds: HikariDataSource) extends Transactor[M] {

    private val L = Predef.implicitly[Suspendable[M]]

    protected[doobie] val connect = L.delay(ds.getConnection)

    /** A program that shuts down this `HikariTransactor`. */
    val shutdown: M[Unit] = L.delay(ds.close)
#-fs2

    /** Constructs a program that configures the underlying `HikariDataSource`. */
    def configure(f: HikariDataSource => M[Unit]): M[Unit] = f(ds)

  }

  object HikariTransactor {

#+scalaz
    /** Constructs a program that yields an unconfigured `HikariTransactor`. */
    def initial[M[_]: Monad : Catchable: Capture]: M[HikariTransactor[M]] =
      Capture[M].apply(new HikariTransactor(new HikariDataSource))

    /** Constructs a program that yields a `HikariTransactor` from an existing `HikariDatasource`. */
    def apply[M[_]: Monad : Catchable: Capture](hikariDataSource : HikariDataSource): HikariTransactor[M] = 
      new HikariTransactor(hikariDataSource)

    /** Constructs a program that yields a `HikariTransactor` configured with the given info. */
    @deprecated("doesn't load driver properly; will go away in 0.2.2; use 4-arg version", "0.2.1")
    def apply[M[_]: Monad : Catchable : Capture](url: String, user: String, pass: String): M[HikariTransactor[M]] =
      apply("java.lang.String", url, user, pass)

    /** Constructs a program that yields a `HikariTransactor` configured with the given info. */
    def apply[M[_]: Monad : Catchable : Capture](driverClassName: String, url: String, user: String, pass: String): M[HikariTransactor[M]] =
      for {
        _ <- Capture[M].apply(Class.forName(driverClassName))
        t <- initial[M]
        _ <- t.configure(ds => Capture[M].apply {
          ds setJdbcUrl  url
          ds setUsername user
          ds setPassword pass
        })
      } yield t
#-scalaz
#+fs2
    /** Constructs a program that yields an unconfigured `HikariTransactor`. */
    def initial[M[_]: Catchable](implicit e: Suspendable[M]): M[HikariTransactor[M]] =
      e.delay(new HikariTransactor(new HikariDataSource))

    /** Constructs a program that yields a `HikariTransactor` from an existing `HikariDatasource`. */
    def apply[M[_]: Catchable: Suspendable](hikariDataSource : HikariDataSource): HikariTransactor[M] =
      new HikariTransactor(hikariDataSource)

    /** Constructs a program that yields a `HikariTransactor` configured with the given info. */
    @deprecated("doesn't load driver properly; will go away in 0.2.2; use 4-arg version", "0.2.1")
    def apply[M[_]: Catchable: Suspendable](url: String, user: String, pass: String): M[HikariTransactor[M]] =
      apply("java.lang.String", url, user, pass)

    /** Constructs a program that yields a `HikariTransactor` configured with the given info. */
    def apply[M[_]: Catchable](driverClassName: String, url: String, user: String, pass: String)(implicit e: Suspendable[M]): M[HikariTransactor[M]] =
      for {
        _ <- e.delay(Class.forName(driverClassName))
        t <- initial[M]
        _ <- t.configure(ds => e.delay {
          ds setJdbcUrl  url
          ds setUsername user
          ds setPassword pass
        })
      } yield t
#-fs2

  }

}
