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

package com.github.dnvriend.webservices.generic

import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{ HttpHeader, StatusCodes }
import akka.stream.scaladsl.Source
import com.github.dnvriend.TestSpec

class HttpClientTest extends TestSpec {
  "HttpClient.encoder" should "url encode text" in {
    HttpClient.encode("abcde abcde") shouldBe "abcde+abcde"
  }

  /**
   * * disabled
   * "HttpClient.queryString" should "create a query string from an empty map" in {
   * HttpClient.queryString(Map.empty) shouldBe ""
   * }
   *
   * it should "create a query string from simple entries" in {
   * HttpClient.queryString(Map("a" → "b", "c" → "d")) shouldBe "?a=b&c=d"
   * HttpClient.queryString(Map("a" → "b c", "d" → "e f")) shouldBe "?a=b+c&d=e+f"
   * HttpClient.queryString(Map("a" → "", "c" → "")) shouldBe "?a=&c="
   * HttpClient.queryString(Map("" → "", "" → "")) shouldBe "?"
   * }
   * *
   */

  "HttpClient.header" should "create a single http header" in {
    HttpClient.header("foo", "bar").value mustBe {
      case HttpHeader("foo", "bar") ⇒
    }
  }

  it should "create a List[HttpHeader] from a Map[String, String]" in {
    HttpClient.headers(Map("foo" → "bar", "bar" → "baz")).sortBy(_.name()) mustBe {
      case List(HttpHeader("bar", "baz"), HttpHeader("foo", "bar")) ⇒
    }
  }

  /**
   * see: http://httpbin.org/
   */
  "Client connection to httpbin.org (echo service)" should "non TLS HTTP 200 for '/get'" in {
    HttpClient("httpbin.org", 80, tls = false).get("/get").futureValue.status shouldBe StatusCodes.OK
  }

  it should "TLS HTTP 200 for a get on '/get'" in {
    HttpClient("httpbin.org", 443, tls = true).get("/get").futureValue.status shouldBe StatusCodes.OK
  }

  it should "support basic auth for non-tls" in {
    HttpClient("httpbin.org", 80, tls = false, Option("foo"), Option("bar")).get("/basic-auth/foo/bar").futureValue.status shouldBe StatusCodes.OK
  }

  it should "support basic auth for tls" in {
    HttpClient("httpbin.org", 443, tls = true, Option("foo"), Option("bar")).get("/basic-auth/foo/bar").futureValue.status shouldBe StatusCodes.OK
  }

  it should "support post" in {
    HttpClient("httpbin.org", 443, tls = true, Option("foo"), Option("bar")).post("/post").futureValue.status shouldBe StatusCodes.OK
  }

  it should "support put" in {
    HttpClient("httpbin.org", 443, tls = true, Option("foo"), Option("bar")).put("/put").futureValue.status shouldBe StatusCodes.OK
  }

  it should "support delete" in {
    HttpClient("httpbin.org", 443, tls = true, Option("foo"), Option("bar")).delete("/delete").futureValue.status shouldBe StatusCodes.OK
  }

  it should "support patch" in {
    HttpClient("httpbin.org", 443, tls = true, Option("foo"), Option("bar")).patch("/patch").futureValue.status shouldBe StatusCodes.OK
  }

  def httpBinGet = HttpClient.mkRequest(RequestBuilding.Get, "/get")

  "Cached connection" should "non tls HTTP 200 for /get" in {
    Source((1 to 10).map(i ⇒ (httpBinGet, i)))
      .via(HttpClient.cachedConnection("httpbin.org", 80))
      .via(HttpClient.responseToString)
      .log("received")
      .runFold(0) { case (c, e) ⇒ c + 1 }
      .futureValue shouldBe 10
  }
}
