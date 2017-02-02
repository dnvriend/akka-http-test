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

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.github.dnvriend.component.repository.PersonRepository
import com.github.dnvriend.component.simpleserver.SimpleServer
import com.google.inject.{ AbstractModule, Provider }
import play.api.libs.concurrent.AkkaGuiceSupport

import scala.concurrent.ExecutionContext

class Module extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    bind(classOf[SimpleServer])
      .toProvider(classOf[SimpleServerProvider])
      .asEagerSingleton()
  }
}

class SimpleServerProvider @Inject() (personRepository: PersonRepository)(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext) extends Provider[SimpleServer] {
  override def get(): SimpleServer =
    new SimpleServer(personRepository)
}
