/*
 * Copyright 2015 Dennis Vriend
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

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.testkit.ScalatestRouteTest

/**
 * see: https://github.com/akka/akka/blob/releasing-akka-stream-and-http-experimental-1.0-RC4/akka-http-testkit/src/test/scala/akka/http/scaladsl/testkit/ScalatestRouteTestSpec.scala
 * see: http://spray.io/documentation/1.2.3/spray-testkit/
 */
class MarshallersTest extends TestSpecWithoutSystem with Service with ScalatestRouteTest {

  val jsonHeader = RawHeader("Accept", "application/json")
  val xmlHeader = RawHeader("Accept", "application/xml")

  val xmlHeaderV1 = RawHeader("Accept", "application/vnd.acme.v1+xml")
  val xmlHeaderV2 = RawHeader("Accept", "application/vnd.acme.v2+xml")

  "Get to ping" should "should include timestamp field" in {
    Get("/ping") ~> routes ~> check {
      responseAs[String] should include("timestamp")
    }
  }

  "Get to person" should "return person as JSON" in {
    Get("/person") ~> addHeader(jsonHeader) ~> routes ~> check {
      status shouldEqual OK
      responseAs[String] shouldBe """{"name":"John Doe","age":25,"married":false}"""
    }
  }

  //  it should "return person as XML v2 with application/xml" in {
  //    Get("/person") ~> addHeader(xmlHeader) ~> routes ~> check {
  //      responseAs[String] should include("timestamp")
  //    }
  //  }
  //
  //  it should "return person as XML v1 with vendor media type" in {
  //    Get("/person") ~> addHeader(xmlHeaderV1) ~> routes ~> check {
  //      responseAs[String] should include("timestamp")
  //    }
  //  }
  //
  //  it should "return person as XML v2 with vendor media type" in {
  //    Get("/person") ~> addHeader(xmlHeaderV2) ~> routes ~> check {
  //      responseAs[String] should include("timestamp")
  //    }
  //  }
}
