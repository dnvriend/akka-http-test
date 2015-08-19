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

package com.github.dnvriend.webservices.eetnu

import akka.stream.scaladsl.Source
import com.github.dnvriend.TestSpec
import com.github.dnvriend.webservices.common.LatLon

class EetnuClientTest extends TestSpec {
  "venuesByQuery" should "Get Teddy venue with full properties" in {
    EetNuClient().venuesByQuery("1313 EG").futureValue.head mustBe {
      case Venue(
        1770, // id
        "Teddy Snack",
        "Snackbars",
        Some("+31 36 533 5529"),
        None,
        None,
        None,
        None,
        None,
        Some(91), // rating
        Some("https://www.eet.nu/almere/teddy-snack"),
        _,
        _,
        Address(
          "Basilicumweg 335",
          "1313 EG",
          "Almere",
          "Flevoland",
          "The Netherlands"
          ),
        "regular", // plan
        Images(List())) ⇒
    }
  }

  it should "get teddy by geo" in {
    EetNuClient().venuesByGeo("52.3817809", "5.1991995").futureValue.headOption mustBe {
      case Some(Venue(1770, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)) ⇒
    }
  }

  it should "Get Teddy venue by query on zipcode with space" in {
    EetNuClient().venuesByQuery("1313 EG").futureValue.headOption mustBe {
      case Some(Venue(1770, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)) ⇒
    }
  }

  it should "Get Teddy venue on lowercase zipcode with space" in {
    EetNuClient().venuesByQuery("1313 eg").futureValue.headOption mustBe {
      case Some(Venue(1770, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)) ⇒
    }
  }

  it should "Get no venue on trimmed zipcode" in {
    EetNuClient().venuesByQuery("1313EG").futureValue.headOption mustBe {
      case None ⇒
    }
  }

  "VenuesById" should "get Teddy venue by id" in {
    EetNuClient().venueById("1770").futureValue.value mustBe {
      case Venue(
        1770, // id
        "Teddy Snack",
        "Snackbars",
        Some("+31 36 533 5529"),
        None,
        None,
        None,
        None,
        None,
        Some(91), // rating
        Some("https://www.eet.nu/almere/teddy-snack"),
        _,
        _,
        Address(
          "Basilicumweg 335",
          "1313 EG",
          "Almere",
          "Flevoland",
          "The Netherlands"
          ),
        "regular", // plan
        Images(List())) ⇒
    }
  }

  it should "return none for unknown venue" in {
    EetNuClient().venueById("-11111").futureValue shouldBe None
  }

  "ReviewsByVenueId" should "get a list of reviews by id" in {
    EetNuClient().reviewsByVenueId("1770").futureValue should not be 'empty
  }

  "VenuesByZipcode" should "return teddy for zipcode with space" in {
    EetNuClient().venuesByZipcode("1313 EG").futureValue.headOption mustBe {
      case Some(Venue(1770, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)) ⇒
    }
  }

  it should "return teddy for zipcode without space" in {
    EetNuClient().venuesByZipcode("1313EG").futureValue.headOption mustBe {
      case Some(Venue(1770, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)) ⇒
    }
  }

  it should "return None for an invalid zipcode" in {
    EetNuClient().venuesByQuery("0000AA").futureValue.headOption mustBe {
      case None ⇒
    }
  }

  "EetNuClient Zipcode Normalizer" should "normalize zips with spaces" in {
    EetNuClient.normalizeZipcode("1234 AA") shouldBe Option("1234 AA")
  }

  it should "normalize zips without spaces" in {
    EetNuClient.normalizeZipcode("1234AA") shouldBe Option("1234 AA")
  }

  it should "normalize zips with lowercase letters" in {
    EetNuClient.normalizeZipcode("1234aa") shouldBe Option("1234 AA")
  }

  it should "return none for invalid zipcodes" in {
    EetNuClient.normalizeZipcode("1234A") shouldBe None
    EetNuClient.normalizeZipcode("1234") shouldBe None
    EetNuClient.normalizeZipcode("AA") shouldBe None
    EetNuClient.normalizeZipcode("0000 AA") shouldBe None
    EetNuClient.normalizeZipcode("0000AA") shouldBe None
    EetNuClient.normalizeZipcode("0000AA") shouldBe None
  }

  "Eetnu cached connection" should "get venues by zipcode" in {
    Source((1 to 10).map(i ⇒ ("1313 EG", i)))
      .via(EetNuClient().venuesByZipcode)
      .runFold(0) { case (c, e) ⇒ c + 1 }
      .futureValue shouldBe 10
  }

  it should "get venues by geo" in {
    Source((1 to 10).map(i ⇒ (LatLon(52.3817809, 5.1991995), i)))
      .via(EetNuClient().venuesByGeo)
      .runFold(0) { case (c, e) ⇒ c + 1 }
      .futureValue shouldBe 10
  }

  it should "not send requests for invalid zipcodes" in {
    Source((1 to 50).map(i ⇒ ("0000AA", i)))
      .via(EetNuClient().venuesByZipcode)
      .runFold(0) { case (c, e) ⇒ c + 1 }
      .futureValue shouldBe 0
  }
}
