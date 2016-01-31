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

package com.github.dnvriend.webservices.postcode

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import com.github.dnvriend.webservices.generic.HttpClient
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try
import scala.util.matching.Regex

case class Address(street: String,
  houseNumber: Int,
  houseNumberAddition: String,
  postcode: String,
  city: String,
  municipality: String,
  province: String,
  rdX: Option[Int],
  rdY: Option[Int],
  latitude: Double,
  longitude: Double,
  bagNumberDesignationId: String,
  bagAddressableObjectId: String,
  addressType: String,
  purposes: Option[List[String]],
  surfaceArea: Int,
  houseNumberAdditions: List[String])

trait Marshallers extends DefaultJsonProtocol {
  implicit val addressJsonFormat = jsonFormat17(Address)
}

case class GetAddressRequest(zip: String, houseNumber: String)

trait PostcodeClient {
  def address(postcode: String, houseNumber: Int): Future[Option[Address]]

  def address[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(GetAddressRequest, T), (Option[Address], T), NotUsed]
}

object PostcodeClient {
  import spray.json._
  val ZipcodeWithoutSpacePattern: Regex = """([1-9][0-9]{3})([A-Za-z]{2})""".r
  val ZipcodeWithSpacePattern: Regex = """([1-9][0-9]{3})[\s]([A-Za-z]{2})""".r

  def mapToAddress(json: String)(implicit reader: JsonReader[Address]): Option[Address] =
    Try(json.parseJson.convertTo[Address]).toOption

  def responseToString(resp: HttpResponse)(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Future[String] =
    HttpClient.responseToString(resp)

  def getAddressRequestFlow[T]: Flow[(GetAddressRequest, T), (HttpRequest, T), NotUsed] =
    Flow[(GetAddressRequest, T)].map { case (request, id) ⇒ (HttpClient.mkGetRequest(s"/rest/addresses/${request.zip}/${request.houseNumber}/"), id) }

  def mapResponseToAddressFlow[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, reader: JsonReader[Address]): Flow[(Try[HttpResponse], T), (Option[Address], T), NotUsed] =
    HttpClient.responseToString[T].map { case (json, id) ⇒ (mapToAddress(json), id) }
  /**
   * Returns an option of the zipcode without spaces, if there were any. Any invalid zipcode
   * will be returned as None
   *
   * @param zipcode
   * @return
   */
  def normalizeZipcode(zipcode: String): Option[String] = zipcode.toUpperCase match {
    case ZipcodeWithoutSpacePattern(numbers, letters) ⇒ Option(s"$numbers$letters")
    case ZipcodeWithSpacePattern(numbers, letters)    ⇒ Option(s"$numbers$letters")
    case _                                            ⇒ None
  }

  def apply()(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, log: LoggingAdapter) = new PostcodeClientImpl
}

class PostcodeClientImpl()(implicit val system: ActorSystem, val mat: Materializer, val ec: ExecutionContext, val log: LoggingAdapter) extends PostcodeClient with Marshallers {
  import PostcodeClient._
  private val client = HttpClient("postcode")

  override def address(postcode: String, houseNumber: Int): Future[Option[Address]] =
    normalizeZipcode(postcode) match {
      case Some(zip) ⇒ client.get(s"/rest/addresses/$zip/$houseNumber/")
        .flatMap(responseToString).map(mapToAddress)
      case None ⇒ Future.successful(None)
    }

  override def address[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(GetAddressRequest, T), (Option[Address], T), NotUsed] =
    getAddressRequestFlow[T]
      .via(client.cachedHostConnectionFlow[T])
      .via(mapResponseToAddressFlow[T])
}
