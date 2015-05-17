package com.github.dnvriend

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._

import scala.concurrent.ExecutionContextExecutor

trait Service extends Marshallers {
  implicit def system: ActorSystem
  implicit def ec: ExecutionContextExecutor
  implicit def materializer: FlowMaterializer
  implicit def log: LoggingAdapter

  def routes: Flow[HttpRequest, HttpResponse, Unit] =
    logRequestResult("akka-http-test") {
      pathPrefix("person") {
        complete {
          Person("John", "Doe", TimeUtil.timestamp)
        }
      }
    }
}

object SimpleServer extends App with Service with CoreServices {
  Http().bindAndHandle(routes, "0.0.0.0", 8080)
}
