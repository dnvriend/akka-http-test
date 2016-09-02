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

package com.github.dnvriend

import akka.http.scaladsl.marshalling.{ Marshaller, ToResponseMarshaller }
import akka.http.scaladsl.model.{ HttpEntity, _ }
import com.github.dnvriend.DisjunctionMarshaller.{ ErrorMessage, FatalError }
import spray.json.JsonWriter

import scalaz._
import Scalaz._

object DisjunctionMarshaller {
  trait ErrorMessage { def description: String }
  trait FatalError extends ErrorMessage
  trait NonFatalError extends ErrorMessage
}

trait DisjunctionMarshaller {
  type DisjunctionNel[A, +B] = Disjunction[NonEmptyList[A], B]

  implicit def disjunctionMarshaller[A1, A2, B](implicit m1: Marshaller[A1, B], m2: Marshaller[A2, B]): Marshaller[Disjunction[A1, A2], B] = Marshaller { implicit ec =>
    {
      case -\/(a1) => m1(a1)
      case \/-(a2) => m2(a2)
    }
  }

  implicit def errorDisjunctionMarshaller[A](implicit w1: JsonWriter[A], w2: JsonWriter[List[String]]): ToResponseMarshaller[DisjunctionNel[String, A]] =
    Marshaller.withFixedContentType(MediaTypes.`application/json`) {
      case -\/(errors) => HttpResponse(
        status = StatusCodes.BadRequest,
        entity = HttpEntity(ContentType(MediaTypes.`application/json`), w2.write(errors.toList).compactPrint)
      )
      case \/-(success) => HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), w1.write(success).compactPrint))
    }

  implicit def errorMessageDisjunctionMarshaller[A <: ErrorMessage, B](implicit w1: JsonWriter[B], w2: JsonWriter[List[String]]): ToResponseMarshaller[DisjunctionNel[A, B]] = {
    def createResponseWithStatusCode(code: StatusCode, errors: List[ErrorMessage]) = HttpResponse(
      status = code,
      entity = HttpEntity(ContentType(MediaTypes.`application/json`), w2.write(errors.map(_.description)).compactPrint)
    )
    Marshaller.withFixedContentType(MediaTypes.`application/json`) {
      case -\/(errors) if errors.toList.exists(_.isInstanceOf[FatalError]) => createResponseWithStatusCode(StatusCodes.InternalServerError, errors.toList)
      case -\/(errors)                                                     => createResponseWithStatusCode(StatusCodes.BadRequest, errors.toList)
      case \/-(success)                                                    => HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), w1.write(success).compactPrint))
    }
  }
}

trait ValidationMarshaller {
  implicit def validationMarshaller[A1, A2, B](implicit m1: Marshaller[A1, B], m2: Marshaller[A2, B]): Marshaller[Validation[A1, A2], B] = Marshaller { implicit ec =>
    {
      case Failure(a1) => m1(a1)
      case Success(a2) => m2(a2)
    }
  }

  implicit def errorValidationMarshaller[A](implicit w1: JsonWriter[A], w2: JsonWriter[List[String]]): ToResponseMarshaller[ValidationNel[String, A]] =
    Marshaller.withFixedContentType(MediaTypes.`application/json`) {
      case Failure(errors) => HttpResponse(
        status = StatusCodes.BadRequest,
        entity = HttpEntity(ContentType(MediaTypes.`application/json`), w2.write(errors.toList).compactPrint)
      )
      case Success(success) => HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), w1.write(success).compactPrint))
    }

  implicit def errorMessageValidationMarshaller[A <: ErrorMessage, B](implicit w1: JsonWriter[B], w2: JsonWriter[List[String]]): ToResponseMarshaller[ValidationNel[A, B]] = {
    def createResponseWithStatusCode(code: StatusCode, errors: List[ErrorMessage]) = HttpResponse(
      status = code,
      entity = HttpEntity(ContentType(MediaTypes.`application/json`), w2.write(errors.map(_.description)).compactPrint)
    )
    Marshaller.withFixedContentType(MediaTypes.`application/json`) {
      case Failure(errors) if errors.toList.exists(_.isInstanceOf[FatalError]) => createResponseWithStatusCode(StatusCodes.InternalServerError, errors.toList)
      case Failure(errors)                                                     => createResponseWithStatusCode(StatusCodes.BadRequest, errors.toList)
      case Success(success)                                                    => HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), w1.write(success).compactPrint))
    }
  }
}
