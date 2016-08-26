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

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.{ StatusCodes, Uri }
import akka.http.scaladsl.server.{ Directives, Route }
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.pattern.ask
import akka.stream.scaladsl.{ Sink, Source }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.util.Timeout
import com.github.dnvriend.domain.Person
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

// see: akka.http.scaladsl.marshalling.ToResponseMarshallable
// see: akka.http.scaladsl.marshalling.PredefinedToResponseMarshallers
object RestRoute extends Directives {
  def routes(personDb: ActorRef)(implicit timeout: Timeout, trmSingle: ToResponseMarshaller[PersonWithId], trmList: ToResponseMarshaller[List[PersonWithId]], fru: FromRequestUnmarshaller[Person]): Route = {
    pathEndOrSingleSlash {
      redirect(Uri("/api/person"), StatusCodes.PermanentRedirect)
    } ~
      pathPrefix("api" / "person") {
        get {
          path(IntNumber) { id =>
            println(s"PathEndsInNumber=$id")
            complete((personDb ? "findAll").mapTo[List[PersonWithId]])
          } ~
            pathEndOrSingleSlash {
              parameter("foo") { foo =>
                println(s"foo=$foo")
                complete((personDb ? "findAll").mapTo[List[PersonWithId]])
              } ~
                parameter('bar) { bar =>
                  println(s"bar=$bar")
                  complete((personDb ? "findAll").mapTo[List[PersonWithId]])
                } ~
                complete((personDb ? "findAll").mapTo[List[PersonWithId]])
            }
        } ~
          (post & pathEndOrSingleSlash & entity(as[Person])) { person =>
            complete((personDb ? person).mapTo[PersonWithId])
          }
      } ~
      path("failure") {
        pathEnd {
          complete(Future.failed[String](new RuntimeException("Simulated Failure")))
        }
      } ~
      path("success") {
        pathEnd {
          complete(Future.successful("Success!!"))
        }
      }
  }
}

object HighLevelServer extends App with SprayJsonSupport with DefaultJsonProtocol {
  // setting up some machinery
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val log: LoggingAdapter = Logging(system, this.getClass)
  implicit val timeout: Timeout = Timeout(10.seconds)

  // the jsonFormats for Person and PersonWithId
  implicit val personJsonFormat = jsonFormat3(Person)
  implicit val personWithIdJsonFormat = jsonFormat4(PersonWithId)

  val personDb = system.actorOf(Props[PersonDb])

  //  Http().bindAndHandle(routes, "0.0.0.0", 8080)

  val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
    Http().bind(interface = "localhost", port = 8080)
  val binding: Future[Http.ServerBinding] = serverSource.to(Sink.foreach(_.handleWith(RestRoute.routes(personDb)))).run
}
