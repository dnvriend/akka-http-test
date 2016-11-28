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

import com.github.dnvriend.component.helloworld.controller.dto.HelloWorldDto
import com.github.dnvriend.component.helloworld.repository.HelloWorldRepository
import com.github.dnvriend.component.helloworld.repository.entity.HelloWorld
import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._

class HelloWorldController @Inject() (repo: HelloWorldRepository) extends Controller {
  def getHelloWorld = Action {
    //TODO: read the shapeless book and derive type classes to do this stuff...
    val genericHelloWorld = HelloWorld.generic.to(repo.getHelloWorld)
    val dto = HelloWorldDto.generic.from(genericHelloWorld)
    Ok(Json.toJson(dto))
  }
}
