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

import java.net.URLEncoder

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, FromResponseUnmarshaller, Unmarshal}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.hunorkovacs.koauth.domain.KoauthRequest
import com.hunorkovacs.koauth.service.consumer.{DefaultConsumerService, RequestWithInfo}
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object HttpClientConfig {
  def apply(config: Config): HttpClientConfig =
    HttpClientConfig(
      config.getString("host"),
      Try(config.getInt("port")).getOrElse(80),
      Try(config.getBoolean("tls")).toOption.getOrElse(false),
      Try(config.getString("username")).toOption.find(_.nonEmpty),
      Try(config.getString("password")).toOption.find(_.nonEmpty),
      Try(config.getString("consumerKey")).toOption.find(_.nonEmpty),
      Try(config.getString("consumerSecret")).toOption.find(_.nonEmpty)
    )
}

case class HttpClientConfig(host: String, port: Int, tls: Boolean, username: Option[String], password: Option[String], consumerKey: Option[String], consumerSecret: Option[String])

object HttpClient {
  import scala.collection.JavaConversions._

  /**
   * Creates a new HttpClient and uses the configuration name to look up the connection configuration
   */
  def apply(name: String)(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, log: LoggingAdapter): HttpClient =
    new HttpClient(HttpClientConfig(system.settings.config.getConfig(s"webservices.$name")))

  /**
   * Creates a new HttpClient based on typesafe configuration
   */
  def apply(config: Config)(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, log: LoggingAdapter): HttpClient =
    new HttpClient(HttpClientConfig(config))

  /**
   * Creates a new HttpClient based on the configuration given in the constructor
   */
  def apply(host: String, port: Int, tls: Boolean, username: Option[String] = None, password: Option[String] = None, consumerKey: Option[String] = None, consumerSecret: Option[String] = None)(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext, log: LoggingAdapter): HttpClient =
    new HttpClient(HttpClientConfig(host, port, tls, username, password, consumerKey, consumerSecret))

  /**
   * Translates a string into application/x-www-form-urlencoded format using 'UTF-8'.
   * This method uses the supplied encoding scheme to obtain the bytes for unsafe characters.
   */
  def encode(value: String): String = URLEncoder.encode(value, "UTF-8")

  def responseToString(response: HttpResponse)(implicit /*system: ActorSystem,*/ mat: Materializer, ec: ExecutionContext): Future[String] = response.status match {
    //    case StatusCodes.OK       ⇒ Unmarshal(response.entity).to[String]
    //    case StatusCodes.NotFound ⇒ Unmarshal(response.entity).to[String]
    case status ⇒ Unmarshal(response.entity).to[String]
  }

  def responseTo[T : FromEntityUnmarshaller](response: HttpResponse)(implicit mat: Materializer, ec: ExecutionContext): Future[T] = {
    Unmarshal(response.entity).to[T]
  }

  def responseToString[T](implicit /*system: ActorSystem,*/ mat: Materializer, ec: ExecutionContext): Flow[(Try[HttpResponse], T), (String, T), NotUsed] =
    Flow[(Try[HttpResponse], T)].mapAsync(1) {
      case (Failure(t), e)    ⇒ Future.failed(t)
      case (Success(resp), e) ⇒ responseToString(resp).map(str ⇒ (str, e))
    }

  // Since the project is exemplary and no doubt many people will copy-paste from it (myself included!), it would
  // be nice to point to '.withQuery' as the akka-http way of providing query parameters:
  //
  //  -> http://stackoverflow.com/questions/31929843/query-parameters-for-get-requests-using-akka-http-formally-known-as-spray#answer-31939776
  //
  /*** disabled
  def queryString(queryParams: Map[String, String]): String =
    if (queryParams.nonEmpty)
      "?" + queryParams
        .filterNot {
          case (key, value) ⇒ key.length == 0
        }.mapValues(encode)
        .toList
        .map {
          case (key, value) ⇒ s"$key=$value"
        }.mkString("&")
    else ""
  ***/

  def header(key: String, value: String): Option[HttpHeader] =
    HttpHeader.parse(key, value) match {
      case ParsingResult.Ok(header, errors) ⇒ Option(header)
      case _                                ⇒ None
    }

  def headers(headersMap: Map[String, String]): List[HttpHeader] =
    headersMap.flatMap {
      case (key, value) ⇒ header(key, value)
    }.toList

  /**
   * A cached host connection pool Flow
   */
  def cachedConnection[T](host: String, port: Int)(implicit system: ActorSystem, mat: Materializer): Flow[(HttpRequest, T), (Try[HttpResponse], T), Http.HostConnectionPool] =
    Http().cachedHostConnectionPool[T](host, port)

  /**
   * An encrypted cached host connection pool Flow
   */
  def cachedTlsConnection[T](host: String, port: Int)(implicit system: ActorSystem, mat: Materializer): Flow[(HttpRequest, T), (Try[HttpResponse], T), Http.HostConnectionPool] =
    Http().cachedHostConnectionPoolHttps[T](host, port)

  /**
   * An encrypted HTTP client connection to the given endpoint.
   */
  def tlsConnection(host: String, port: Int)(implicit system: ActorSystem): Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
    Http().outgoingConnectionHttps(host, port)

  /**
   * A HTTP client connection to the given endpoint.
   */
  def httpConnection(host: String, port: Int)(implicit system: ActorSystem): Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
    Http().outgoingConnection(host, port)

  def cachedConnection[T](config: HttpClientConfig)(implicit system: ActorSystem, mat: Materializer): Flow[(HttpRequest, T), (Try[HttpResponse], T), Http.HostConnectionPool] =
    if (config.tls) cachedTlsConnection(config.host, config.port) else
      cachedConnection(config.host, config.port)

  /**
   * Returns a flow that will be configured based on the client's config, that accepts HttpRequest elements and outputs HttpResponse elements
   * It materializes a Future[Http.OutgoingConnection]
   */
  def connection(config: HttpClientConfig)(implicit system: ActorSystem): Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
    if (config.tls) tlsConnection(config.host, config.port) else
      httpConnection(config.host, config.port)

  def cachedConnectionPipeline[T](config: HttpClientConfig)(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(HttpRequest, T), (Try[HttpResponse], T), NotUsed] =
    Flow[(HttpRequest, T)].mapAsync(1) {
      case (request, id) ⇒ addCredentials(config)(request).map(req ⇒ (req, id))
    }.via(cachedConnection(config))

  def singleRequestPipeline(request: HttpRequest, config: HttpClientConfig)(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Future[HttpResponse] =
    Source.single(request)
      .mapAsync(1)(addCredentials(config))
      .via(connection(config))
      .runWith(Sink.head)

  def basicAuthenticationCredentials(username: String, password: String)(implicit ec: ExecutionContext): HttpRequest ⇒ Future[HttpRequest] = { request ⇒
    Future.successful(RequestBuilding.addCredentials(BasicHttpCredentials(username, password))(request))
  }

  def oneLeggedOAuth1Credentials(uri: String, consumerKey: String, consumerSecret: String, tls: Boolean, host: String)(implicit ec: ExecutionContext): HttpRequest ⇒ Future[HttpRequest] = { request ⇒
    val scheme: String = if (tls) "https" else "http"
    def consumerService = new DefaultConsumerService(ec)
    // please note that the used URL (and request params) must be the same as the request we send the request to!!
    def koAuthRequest(url: String) = KoauthRequest("GET", url, None, None)
    def oAuthHeader(uri: String): Future[RequestWithInfo] = consumerService.createOauthenticatedRequest(koAuthRequest(s"$scheme://$host$uri"), consumerKey, consumerSecret, "", "")
    oAuthHeader(uri).map { oauth ⇒
      request.addHeader(header("Authorization", oauth.header).orNull)
    }
  }

  private def addCredentials(config: HttpClientConfig)(request: HttpRequest)(implicit ec: ExecutionContext): Future[HttpRequest] = config match {
    case HttpClientConfig(_, _, _, Some(username), Some(password), None, None) ⇒
      basicAuthenticationCredentials(username, password)(ec)(request)
    case HttpClientConfig(_, _, _, None, None, Some(consumerKey), Some(consumerSecret)) ⇒
      oneLeggedOAuth1Credentials(request.uri.toString(), consumerKey, consumerSecret, config.tls, config.host)(ec)(request)
    case _ ⇒ Future.successful(request)
  }

  def mkEntity(body: String): HttpEntity.Strict = HttpEntity(ContentTypes.`application/json`, body)

  def mkRequest(requestBuilder: RequestBuilding#RequestBuilder, url: String, body: String = "", queryParamsMap: Map[String, String] = Map.empty, headersMap: Map[String, String] = Map.empty): HttpRequest =
    requestBuilder(Uri(url).withQuery(Query(queryParamsMap)), mkEntity(body)).addHeaders(headers(headersMap))

  def mkGetRequest(url: String, body: String = "", queryParamsMap: Map[String, String] = Map.empty, headersMap: Map[String, String] = Map.empty) =
    mkRequest(RequestBuilding.Get, url, body, queryParamsMap, headersMap)

  def mkPostRequest(url: String, body: String = "", queryParamsMap: Map[String, String] = Map.empty, headersMap: Map[String, String] = Map.empty) =
    mkRequest(RequestBuilding.Post, url, body, queryParamsMap, headersMap)
}

class HttpClient(val config: HttpClientConfig)(implicit val system: ActorSystem, val log: LoggingAdapter, val ec: ExecutionContext, val mat: Materializer) extends RequestBuilding {
  import HttpClient._

  /**
   * A cached host connection pool Flow that will be configured based on the client configuration. It accepts 'tagged' tuples of
   * (HttpRequest, T) elements and outputs 'tagged' tuples of (Try[HttpResponse], T) elements
   */
  def cachedHostConnectionFlow[T](implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Flow[(HttpRequest, T), (Try[HttpResponse], T), NotUsed] =
    cachedConnectionPipeline(config)

  def get(url: String, body: String = "", queryParamsMap: Map[String, String] = Map.empty, headersMap: Map[String, String] = Map.empty): Future[HttpResponse] =
    singleRequestPipeline(mkRequest(RequestBuilding.Get, url, body, queryParamsMap, headersMap), config)

  def post(url: String, body: String = "", queryParamsMap: Map[String, String] = Map.empty, headersMap: Map[String, String] = Map.empty): Future[HttpResponse] =
    singleRequestPipeline(mkRequest(RequestBuilding.Post, url, body, queryParamsMap, headersMap), config)

  def put(url: String, body: String = "", queryParamsMap: Map[String, String] = Map.empty, headersMap: Map[String, String] = Map.empty): Future[HttpResponse] =
    singleRequestPipeline(mkRequest(RequestBuilding.Put, url, body, queryParamsMap, headersMap), config)

  def patch(url: String, body: String = "", queryParamsMap: Map[String, String] = Map.empty, headersMap: Map[String, String] = Map.empty): Future[HttpResponse] =
    singleRequestPipeline(mkRequest(RequestBuilding.Patch, url, body, queryParamsMap, headersMap), config)

  def delete(url: String, body: String = "", queryParamsMap: Map[String, String] = Map.empty, headersMap: Map[String, String] = Map.empty): Future[HttpResponse] =
    singleRequestPipeline(mkRequest(RequestBuilding.Delete, url, body, queryParamsMap, headersMap), config)

  def options(url: String, body: String = "", queryParamsMap: Map[String, String] = Map.empty, headersMap: Map[String, String] = Map.empty): Future[HttpResponse] =
    singleRequestPipeline(mkRequest(RequestBuilding.Options, url, body, queryParamsMap, headersMap), config)

  def head(url: String, body: String = "", queryParamsMap: Map[String, String] = Map.empty, headersMap: Map[String, String] = Map.empty): Future[HttpResponse] =
    singleRequestPipeline(mkRequest(RequestBuilding.Head, url, body, queryParamsMap, headersMap), config)
}
