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

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{ Directives, Route }
import spray.json.DefaultJsonProtocol

import scala.util.Try

object TryRoute extends Directives with SprayJsonSupport with DefaultJsonProtocol {
  def route: Route = {
    pathPrefix("try") {
      (get & path("failure")) {
        complete(Try((1 / 0).toString))
      } ~
        (get & path("success")) {
          complete(Try(1.toString))
        }
    }
  }
}
