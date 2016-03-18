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

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import com.github.dnvriend.webservices.common.LatLon
import com.github.dnvriend.webservices.generic.HttpClient
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

// SearchRestaurants
case class Rating(name: Option[String], rating: Option[String])
case class SearchRestaurant(id: Long, name: Option[String], imageurl: Option[String], kitchenname: Option[String], address: Option[String], zipcode: Option[String], place: Option[String], country: Option[String], latitude: Option[String], longitude: Option[String], ratings: Option[List[Rating]], averageprice: Option[Double], seatmeid: Option[Long], distance: Option[Double])
case class SearchRestaurantsResponse(result: Boolean, message: String, restaurants: List[SearchRestaurant], count: Long)

// GetRestaurantDetails
case class RestaurantDetail(id: String, name: Option[String], imageurl: Option[String], kitchenname: Option[String], address: Option[String], zipcode: Option[String], place: Option[String], country: Option[String], latitude: Option[String], longitude: Option[String], ratings: Option[List[Rating]], averageprice: Option[String], seatmeid: Option[String])
case class GetRestaurantDetailsResponse(result: Boolean, message: String, restaurant: Option[RestaurantDetail])

// GetReviews
case class Review(restaurant_id: String, restaurant_name: Option[String], name: Option[String], user_star_value: Option[Int], date: Option[String], description: Option[String], ratings: List[Rating])
case class GetReviewResponse(result: Boolean, message: String, reviews: List[Review], num_reviews: String)

trait Marshallers extends DefaultJsonProtocol {
  implicit val ratingJsonFormat = jsonFormat2(Rating)
  implicit val restaurantJsonFormat = jsonFormat14(SearchRestaurant)
  implicit val searchRestaurantsResponseJsonFormat = jsonFormat4(SearchRestaurantsResponse)
  implicit val restaurantDetailJsonFormat = jsonFormat13(RestaurantDetail)
  implicit val getRestaurantDetailsResponseJsonFormat = jsonFormat3(GetRestaurantDetailsResponse)
  implicit val reviewJsonFormat = jsonFormat7(Review)
  implicit val getReviewResponseJsonFormat = jsonFormat4(GetReviewResponse)
}

object IensClient {
  import spray.json._
  def apply()(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, log: LoggingAdapter) = new IensClientImpl

  def responseToString(resp: HttpResponse)(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Future[String] =
    HttpClient.responseToString(resp)

  def asSearchRestaurantsResponse(json: String)(implicit reader: JsonReader[SearchRestaurantsResponse]): SearchRestaurantsResponse =
    json.parseJson.convertTo[SearchRestaurantsResponse]

  def asGetRestaurantDetailsResponse(json: String)(implicit reader: JsonReader[GetRestaurantDetailsResponse]): GetRestaurantDetailsResponse =
    json.parseJson.convertTo[GetRestaurantDetailsResponse]

  def asGetReviewResponse(json: String)(implicit reader: JsonReader[GetReviewResponse]): GetReviewResponse =
    json.parseJson.convertTo[GetReviewResponse]

  def restaurantsByGeoRequestFlow[T]: Flow[(LatLon, T), (HttpRequest, T), NotUsed] =
    Flow[(LatLon, T)].map {
      case (LatLon(lat, lon), id) ⇒
        val requestParams = Map(
          "id" → "searchrestaurants",
          "limit" → "1",
          "offset" → "0",
          "latitude" → lat.toString,
          "longitude" → lon.toString
        )
        (HttpClient.mkGetRequest("/rest/restaurant", "", requestParams), id)
    }

  def asSearchRestaurantsResponseFlow[A](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, reader: JsonReader[SearchRestaurantsResponse]): Flow[(Try[HttpResponse], A), (Try[SearchRestaurantsResponse], A), NotUsed] =
    HttpClient.responseToString[A].map {
      case (json, id) ⇒ (Try(json.parseJson.convertTo[SearchRestaurantsResponse]), id)
    }

  def restaurantDetailsRequestFlow[T]: Flow[(Long, T), (HttpRequest, T), NotUsed] =
    Flow[(Long, T)].map {
      case (vendorId, id) ⇒ (HttpClient.mkGetRequest("/rest/restaurant", queryParamsMap = Map("id" → "getrestaurantdetails", "restaurant_id" → vendorId.toString)), id)
    }

  def asGetRestaurantDetailsResponseFlow[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, reader: JsonReader[GetRestaurantDetailsResponse]): Flow[(Try[HttpResponse], T), (Try[GetRestaurantDetailsResponse], T), NotUsed] =
    HttpClient.responseToString[T].map {
      case (json, id) ⇒ (Try(json.parseJson.convertTo[GetRestaurantDetailsResponse]), id)
    }

  def reviewsRequestFlow[T]: Flow[(Long, T), (HttpRequest, T), NotUsed] =
    Flow[(Long, T)].map {
      case (vendorId, id) ⇒ (HttpClient.mkGetRequest("/rest/review", queryParamsMap = Map("restaurant_id" → id.toString)), id)
    }

  def asGetReviewResponseFlow[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, reader: JsonReader[GetReviewResponse]): Flow[(Try[HttpResponse], T), (Try[GetReviewResponse], T), NotUsed] =
    HttpClient.responseToString[T].map {
      case (json, id) ⇒ (Try(json.parseJson.convertTo[GetReviewResponse]), id)
    }
}

trait IensClient {
  def restaurantsByGeo(langitude: Double, longitude: Double, limit: Int = Int.MaxValue, offset: Int = 0): Future[SearchRestaurantsResponse]

  def restaurantsByGeo[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(LatLon, T), (Try[SearchRestaurantsResponse], T), NotUsed]

  def restaurantDetails(id: Long): Future[GetRestaurantDetailsResponse]

  def restaurantDetails[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(Long, T), (Try[GetRestaurantDetailsResponse], T), NotUsed]

  def reviews(id: Long): Future[GetReviewResponse]

  def reviews[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(Long, T), (Try[GetReviewResponse], T), NotUsed]
}

class IensClientImpl()(implicit val system: ActorSystem, val mat: Materializer, val ec: ExecutionContext, val log: LoggingAdapter) extends IensClient with Marshallers {
  import IensClient._

  private val client = HttpClient("iens")

  override def restaurantsByGeo(langitude: Double, longitude: Double, limit: Int, offset: Int): Future[SearchRestaurantsResponse] = {
    client.get("/rest/restaurant", queryParamsMap = Map(
      "id" → "searchrestaurants",
      "limit" → limit.toString,
      "offset" → offset.toString,
      "latitude" → langitude.toString,
      "longitude" → longitude.toString
    ))
      .flatMap(responseToString)
      .map(asSearchRestaurantsResponse)
  }

  override def restaurantsByGeo[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(LatLon, T), (Try[SearchRestaurantsResponse], T), NotUsed] =
    restaurantsByGeoRequestFlow[T]
      .via(client.cachedHostConnectionFlow[T])
      .via(asSearchRestaurantsResponseFlow[T])

  override def restaurantDetails(id: Long): Future[GetRestaurantDetailsResponse] = {
    client.get("/rest/restaurant", queryParamsMap = Map("id" → "getrestaurantdetails", "restaurant_id" → id.toString))
      .flatMap(responseToString)
      .map(asGetRestaurantDetailsResponse)
  }

  override def restaurantDetails[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(Long, T), (Try[GetRestaurantDetailsResponse], T), NotUsed] =
    restaurantDetailsRequestFlow[T]
      .via(client.cachedHostConnectionFlow[T])
      .via(asGetRestaurantDetailsResponseFlow[T])

  override def reviews(id: Long): Future[GetReviewResponse] =
    client.get("/rest/review", queryParamsMap = Map("restaurant_id" → id.toString))
      .flatMap(responseToString)
      .map(asGetReviewResponse)

  override def reviews[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(Long, T), (Try[GetReviewResponse], T), NotUsed] =
    reviewsRequestFlow[T]
      .via(client.cachedHostConnectionFlow[T])
      .via(asGetReviewResponseFlow[T])
}
