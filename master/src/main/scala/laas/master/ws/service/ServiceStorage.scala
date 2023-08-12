package io.github.cakelier
package laas.master.ws.service

import java.util.UUID
import javax.sql.DataSource

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import io.getquill.*

import laas.master.model.Executable.ExecutableId
import laas.master.model.User.DeployedExecutable
import AnyOps.*

trait ServiceStorage {

  def findByUsername(username: String): Future[Seq[DeployedExecutable]]

  def register(username: String, password: String): Future[Unit]

  def login(username: String, password: String): Future[Boolean]

  def isExecutableOfUser(username: String, id: ExecutableId): Future[Boolean]

  def addExecutableToUser(username: String, id: ExecutableId, fileName: String): Future[Unit]
}

object ServiceStorage {

  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  private class ServiceStorageImpl(ctx: PostgresJdbcContext[SnakeCase])(using ExecutionContext) extends ServiceStorage {

    import ctx.*

    private case class Users(username: String, password: String)

    private case class Executables(id: String, name: String, username: String)

    override def findByUsername(username: String): Future[Seq[DeployedExecutable]] =
      Future {
        ctx
          .run(query[Executables].filter(_.username === lift(username)))
          .map(r => DeployedExecutable(r.name, UUID.fromString(r.id)))
      }

    override def register(username: String, password: String): Future[Unit] =
      Future(ctx.run(query[Users].insertValue(lift(Users(username, password)))))

    override def login(username: String, password: String): Future[Boolean] =
      Future {
        ctx
          .run(query[Users].filter(u => u.username === lift(username) && u.password === lift(password)))
          .nonEmpty
      }

    override def isExecutableOfUser(username: String, id: ExecutableId): Future[Boolean] =
      Future {
        ctx
          .run(query[Executables].filter(e => e.username === lift(username) && e.id === lift(id.toString)))
          .nonEmpty
      }

    override def addExecutableToUser(username: String, id: ExecutableId, fileName: String): Future[Unit] =
      Future {
        ctx.run(query[Executables].insertValue(lift(Executables(id.toString, fileName, username))))
      }
  }

  def apply(dataSource: DataSource)(using ExecutionContext): ServiceStorage =
    ServiceStorageImpl(PostgresJdbcContext[SnakeCase](SnakeCase, dataSource))
}
