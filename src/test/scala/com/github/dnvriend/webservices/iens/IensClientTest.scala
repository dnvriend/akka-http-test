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

package com.github.dnvriend.webservices.iens

import akka.stream.scaladsl.Source
import com.github.dnvriend.TestSpec
import com.github.dnvriend.webservices.common.LatLon
import org.scalatest.Ignore

@Ignore
class IensClientTest extends TestSpec {
  val latitude = 52.170258
  val longitude = 4.515579
  val restaurantId = 23091

  "restaurantsByGeo" should "return a list of restaurants" in {
    IensClient().restaurantsByGeo(latitude, longitude, 1).futureValue mustBe {
      case SearchRestaurantsResponse(true, "", head :: tail, _) =>
    }
  }

  it should "find no restaurants for unknown lat/long" in {
    IensClient().restaurantsByGeo(90.0, 0.0, 1).futureValue mustBe {
      case SearchRestaurantsResponse(true, "", Nil, 0) =>
    }
  }

  "restaurantdetails" should "return a restaurant for known id" in {
    IensClient().restaurantDetails(restaurantId).futureValue mustBe {
      case GetRestaurantDetailsResponse(_, _, Some(RestaurantDetail("23091", _, _, _, _, _, _, _, _, _, _, _, _))) =>
    }
  }

  it should "find no restaurant for unknown id" in {
    IensClient().restaurantDetails(-1).futureValue mustBe {
      case GetRestaurantDetailsResponse(_, _, None) =>
    }
  }

  "reviews" should "return a list of reviews" in {
    IensClient().reviews(restaurantId).futureValue mustBe {
      case GetReviewResponse(_, _, head :: tail, _) =>
    }
  }

  it should "return an empty list of reviews for an unknown restaurant id" in {
    IensClient().reviews(-1).futureValue mustBe {
      case GetReviewResponse(_, _, Nil, "0") =>
    }
  }

  "Iens cached connection" should "get venues by geo" in {
    Source((1 to 50).map(i => (LatLon(latitude, longitude), i)))
      .via(IensClient().restaurantsByGeo)
      .runFold(0) { case (c, e) => c + 1 }
      .futureValue shouldBe 50
  }
}
