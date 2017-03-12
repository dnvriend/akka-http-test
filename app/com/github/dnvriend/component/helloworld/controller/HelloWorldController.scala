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

package com.github.dnvriend.component.helloworld.controller

import com.github.dnvriend.component.helloworld.repository.HelloWorldRepository
import com.google.inject.Inject
import play.api.mvc._
import scalaz._
import Scalaz._

// Why does  this work; the result type of getByIdD is a Disjunction[String, HelloWorld]..
//
// This is because of the implicit conversion from HelloWorld to a Result type
//
// Action assumes either a 'play.api.mvc.Result' or a function, lets call that function
// 'f' that converts Request => Result. Here we transform the type Disjunction[String, HelloWorld]
// to a 'play.api.mvc.Result'.
//
// You should look at the HelloWorld entity to read more.
//
class HelloWorldController @Inject() (repo: HelloWorldRepository) extends Controller {
  def getHelloWorld = Action { request =>
    val header: String = request.headers.get("X-ExampleFilter").getOrElse("No header")
    val msg = repo.getHelloWorld
    msg.copy(msg = s"${msg.msg} - $header")
  }
  def getHelloWorldOpt(id: Long) = Action(repo.getById(id))
  def getHelloWorldMB(id: Long) = Action(repo.getById(id).toMaybe)
  def getHelloWorldD(id: Long) = Action(repo.getByIdD(id))
  def getHelloWorldV(id: Long) = Action(repo.getByIdD(id).validation)
  def getHelloWorldVN(id: Long) = Action(repo.getByIdD(id).validationNel)
}
