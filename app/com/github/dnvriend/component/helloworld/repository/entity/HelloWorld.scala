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

package com.github.dnvriend.component.helloworld.repository.entity

import play.api.libs.json.{ Format, Json }
import play.api.mvc.{ Result, Results }

import scala.language.implicitConversions
import scalaz._
import Scalaz._

// The implicit resolution rules are
// 1. First look in current scope
// - Implicits defined in current scope
// - Explicit imports
// - wildcard imports
// - _(*) Same scope in other files (*)_
//
// We haven't imported anything in the HelloWorldController..
//
// 2. Now look at associated types in
// - Companion objects of a type
// - Implicit scope of an argument's type (2.9.1)
// - Implicit scope of type arguments (2.8.0)
// - Outer objects for nested types
//
// If we returned an HelloWorld type in the controller, the companion object
// (which is object HelloWorld) would be responsible to convert HelloWorld to a result
// that can be done by means of one of the implicit methods eg. toResult
//
// If we return an Option[HelloWorld], the resolution would first to look at the companion of Option.. no luck there
// next we will look in the 'Implicit scope of an argument type' of Option[T] if we can convert Option[HelloWorld] to
// 'Result', the argument type is 'HelloWorld' and if we look at the companion object of HelloWorld we have a hit.
// because we find a way to convert Option[HelloWorld] to a Result.
//
object HelloWorld extends GenericResult {
  implicit val format: Format[HelloWorld] = Json.format[HelloWorld]
}

final case class HelloWorld(msg: String)

trait GenericResult extends Results {
  implicit def fromA[A: Format](a: A): Result =
    Ok(Json.toJson(a))
  implicit def fromOption[A: Format](option: Option[A]): Result =
    option.map(a => fromA(a)).getOrElse(NotFound)
  implicit def fromMaybe[A: Format](maybe: Maybe[A]): Result =
    maybe.toOption
  implicit def fromDisjunction[A: Format](disjunction: Disjunction[String, A]): Result =
    disjunction.map(a => fromA(a)).valueOr(msg => NotFound(msg))
  implicit def fromValidation[A: Format](validation: Validation[String, A]): Result =
    validation.disjunction
  implicit def fromValidationNel[A: Format](validation: ValidationNel[String, A]): Result =
    validation.leftMap(_.toList.mkString(",")).disjunction
}
