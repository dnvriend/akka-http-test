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

import javax.inject._

import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{ RequestHeader, Result }
import play.api.routing.Router

import scala.concurrent.Future

@Singleton
class ErrorHandler @Inject() (env: Environment, config: Configuration, sourceMapper: OptionalSourceMapper, router: Provider[Router]) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {
  override protected def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(NotFound(Json.obj("path" -> request.path, "messages" -> List("not found", message))))
  }

  override protected def onDevServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    Future.successful(InternalServerError(Json.obj("path" -> request.path, "messages" -> List(exception.description))))
  }

  override protected def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    Future.successful(InternalServerError(Json.obj("path" -> request.path, "messages" -> List(exception.description))))
  }

  override protected def onForbidden(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(Forbidden(Json.obj("path" -> request.path, "messages" -> List("forbidden", message))))
  }
}
