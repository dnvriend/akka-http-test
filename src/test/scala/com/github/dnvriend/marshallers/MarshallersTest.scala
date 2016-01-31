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

package com.github.dnvriend.marshallers

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.dnvriend.{ MediaVersionTypes, Service, TestSpecWithoutSystem }

/**
 * see: https://github.com/akka/akka/blob/releasing-akka-stream-and-http-experimental-1.0-RC4/akka-http-testkit/src/test/scala/akka/http/scaladsl/testkit/ScalatestRouteTestSpec.scala
 * see: http://spray.io/documentation/1.2.3/spray-testkit/
 */
class MarshallersTest extends TestSpecWithoutSystem with Service with ScalatestRouteTest {

  val jsonHeader = Accept(`application/json`)
  val xmlHeader = Accept(`application/xml`)

  val jsonHeaderV1 = Accept(MediaVersionTypes.`application/vnd.acme.v1+json`)
  val jsonHeaderV2 = Accept(MediaVersionTypes.`application/vnd.acme.v2+json`)
  val xmlHeaderV1 = Accept(MediaVersionTypes.`application/vnd.acme.v1+xml`)
  val xmlHeaderV2 = Accept(MediaVersionTypes.`application/vnd.acme.v2+xml`)

  "Get to ping" should "should include timestamp field" in {
    Get("/ping") ~> routes ~> check {
      status shouldEqual OK
      responseAs[String] should include("timestamp")
    }
  }

  "Get to person as JSON" should "return v2 for application/json" in {
    Get("/person") ~> addHeader(jsonHeader) ~> routes ~> check {
      status shouldEqual OK
      responseAs[String] shouldBe """{"name":"John Doe","age":25,"married":false}"""
    }
  }

  it should "return v1 for application/vnd.acme.v1+json" in {
    Get("/person") ~> addHeader(jsonHeaderV1) ~> routes ~> check {
      status shouldEqual OK
      responseAs[String] shouldBe """{"name":"John Doe","age":25}"""
    }
  }

  it should "return v2 for application/vnd.acme.v2+json" in {
    Get("/person") ~> addHeader(jsonHeaderV2) ~> routes ~> check {
      status shouldEqual OK
      responseAs[String] shouldBe """{"name":"John Doe","age":25,"married":false}"""
    }
  }

  "Get person as XML" should "return v2 for application/xml" in {
    Get("/person") ~> addHeader(xmlHeader) ~> routes ~> check {
      status shouldEqual OK
      responseAs[String] should include("<married>")
    }
  }

  it should "return v1 for application/vnd.acme.v1+xml" in {
    Get("/person") ~> addHeader(xmlHeaderV1) ~> routes ~> check {
      status shouldEqual OK
      responseAs[String] should not include "<married>"
    }
  }

  it should "return v2 for application/vnd.acme.v2+xml" in {
    Get("/person") ~> addHeader(xmlHeaderV2) ~> routes ~> check {
      status shouldEqual OK
      responseAs[String] should include("<married>")
    }
  }
}
