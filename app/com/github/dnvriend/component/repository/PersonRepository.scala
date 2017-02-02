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

package com.github.dnvriend.component.repository

import javax.inject.{ Inject, Singleton }

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.github.dnvriend.component.simpleserver.dto.http.Person

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class PersonRepository @Inject() (implicit ec: ExecutionContext) {
  def people(numberOfPeople: Int): Source[Person, NotUsed] =
    Source.repeat(Person("foo", 1, false)).zipWith(Source.fromIterator(() => Iterator from 0)) {
      case (p, i) => p.copy(
        name = if (i % 10 == 0) "baz-" + i else if (i % 2 == 0) "foo-" + i else "bar-" + i,
        age = i,
        married = i % 2 == 0
      )
    }.take(numberOfPeople)

  def listOfPersons(numberOfPeople: Int): Seq[Person] = (0 to numberOfPeople).map { i =>
    Person(
      name = if (i % 10 == 0) "baz-" + i else if (i % 2 == 0) "foo-" + i else "bar-" + i,
      age = i,
      married = i % 2 == 0
    )
  }

  def personAsync: Future[Person] = Future.successful(personSync)

  def personSync: Person = Person("John Doe", 25, false)
}
