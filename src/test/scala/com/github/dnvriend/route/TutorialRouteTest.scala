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

package com.github.dnvriend.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._
import org.scalatest.{ FlatSpec, Matchers }

// see: http://doc.akka.io/docs/akka/current/scala/http/routing-dsl/testkit.html#route-testkit
class TutorialRouteTest extends FlatSpec with Matchers with ScalatestRouteTest {
  val smallRoute: Route =
    get {
      pathSingleSlash {
        complete {
          "Captain on the bridge!"
        }
      } ~
        path("ping") {
          complete("PONG!")
        }
    }

  it should "return a greeting for GET requests to the root path" in {
    Get() ~> smallRoute ~> check {
      responseAs[String] shouldBe "Captain on the bridge!"
    }
  }

  it should "return a 'PONG!' response for GET requests to /ping" in {
    Get("/ping") ~> smallRoute ~> check {
      responseAs[String] shouldEqual "PONG!"
    }
  }

  it should "leave GET requests to other paths unhandled" in {
    Get("/kermit") ~> smallRoute ~> check {
      handled shouldBe false
    }
  }

  it should "return a MethodNotAllowed error for PUT requests to the root path" in {
    Put() ~> Route.seal(smallRoute) ~> check {
      status === StatusCodes.MethodNotAllowed
      responseAs[String] shouldEqual "HTTP method not allowed, supported methods: GET"
    }
  }
}
