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

import akka.event.LoggingAdapter
import akka.http.scaladsl.common.{ EntityStreamingSupport, JsonEntityStreamingSupport }
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{ Directives, Route }
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.github.dnvriend.component.repository.PersonRepository
import com.github.dnvriend.component.simpleserver.dto.http.Person
import com.github.dnvriend.component.simpleserver.marshaller.Marshallers

import scala.concurrent.ExecutionContext

object JsonStreamingRoute extends Directives with SprayJsonSupport with Marshallers {
  val start = ByteString.empty
  val sep = ByteString("\n")
  val end = ByteString.empty

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()
    .withFramingRenderer(Flow[ByteString].intersperse(start, sep, end))
    .withParallelMarshalling(parallelism = 8, unordered = true)

  def route(dao: PersonRepository)(implicit mat: Materializer, ec: ExecutionContext): Route =
    path("stream" / IntNumber) { numberOfPersons =>
      (get & pathEnd) {
        complete(dao.people(numberOfPersons))
      }
    } ~
      (post & path("stream") & entity(asSourceOf[Person])) { people =>
        val total = people.log("people").runFold(0) { case (c, _) => c + 1 }
        complete(total.map(n => s"Received $n number of person"))
      }
}
