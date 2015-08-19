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

package com.github.dnvriend.webservices.iens

import akka.http.scaladsl.model.StatusCodes
import com.github.dnvriend.TestSpec
import com.github.dnvriend.webservices.generic.HttpClient
import com.hunorkovacs.koauth.domain.KoauthRequest
import com.hunorkovacs.koauth.service.consumer.{ DefaultConsumerService, RequestWithInfo }
import org.scalatest.Ignore

import scala.concurrent.Future

/**
 * see: http://oauth.net/code/
 * see: http://oauthbible.com/
 * see: https://github.com/kovacshuni/koauth
 * see: https://github.com/danielcrenna/oauth
 * see: http://stackoverflow.com/questions/7093878/php-oauthprovider-library-invalid-signature-oauth-flow-just-confused
 * see: https://github.com/kovacshuni/koauth-samples/blob/master/scala-consumer-scalatra/src/main/scala/com/hunorkovacs/koauth/sample/scala/consumerscalatra/Twitter.scala
 * see: https://github.com/hasanozgan/spray-oauth
 * see: https://developer.github.com/guides/basics-of-authentication/
 */
@Ignore
class IensConnectionTest extends TestSpec {
  val consumerKey = "YOUR_KEY_HERE"
  val consumerSecret = "YOUR_SECRET_KEY_HERE"
  // got to create a new ConsumerService, it genereates new ids and hashes each time
  val consumerService = new DefaultConsumerService(ec)
  // please note that the used URL (and request params) must be the same as the request we send the request to!!
  def koAuthRequest(url: String) = KoauthRequest("GET", url, None, None)
  def oAuthHeader(url: String): Future[RequestWithInfo] = consumerService.createOauthenticatedRequest(koAuthRequest(s"https://www.iens.nl$url"), consumerKey, consumerSecret, "", "")
  val iensHttpClient = HttpClient("www.iens.nl", 443, tls = true)
  val url = "/rest/review/?restaurant_id=22630"

  "Iens" should "call with external headers set" in {
    //     note that the HttpClient uses the 'hostname' only approach,
    //     setting tls to 'true' will enable https connections, and tls to 'false' will set it to http
    //     note that the KoauthRequest object needs the whole URL so for Iens that will be https://www.iens.nl/rest/review/?restaurant_id=22630
    //     and must be refreshed for each request, so when you wish another resource or ID, you will need to update the URL also.
    (1 to 2).foreach { i ⇒
      val response = iensHttpClient.get(url, "", Map.empty, Map("Authorization" -> oAuthHeader(url).futureValue.header)).futureValue
      response.status shouldBe StatusCodes.OK
    }
  }

  it should "call service by configuration" in {
    val iens = HttpClient("www.iens.nl", 443, tls = true, consumerKey = Option(consumerKey), consumerSecret = Option(consumerSecret))
    (1 to 2).foreach { i ⇒
      iens.get(url, "", Map.empty, Map.empty).futureValue.status shouldBe StatusCodes.OK
    }
  }

  it should "call service by default configuration" in {
    val iens = HttpClient("iens")
    (1 to 2).foreach { i ⇒
      iens.get(url, "", Map.empty, Map.empty).futureValue.status shouldBe StatusCodes.OK
    }
  }

  "find by geo" should "return a list of restaurants" in {
    val iens = HttpClient("iens")
    iens.get("/rest/restaurant", "", Map("id" -> "searchrestaurants", "latitude" -> "52.166083", "longitude" -> "4.518241"))
      .flatMap(resp ⇒ HttpClient.responseToString(resp)).futureValue.size should not be 0
  }

  "get restaurant by id" should "return restaurant" in {
    val iens = HttpClient("iens")
    iens.get("/rest/restaurant", "", Map("id" -> "getrestaurantdetails", "restaurant_id" -> "23091"))
      .flatMap(resp ⇒ HttpClient.responseToString(resp)).futureValue.size should not be 0
  }

  "get reviews by id" should "return reviews" in {
    val iens = HttpClient("iens")
    iens.get("/rest/review", "", Map("restaurant_id" -> "23091"))
      .flatMap(resp ⇒ HttpClient.responseToString(resp)).futureValue.size should not be 0
  }
}
