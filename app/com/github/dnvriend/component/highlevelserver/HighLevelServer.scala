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

///*
// * Copyright 2016 Dennis Vriend
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.github.dnvriend.component.highlevelserver
//
//import akka.actor.{ ActorSystem, Props }
//import akka.event.{ Logging, LoggingAdapter }
//import akka.http.scaladsl.Http
//import akka.stream.scaladsl.{ Sink, Source }
//import akka.stream.{ ActorMaterializer, Materializer }
//import akka.util.Timeout
//import com.github.dnvriend.component.highlevelserver.repository.PersonRepository
//import com.github.dnvriend.component.highlevelserver.route.RestRoute
//
//import scala.concurrent.duration._
//import scala.concurrent.{ ExecutionContext, Future }
//
//class HighLevelServer(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, logging: LoggingAdapter, timeout: Timeout) {
//  val personDb = system.actorOf(Props[PersonRepository])
//  val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
//    Http().bind(interface = "localhost", port = 8080)
//  val binding: Future[Http.ServerBinding] = serverSource.to(Sink.foreach(_.handleWith(RestRoute.routes(personDb)))).run
//}
//
//object HighLevelServer extends App {
//  // setting up some machinery
//  implicit val system: ActorSystem = ActorSystem()
//  implicit val mat: Materializer = ActorMaterializer()
//  implicit val ec: ExecutionContext = system.dispatcher
//  implicit val log: LoggingAdapter = Logging(system, this.getClass)
//  implicit val timeout: Timeout = Timeout(10.seconds)
//  new HighLevelServer()
//}
