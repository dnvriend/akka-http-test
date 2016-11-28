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

import akka.http.scaladsl.common.{ CsvEntityStreamingSupport, EntityStreamingSupport }
import akka.http.scaladsl.marshalling.{ Marshaller, Marshalling }
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.server.{ Directives, Route }
import akka.util.ByteString
import com.github.dnvriend.component.repository.PersonRepository
import com.github.dnvriend.component.simpleserver.dto.http.Person
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

object CsvStreamingRoute extends Directives with PlayJsonSupport {
  implicit val personAsCsv = Marshaller.strict[Person, ByteString] { person =>
    Marshalling.WithFixedContentType(ContentTypes.`text/csv(UTF-8)`, () => {
      ByteString(List(person.name.replace(",", "."), person.age, person.married).mkString(","))
    })
  }

  implicit val csvStreamingSupport: CsvEntityStreamingSupport = EntityStreamingSupport.csv()

  def route(dao: PersonRepository): Route =
    path("stream" / IntNumber) { numberOfPeople =>
      pathEnd {
        get {
          complete(dao.people(numberOfPeople))
        }
      }
    }
}
