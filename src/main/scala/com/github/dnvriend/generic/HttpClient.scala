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

package com.github.dnvriend.generic

import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.{ Sink, Source }
import com.github.dnvriend.GenericServices
import com.typesafe.config.Config

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

object HttpClientConfig {
  def apply(config: Config): HttpClientConfig =
    HttpClientConfig(
      config.getString("host"),
      Try(config.getInt("port")).getOrElse(80),
      Try(config.getBoolean("tls")).toOption.getOrElse(false),
      Try(config.getString("username")).toOption.find(_.nonEmpty),
      Try(config.getString("password")).toOption.find(_.nonEmpty),
      Try(config.getString("apikey")).toOption.find(_.nonEmpty))
}

case class HttpClientConfig(host: String, port: Int, tls: Boolean, username: Option[String], password: Option[String], apiKey: Option[String])

object HttpClient {
  def encode(value: String): String = URLEncoder.encode(value, "UTF-8")

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

  def header(key: String, value: String): Option[HttpHeader] =
    HttpHeader.parse(key, value) match {
      case ParsingResult.Ok(header, errors) ⇒ Option(header)
      case _                                ⇒ None
    }

  def headers(headersMap: Map[String, String]): List[HttpHeader] =
    headersMap.flatMap {
      case (key, value) ⇒ header(key, value)
    }.toList

  def apply(host: String, port: Int, tls: Boolean)(implicit system: ActorSystem, mat: Materializer): HttpClient =
    new HttpClientImpl(host, port, tls)
}

trait HttpClient extends GenericServices {
  import HttpClient._

  def name: String

  def config: HttpClientConfig =
    HttpClientConfig(system.settings.config.getConfig(s"http.client.$name"))

  /**
   * An encrypted HTTP client connection to the given endpoint.
   * @param host
   * @param port
   * @return
   */
  private def tlsConnection(host: String, port: Int) =
    Http().outgoingConnectionTls(host, port)

  /**
   * An HTTP client connection to the given endpoint.
   * @param host
   * @param port
   * @return
   */
  private def httpConnection(host: String, port: Int) =
    Http().outgoingConnection(host, port)

  private def connection(config: HttpClientConfig) =
    if (config.tls) tlsConnection(config.host, config.port) else
      httpConnection(config.host, config.port)

  private def requestPipeline(request: HttpRequest): Future[HttpResponse] =
    Source.single(request)
      .map(addCredentials(config))
      .log(s"$name-request")
      .via(connection(config))
      .log(s"$name-response")
      .runWith(Sink.head)

  private def addCredentials(config: HttpClientConfig)(request: HttpRequest): HttpRequest =
    RequestBuilding.addCredentials(BasicHttpCredentials(config.username.getOrElse(""), config.password.getOrElse("")))(request)

  import scala.collection.JavaConversions._
  def get(url: String, queryParamsMap: Map[String, String] = Map.empty, headersMap: Map[String, String] = Map.empty): Future[String] =
    requestPipeline(RequestBuilding.Get(url + queryString(queryParamsMap)).addHeaders(headers(headersMap)))
      .flatMap { response ⇒
        response.status match {
          case StatusCodes.OK       ⇒ Unmarshal(response.entity).to[String]
          case StatusCodes.NotFound ⇒ Unmarshal(response.entity).to[String]
          case _                    ⇒ Future.failed(new RuntimeException("Unknown Error"))
        }
      }
}

class HttpClientImpl(host: String, port: Int, tls: Boolean, username: Option[String] = None, password: Option[String] = None, apikey: Option[String] = None)(implicit val system: ActorSystem, val mat: Materializer) extends HttpClient {

  override def config: HttpClientConfig = HttpClientConfig(host, port, tls, username, password, apikey)

  override def name: String = "generic"

  override implicit val log: LoggingAdapter = Logging(system, this.getClass)

  override implicit val ec: ExecutionContext = system.dispatcher
}
