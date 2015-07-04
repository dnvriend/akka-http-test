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
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl._
import com.github.dnvriend.util.TimeUtil

import scala.xml.NodeSeq

case class Person(name: String, age: Int)
case class Ping(timestamp: String)

trait Service extends Marshallers with CoreServices {

  def routes: Flow[HttpRequest, HttpResponse, Unit] =
    logRequestResult("akka-http-test") {
      path("") {
        redirect("person/json", StatusCodes.PermanentRedirect)
      } ~
        pathPrefix("person") {
          pathPrefix("json") {
            complete {
              Person("John Doe", 25)
            }
          } ~
            pathPrefix("xml") {
              complete {
                Marshal(Person("John Doe", 25)).to[NodeSeq]
              }
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
