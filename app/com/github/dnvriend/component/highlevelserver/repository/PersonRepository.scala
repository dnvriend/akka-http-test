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

package com.github.dnvriend.component.highlevelserver.repository

import akka.actor.Actor
import com.github.dnvriend.component.highlevelserver.dto.{ Person, PersonWithId }

class PersonRepository extends Actor {
  override def receive: Receive = database(0, Map.empty)
  def database(id: Int, people: Map[Int, PersonWithId]): Receive = {
    case person: Person =>
      val personWithId = PersonWithId(id + 1, person.name, person.age, person.married)
      context.become(database(id + 1, people + (id + 1 -> personWithId)))
      sender() ! personWithId

    case "findAll" =>
      sender() ! people.values.toList.sortBy(_.id)
  }
}
