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

package com.github.dnvriend.component.simpleserver.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ Directives, Route }
import akka.stream.Materializer
import com.github.dnvriend.component.repository.PersonRepository
import com.github.dnvriend.component.simpleserver.dto.http.{ Person, Ping }
import com.github.dnvriend.component.simpleserver.marshaller.Marshallers
import com.github.dnvriend.util.TimeUtil

import scala.concurrent.ExecutionContext

object SimpleServerRestRoutes extends Directives with Marshallers {
  def routes(dao: PersonRepository)(implicit mat: Materializer, ec: ExecutionContext): Route =
    logRequestResult("akka-http-test") {
      pathPrefix("person") {
        path("sync") {
          get {
            complete(dao.personSync)
          }
        } ~
          path("async") {
            get {
              complete(dao.personAsync)
            }
          } ~
          pathEnd {
            get {
              complete(dao.personSync)
            }
          } ~
          (post & entity(as[Person])) { person =>
            complete(StatusCodes.Created)
          }
      } ~ pathPrefix("persons") {
        pathPrefix("strict" / IntNumber) { numberOfPersons =>
          pathEnd {
            get {
              complete(dao.listOfPersons(numberOfPersons))
            }
          }
        } ~ JsonStreamingRoute.route(dao) ~ CsvStreamingRoute.route(dao)
      } ~ (get & pathPrefix("ping")) {
        complete(Ping(TimeUtil.timestamp))
      } ~ SimpleDisjunctionRoute.route ~ TryRoute.route
    }
}
