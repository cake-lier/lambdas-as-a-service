/*
 * Copyright (c) 2023 Matteo Castellucci
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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

import org.mindrot.jbcrypt.BCrypt

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
      Future.delegate(
        if (ctx.run(query[Users].filter(_.username === lift(username))).nonEmpty) {
          Future.failed[Unit](IllegalArgumentException("The username is already taken, please choose another"))
        } else {
          Future(ctx.run(query[Users].insertValue(lift(Users(
            username,
            BCrypt.hashpw(password, BCrypt.gensalt(12))
          )))))
        }
      )

    override def login(username: String, password: String): Future[Boolean] =
      Future {
        ctx
          .run(query[Users].filter(_.username === lift(username)).map(_.password))
          .exists(BCrypt.checkpw(password, _))
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
