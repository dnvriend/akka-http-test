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

import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{ Concat, Source }
import akka.util.ByteString
import com.github.dnvriend.domain.Person
import com.github.dnvriend.util.TimeUtil

trait Service extends Marshallers with GenericServices {
  import spray.json._
  implicit val personjsonformat = jsonFormat3(Person)

  def personSource(numberOfPersons: Int): Source[ByteString, Any] = Source.combine(
    Source.single("[").map(ByteString(_)),
    Source.repeat(Person("foo", 1)).zipWith(Source.fromIterator(() ⇒ Iterator from 0)) {
      case (p, i) ⇒ p.copy(name = p.name + "-" + i, age = i, married = i % 2 == 0)
    }.take(numberOfPersons).map(_.toJson.prettyPrint).intersperse(",").map(ByteString(_)),
    Source.single("]").map(ByteString(_))
  )(nr ⇒ Concat(nr))

  def routes: Route =
    logRequestResult("akka-http-test") {
      path("") {
        redirect("person", StatusCodes.PermanentRedirect)
      } ~
        pathPrefix("person") {
          pathEnd {
            get {
              complete {
                Person("John Doe", 25)
              }
            } ~
              post {
                entity(as[Person]) { person ⇒
                  complete(StatusCodes.OK, person.toString)
                }
              }
          }
        } ~ pathPrefix("persons") {
          pathPrefix("strict") {
            pathEnd {
              complete(Seq(Person("foo", 1), Person("bar", 2)))
            }
          } ~
            pathPrefix("stream" / IntNumber) { numberOfPersons ⇒
              pathEnd {
                complete {
                  HttpResponse(
                    //                entity = HttpEntity.Chunked(MediaTypes.`application/json`, Source.tick(0.seconds, 1.second, "test"))
                    //                entity = HttpEntity.CloseDelimited(ContentTypes.`text/plain(UTF-8)`, Source.tick(0.seconds, 500.millis, ByteString("test")).take(10))
                    entity = HttpEntity.CloseDelimited(ContentTypes.`text/plain(UTF-8)`, personSource(numberOfPersons))
                  )
                }
              }
            }
        } ~ pathPrefix("ping") {
          complete {
            Ping(TimeUtil.timestamp)
          }
        }
    }
}

object SimpleServer extends App with Service with CoreServices {
  // see: http://patorjk.com/software/taag/#p=testall&h=1&v=2&f=Old%20Banner&t=akka-http-test
  val banner =
    s"""
      |
      |  ##   #    # #    #   ##         #    # ##### ##### #####        ##### ######  ####  #####
      | #  #  #   #  #   #   #  #        #    #   #     #   #    #         #   #      #        #
      |#    # ####   ####   #    # ##### ######   #     #   #    # #####   #   #####   ####    #
      |###### #  #   #  #   ######       #    #   #     #   #####          #   #           #   #
      |#    # #   #  #   #  #    #       #    #   #     #   #              #   #      #    #   #
      |#    # #    # #    # #    #       #    #   #     #   #              #   ######  ####    #
      |
      |
    """.stripMargin
  println(banner)
  Http().bindAndHandle(routes, "0.0.0.0", 8080)
}
