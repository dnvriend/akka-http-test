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

package com.github.dnvriend.component.webservices.eetnu

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import com.github.dnvriend.component.webservices.common.LatLon
import com.github.dnvriend.component.webservices.generic.HttpClient
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try
import scala.util.matching.Regex

// locations
case class Locations(results: List[Location])
case class Location(id: Long, url: String, name: String, `type`: String, created_at: String, updated_at: Option[String], geolocation: Geolocation, counters: Counters, resources: Resources)
case class Geolocation(latitude: Double, longitude: Double)
case class Counters(venues: Long)
case class Resources(venues: Option[String], nearby_venues: Option[String], tags: Option[String])

// venues
case class Venues(results: List[Venue])
case class Venue(id: Long, name: String, category: String, telephone: Option[String], mobile: Option[String], website_url: Option[String], facebook_url: Option[String], twitter: Option[String], tagline: Option[String], rating: Option[Double], url: Option[String], created_at: String, updated_at: Option[String], address: Address, plan: String, images: Images)
case class Address(street: String, zipcode: String, city: String, region: String, country: String)
case class Images(original: List[String])

// reviews
case class Reviews(results: List[Review])
case class Review(body: Option[String], author: Option[Author], scores: Option[Scores], created_at: Option[String], updated_at: Option[String], rating: Option[Double])
case class Author(name: Option[String], email: Option[String])
case class Scores(food: Option[Double], ambiance: Option[Double], service: Option[Double], value: Option[Double])
case class CreatedAt(createdAt: Option[String])

trait Marshallers extends DefaultJsonProtocol {
  implicit val countersFormat = jsonFormat1(Counters)
  implicit val resourcesFormat = jsonFormat3(Resources)
  implicit val geolocationFormat = jsonFormat2(Geolocation)
  implicit val locationJsonFormat = jsonFormat9(Location)
  implicit val locationsJsonFormat = jsonFormat1(Locations)

  implicit val imagesFormat = jsonFormat1(Images)
  implicit val addressFormat = jsonFormat5(Address)
  implicit val venueFormat = jsonFormat16(Venue)
  implicit val venuesFormat = jsonFormat1(Venues)

  implicit val authorJsonFormat = jsonFormat2(Author)
  implicit val scoresJsonFormat = jsonFormat4(Scores)
  implicit val reviewJsonFormat = jsonFormat6(Review)
  implicit val reviewsJsonFormat = jsonFormat1(Reviews)
}

trait EetNuClient {
  def venuesByZipcode(zipcode: String): Future[List[Venue]]

  def venuesByZipcode[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(String, T), (List[Venue], T), NotUsed]

  def venuesByGeo(lat: String, lon: String): Future[List[Venue]]

  def venuesByGeo[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(LatLon, T), (List[Venue], T), NotUsed]

  def venuesByQuery(query: String): Future[List[Venue]]

  def venueById(id: String): Future[Option[Venue]]

  def venueById[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(String, T), (Option[Venue], T), NotUsed]

  def reviewsByVenueId(id: String): Future[List[Review]]

  def reviewsByVenueId[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(String, T), (List[Review], T), NotUsed]
}

/**
 * see: https://docs.eet.nu/
 * see: https://api.eet.nu/
 */
object EetNuClient {
  import spray.json._
  val ZipcodeWithoutSpacePattern: Regex = """([1-9][0-9]{3})([A-Za-z]{2})""".r
  val ZipcodeWithSpacePattern: Regex = """([1-9][0-9]{3})[\s]([A-Za-z]{2})""".r
  def normalizeZipcode(zipcode: String): Option[String] = zipcode.toUpperCase match {
    case ZipcodeWithoutSpacePattern(numbers, letters) => Option(s"$numbers $letters")
    case ZipcodeWithSpacePattern(numbers, letters)    => Option(s"$numbers $letters")
    case _                                            => None
  }

  def responseToString(resp: HttpResponse)(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Future[String] =
    HttpClient.responseToString(resp)

  def asVenues(json: String)(implicit reader: JsonReader[Venues]): Venues =
    json.parseJson.convertTo[Venues]

  def asVenuesFlow[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, reader: JsonReader[Venues]): Flow[(Try[HttpResponse], T), (Option[Venues], T), NotUsed] =
    HttpClient.responseToString[T].map { case (json, id) => (Try(json.parseJson.convertTo[Venues]).toOption, id) }

  def asVenueFlow[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(Option[Venues], T), (List[Venue], T), NotUsed] =
    Flow[(Option[Venues], T)].map { case (venues, id) => (venues.map(_.results).getOrElse(Nil), id) }

  def responseToVenueFlow[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, reader: JsonReader[Venue]): Flow[(Try[HttpResponse], T), (Option[Venue], T), NotUsed] =
    HttpClient.responseToString[T].map { case (json, id) => (Try(json.parseJson.convertTo[Venue]).toOption, id) }

  def asVenue(json: String)(implicit reader: JsonReader[Venue]): Option[Venue] =
    Try(json.parseJson.convertTo[Venue]).toOption

  def asReviews(json: String)(implicit reader: JsonReader[Reviews]): Reviews =
    json.parseJson.convertTo[Reviews]

  def apply()(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, log: LoggingAdapter) = new EetNuClientImpl

  def venuesByQueryRequestFlow[T]: Flow[(String, T), (HttpRequest, T), NotUsed] =
    Flow[(String, T)].map { case (query, id) => (HttpClient.mkGetRequest("/venues", "", Map("query" → query)), id) }

  def venueByIdRequestFlow[T]: Flow[(String, T), (HttpRequest, T), NotUsed] =
    Flow[(String, T)].map { case (vendorId, id) => (HttpClient.mkGetRequest(s"/venues/$vendorId"), id) }

  def venuesByGeoRequestFlow[T]: Flow[(LatLon, T), (HttpRequest, T), NotUsed] =
    Flow[(LatLon, T)].map { case (LatLon(lat, lon), id) => (HttpClient.mkGetRequest("/venues", "", Map("geolocation" → s"$lat,$lon")), id) }

  def reviewsByVenueIdRequestFlow[T]: Flow[(String, T), (HttpRequest, T), NotUsed] =
    Flow[(String, T)].map { case (vendorId, id) => (HttpClient.mkGetRequest(s"/venues/$vendorId/reviews"), id) }

  def asReviewsFlow[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, reader: JsonReader[Reviews]): Flow[(Try[HttpResponse], T), (List[Review], T), NotUsed] =
    HttpClient.responseToString[T].map { case (json, id) => (Try(asReviews(json).results).toOption.getOrElse(Nil), id) }
}

class EetNuClientImpl()(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, log: LoggingAdapter) extends EetNuClient with Marshallers {
  import EetNuClient._

  val client = HttpClient("eetnu")

  override def venuesByZipcode(zipcode: String): Future[List[Venue]] =
    normalizeZipcode(zipcode) match {
      case Some(zip) => venuesByQuery(zip)
      case None      => Future.successful(Nil)
    }

  override def venuesByGeo(lat: String, lon: String): Future[List[Venue]] =
    client.get("/venues", "", Map("geolocation" → s"$lat,$lon"))
      .flatMap(responseToString)
      .map(asVenues)
      .map(_.results)

  override def venuesByQuery(query: String): Future[List[Venue]] =
    client.get("/venues", "", Map("query" → query))
      .flatMap(responseToString)
      .map(asVenues)
      .map(_.results)

  override def venueById(id: String): Future[Option[Venue]] =
    client.get(s"/venues/$id")
      .flatMap(responseToString)
      .map(asVenue)

  override def venueById[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(String, T), (Option[Venue], T), NotUsed] =
    venueByIdRequestFlow[T]
      .via(client.cachedHostConnectionFlow[T])
      .via(responseToVenueFlow[T])

  override def reviewsByVenueId(id: String): Future[List[Review]] =
    client.get(s"/venues/$id/reviews")
      .flatMap(responseToString)
      .map(asReviews)
      .map(_.results)

  override def venuesByZipcode[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(String, T), (List[Venue], T), NotUsed] =
    Flow[(String, T)]
      .map { case (zip, id) => (normalizeZipcode(zip), id) }
      .collect { case (Some(zip), id) => (zip, id) } // drop elements that are no valid zipcodes (no use for them)
      .via(venuesByQueryRequestFlow[T])
      .via(client.cachedHostConnectionFlow[T])
      .via(asVenuesFlow[T])
      .via(asVenueFlow[T])

  override def venuesByGeo[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(LatLon, T), (List[Venue], T), NotUsed] =
    venuesByGeoRequestFlow[T]
      .via(client.cachedHostConnectionFlow[T])
      .via(asVenuesFlow[T])
      .via(asVenueFlow[T])

  override def reviewsByVenueId[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(String, T), (List[Review], T), NotUsed] =
    reviewsByVenueIdRequestFlow[T]
      .via(client.cachedHostConnectionFlow[T])
      .via(asReviewsFlow[T])
}
