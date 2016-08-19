/*
 * Copyright 2016 Dennis Vriend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dnvriend

import akka.actor.{ ActorSystem, Props }
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ StatusCodes, Uri }
import akka.http.scaladsl.server.{ Directives, Route }
import akka.pattern.ask
import akka.stream.scaladsl.{ Sink, Source }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.util.Timeout
import com.github.dnvriend.domain.Person
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

object HighLevelServer extends App with SprayJsonSupport with DefaultJsonProtocol with Directives {
  // setting up some machinery
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val log: LoggingAdapter = Logging(system, this.getClass)
  implicit val timeout: Timeout = Timeout(10.seconds)
  implicit val personJsonFormat = jsonFormat3(Person)
  implicit val personWithIdJsonFormat = jsonFormat4(PersonWithId)

  val personDb = system.actorOf(Props[PersonDb])

  val routes: Route = {
    pathEndOrSingleSlash {
      redirect(Uri("/api/person"), StatusCodes.PermanentRedirect)
    } ~
      path("api" / "person") {
        get {
          complete((personDb ? "findAll").mapTo[List[PersonWithId]])
        } ~
          (post & entity(as[Person])) { person =>
            complete((personDb ? person).mapTo[PersonWithId])
          }
      }
  }

  //  Http().bindAndHandle(routes, "0.0.0.0", 8080)

  val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
    Http().bind(interface = "localhost", port = 8080)
  val binding: Future[Http.ServerBinding] = serverSource.to(Sink.foreach(_.handleWith(routes))).run
}
