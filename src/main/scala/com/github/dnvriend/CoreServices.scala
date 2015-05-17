package com.github.dnvriend

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.{ActorFlowMaterializer, FlowMaterializer}

import scala.concurrent.ExecutionContextExecutor

trait CoreServices {
   implicit val system: ActorSystem = ActorSystem()
   implicit val log: LoggingAdapter = Logging(system, this.getClass)
   implicit val materializer: FlowMaterializer = ActorFlowMaterializer()
   implicit val ec: ExecutionContextExecutor = system.dispatcher
}
