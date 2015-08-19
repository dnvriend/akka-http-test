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

package com.github.dnvriend.webservices.postcode

import akka.stream.scaladsl.Source
import com.github.dnvriend.TestSpec
import org.scalatest.Ignore

@Ignore
class PostcodeClientTest extends TestSpec {
  "address" should "get an address for known zipcode" in {
    PostcodeClient().address("1313HX", 33).futureValue mustBe {
      case Some(Address("Kalmoespad", 33, _, "1313HX", "Almere", _, "Flevoland", _, _, 52.38072358, 5.19307017, _, _, _, _, _, _)) ⇒
    }
  }

  it should "find an address for a zipcode with lowercase letters" in {
    PostcodeClient().address("1313hx", 33).futureValue mustBe {
      case Some(Address("Kalmoespad", 33, _, "1313HX", "Almere", _, "Flevoland", _, _, 52.38072358, 5.19307017, _, _, _, _, _, _)) ⇒
    }
  }

  it should "find an address for a zipcode with a space (due to the normalizer)" in {
    PostcodeClient().address("1313 HX", 33).futureValue mustBe {
      case Some(Address("Kalmoespad", 33, _, "1313HX", "Almere", _, "Flevoland", _, _, 52.38072358, 5.19307017, _, _, _, _, _, _)) ⇒
    }
  }

  it should "find an address for a zipcode with a space and lowercase letters (due to the normalizer)" in {
    PostcodeClient().address("1313 hx", 33).futureValue mustBe {
      case Some(Address("Kalmoespad", 33, _, "1313HX", "Almere", _, "Flevoland", _, _, 52.38072358, 5.19307017, _, _, _, _, _, _)) ⇒
    }
  }

  it should "find no address for an invalid zipcode" in {
    PostcodeClient().address("0000AA", 33).futureValue mustBe {
      case None ⇒
    }
  }

  "Zipcode normalizer" should "normalize zipcode with spaces" in {
    PostcodeClient.normalizeZipcode("1313 HX") shouldBe Some("1313HX")
  }

  it should "normalize zipcode without spaces" in {
    PostcodeClient.normalizeZipcode("1313HX") shouldBe Some("1313HX")
  }

  it should "normalize zipcode with lowercase letters" in {
    PostcodeClient.normalizeZipcode("1313hx") shouldBe Some("1313HX")
  }

  it should "return none for invalid zipcodes" in {
    PostcodeClient.normalizeZipcode("1234A") shouldBe None
    PostcodeClient.normalizeZipcode("1234") shouldBe None
    PostcodeClient.normalizeZipcode("AA") shouldBe None
    PostcodeClient.normalizeZipcode("0000 AA") shouldBe None
    PostcodeClient.normalizeZipcode("0000AA") shouldBe None
    PostcodeClient.normalizeZipcode("0000AA") shouldBe None
  }

  "PostcodeClient cached connection" should "GetWeatherResult by zip and country" in {
    Source((1 to 10).toStream.map(i ⇒ (GetAddressRequest("1313HX", "33"), i)))
      .via(PostcodeClient().address)
      .runFold(0) { case (c, e) ⇒ println(e); c + 1 }
      .futureValue shouldBe 10
  }
}
