/*
 * Copyright 2015 Dennis Vriend
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

import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.dnvriend.domain.Person
import com.github.dnvriend.util.TimeUtil

trait Service extends Marshallers with GenericServices {

  def routes: Route =
    logRequestResult("akka-http-test") {
      path("") {
        redirect("person", StatusCodes.PermanentRedirect)
      } ~
        pathPrefix("person") {
          complete {
            Person("John Doe", 25)
          }
        } ~
        pathPrefix("ping") {
          complete {
            Ping(TimeUtil.timestamp)
          }
        }
    }
}

object SimpleServer extends App with Service with CoreServices {
  Http().bindAndHandle(routes, "0.0.0.0", 8080)
}
