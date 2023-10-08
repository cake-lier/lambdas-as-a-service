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
import org.mindrot.jbcrypt.BCrypt

import laas.master.model.Executable.ExecutableId
import laas.master.model.User.DeployedExecutable
import AnyOps.*

/** The interface to the storage component of the web service.
  *
  * This trait represents the interface allowing the [[ServiceApi]] to communicate to the storage, so it exposes all the different
  * operations that can be performed on the storage. Each and every operation returns a [[Future]] because interacting with the
  * storage is a blocking operation that can take a long time to execute and this is incompatible with some architectures like the
  * one used for implementing actors.
  */
private[ws] trait ServiceStorage {

  /** Searches into the storage for the data associated to the user for which their username is given. The user data is made by
    * all the [[DeployedExecutable]]s that the user has previously deployed. If no such username is found, an empty [[Seq]] will
    * be returned.
    *
    * @param username
    *   the username of the user for which searching their user data
    * @return
    *   a [[Seq]] of all [[DeployedExecutable]]s that the user previously deployed, or an empty [[Seq]] if no username is found.
    *   The result is wrapped in a [[Future]] because the operation could be long-running and it can fail. In this case, the
    *   future will be failed with a [[Throwable]] explaining the error
    */
  def findByUsername(username: String): Future[Seq[DeployedExecutable]]

  /** Adds a user into the storage, given its username and its password. The username must not be already present into the
    * storage, because it is unique between all users registered in the system.
    *
    * @param username
    *   the username used by the user to register
    * @param password
    *   the password used by the user to register
    * @return
    *   a [[Future]], because the operation could be long-running and it can fail. In this case, the future will be failed with a
    *   [[Throwable]] explaining the error
    */
  def register(username: String, password: String): Future[Unit]

  /** Checks whether a user has already been added to the storage, given its username and its password. Both of these must match
    * with the same user which was previously been added to the storage through a [[register]] operation.
    *
    * @param username
    *   the username used by the user to log in
    * @param password
    *   the password used by the user to log in
    * @return
    *   <code>true</code> if the given username and password match with a user previously inserted, <code>false</code> otherwise.
    *   The result is wrapped in a [[Future]] because the operation could be long-running and it can fail. In this case, the
    *   future will be failed with a [[Throwable]] explaining the error
    */
  def login(username: String, password: String): Future[Boolean]

  /** Checks whether an executable has been deployed by a user or not. This is done by searching in the storage if the given
    * [[ExecutableId]] of the executable is associated to the given username of the user.
    *
    * @param username
    *   the username of the user to be searched for
    * @param id
    *   the [[ExecutableId]] of the executable to be searched for
    * @return
    *   <code>true</code> if the given executable is associated to the given user, <code>false</code> otherwise. The result is
    *   wrapped in a [[Future]] because the operation could be long-running and it can fail. In this case, the future will be
    *   failed with a [[Throwable]] explaining the error
    */
  def isExecutableOfUser(username: String, id: ExecutableId): Future[Boolean]

  /** Adds an executable to the user that deployed it, given the [[ExecutableId]] of the executable that has been deployed, the
    * username of the user which deployed it and the name the user wants to remember the file with.
    *
    * @param username
    *   the username of the user which deployed the executable
    * @param id
    *   the [[ExecutableId]] of the executable that has been deployed
    * @param fileName
    *   the name that the user associated with the executable
    * @return
    *   a [[Future]], because the operation could be long-running and it can fail. In this case, the future will be failed with a
    *   [[Throwable]] explaining the error
    */
  def addExecutableToUser(username: String, id: ExecutableId, fileName: String): Future[Unit]
}

/** Companion object to the [[ServiceStorage]] trait, containing its factory method. */
private[ws] object ServiceStorage {

  /* Implementation for the ServiceStorage trait. */
  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  private class ServiceStorageImpl(ctx: PostgresJdbcContext[SnakeCase])(using ExecutionContext) extends ServiceStorage {

    import ctx.*

    /* Scala representation of the "users" table in the database. */
    private case class Users(username: String, password: String)

    /* Scala representation of the "executables" table in the database. */
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
          Future.failed[Unit](IllegalArgumentException("The username is already taken, please choose another."))
        } else {
          Future(
            ctx.run(
              query[Users].insertValue(
                lift(
                  Users(
                    username,
                    BCrypt.hashpw(password, BCrypt.gensalt(12))
                  )
                )
              )
            )
          )
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

  /** Factory method for creating new instances for the [[ServiceStorage]] trait implemented by a database.
    *
    * @param dataSource
    *   the [[DataSource]] object to be used for connecting to the database which implements the storage
    * @param ExecutionContext
    *   the [[ExecutionContext]] on which the operations on the storage should be carried out
    * @return
    *   a new [[ServiceStorage]] instance
    */
  def apply(dataSource: DataSource)(using ExecutionContext): ServiceStorage =
    ServiceStorageImpl(PostgresJdbcContext[SnakeCase](SnakeCase, dataSource))
}
