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

package com.github.dnvriend.component.simpleserver

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl._
import akka.stream.{ ActorMaterializer, Materializer }
import com.github.dnvriend.component.repository.PersonRepository
import com.github.dnvriend.component.simpleserver.route._

import scala.concurrent.ExecutionContext

class SimpleServer(personDao: PersonRepository)(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, logger: LoggingAdapter) {
  Http().bindAndHandle(SimpleServerRestRoutes.routes(personDao), "0.0.0.0", 8080)
}

object SimpleServerLauncher extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val log: LoggingAdapter = Logging(system, this.getClass)

  sys.addShutdownHook {
    system.terminate()
  }

  new SimpleServer(new PersonRepository)
}
