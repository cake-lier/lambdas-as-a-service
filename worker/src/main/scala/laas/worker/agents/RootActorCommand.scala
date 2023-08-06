package io.github.cakelier
package laas.worker.agents

enum RootActorCommand {

  case WorkerUp(success: Boolean) extends RootActorCommand
}
