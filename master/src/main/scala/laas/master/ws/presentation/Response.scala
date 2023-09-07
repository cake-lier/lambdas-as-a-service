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
package laas.master.ws.presentation

import java.util.UUID

import scala.util.Try

import laas.master.model.Executable.ExecutableId
import laas.master.model.Execution.ExecutionOutput
import laas.master.model.User.DeployedExecutable

enum Response {

  case SendId(id: UUID) extends Response

  case UserStateOutput(deployedExecutables: Try[Seq[DeployedExecutable]]) extends Response

  case ExecuteOutput(id: ExecutableId, output: Try[ExecutionOutput]) extends Response

  case DeployOutput(id: Try[ExecutableId]) extends Response
}
