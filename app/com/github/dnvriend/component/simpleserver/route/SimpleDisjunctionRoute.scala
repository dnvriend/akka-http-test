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

import akka.http.scaladsl.server.{ Directives, Route }
import com.github.dnvriend.component.simpleserver.dto.http.OrderDto
import com.github.dnvriend.component.simpleserver.marshaller.DisjunctionMarshaller.{ ErrorMessage, FatalError, NonFatalError }
import com.github.dnvriend.component.simpleserver.marshaller.{ DisjunctionMarshaller, Marshallers, ValidationMarshaller }

object SimpleDisjunctionRoute extends Directives with DisjunctionMarshaller with ValidationMarshaller with Marshallers {
  import scalaz._
  import Scalaz._
  final case class MyFatalError(description: String) extends FatalError
  final case class MyNonFatalError(description: String) extends NonFatalError

  def route: Route =
    pathPrefix("disjunction" / "simple") {
      (get & path("failure")) {
        complete("failure-left".left[String])
      } ~
        (get & path("success")) {
          complete("success-right".right[String])
        }
    } ~
      pathPrefix("disjunction" / "nel") {
        (get & path("string" / "failure")) {
          complete(("failure1".failureNel[String] *> "failure2".failureNel[String]).disjunction)
        } ~
          (get & path("nonfatal" / "failure")) {
            complete((MyNonFatalError("my-non-fatal-error-1").failureNel[OrderDto] *> MyNonFatalError("my-non-fatal-error-2").failureNel[OrderDto]).disjunction)
          } ~
          (get & path("fatal" / "failure")) {
            complete((MyFatalError("my-fatal-error-1").failureNel[OrderDto] *> MyFatalError("my-fatal-error-2").failureNel[OrderDto]).disjunction)
          } ~
          (get & path("combined" / "failure")) {
            complete((Validation.failureNel[ErrorMessage, OrderDto](MyFatalError("my-fatal-error-1")) *> Validation.failureNel[ErrorMessage, OrderDto](MyNonFatalError("my-non-fatal-error-1"))).disjunction)
          } ~
          (get & path("nonfatal" / "success")) {
            complete(OrderDto(1, "test-OrderDto").successNel[ErrorMessage].disjunction)
          }
      } ~
      pathPrefix("validation") {
        (get & path("failure")) {
          complete("failure".failureNel[String])
        } ~
          (get & path("success")) {
            complete("success".successNel[String])
          }
      }
}
