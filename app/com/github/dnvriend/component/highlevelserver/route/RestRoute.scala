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

package com.github.dnvriend.component.highlevelserver.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.{ StatusCodes, Uri }
import akka.http.scaladsl.server.{ Directives, Route }
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.pattern.ask
import akka.util.Timeout
import com.github.dnvriend.component.highlevelserver.dto.PersonWithId
import com.github.dnvriend.component.highlevelserver.marshaller.Marshaller
import com.github.dnvriend.component.simpleserver.dto.http.Person

import scala.concurrent.Future

// see: akka.http.scaladsl.marshalling.ToResponseMarshallable
// see: akka.http.scaladsl.marshalling.PredefinedToResponseMarshallers
object RestRoute extends Directives with SprayJsonSupport with Marshaller {
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
