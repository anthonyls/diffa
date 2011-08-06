/**
 * Copyright (C) 2010-2011 LShift Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lshift.diffa.agent.rest

import net.lshift.diffa.kernel.diag.DiagnosticsManager
import net.lshift.diffa.docgen.annotations.MandatoryParams.MandatoryParam
import javax.ws.rs.core.Response
import net.lshift.diffa.docgen.annotations.{OptionalParams, MandatoryParams, Description}
import net.lshift.diffa.docgen.annotations.OptionalParams.OptionalParam
import javax.ws.rs._
import net.lshift.diffa.kernel.frontend.Configuration

/**
 * Resource providing REST-based access to diagnostic data.
 */
class DiagnosticsResource(val diagnostics: DiagnosticsManager,
                          val config: Configuration,
                          val domain:String) {


  @GET
  @Path("/{pairKey}/log")
  @Produces(Array("application/json"))
  @Description("Retrieves the most recent log entries for the given pair.")
  @MandatoryParams(Array(new MandatoryParam(name = "pairKey", datatype = "string", description = "Pair Key")))
  @OptionalParams(Array(new OptionalParam(name = "maxItems", datatype = "integer", description = "Maximum number of returned entries")))
  def getPairStates(@PathParam("pairKey") pairKey: String, @QueryParam("maxItems") maxItems:java.lang.Integer): Response = {
    val actualMaxItems = if (maxItems == null) 20 else maxItems.intValue()
    val pair = config.getPair(domain, pairKey)
    val events = diagnostics.queryEvents(pair, actualMaxItems)
    Response.ok(scala.collection.JavaConversions.seqAsJavaList(events)).build
  }
}