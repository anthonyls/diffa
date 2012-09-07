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

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs._
import core.{UriInfo, Context}
import net.lshift.diffa.kernel.client.ActionsClient
import net.lshift.diffa.kernel.diag.DiagnosticsManager
import net.lshift.diffa.kernel.actors.PairPolicyClient
import net.lshift.diffa.kernel.frontend.{Changes, Configuration}
import net.lshift.diffa.kernel.reporting.ReportManager
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.slf4j.LoggerFactory
import net.lshift.diffa.kernel.util.AlertCodes._
import net.lshift.diffa.kernel.config.system.CachedSystemConfigStore
import net.lshift.diffa.kernel.limiting.DomainRateLimiterFactory
import net.lshift.diffa.agent.rest.ResponseUtils._
import net.lshift.diffa.kernel.frontend.EscalationDef
import net.lshift.diffa.kernel.differencing.{DomainDifferenceStore, DifferencesManager}
import net.lshift.diffa.kernel.config._
import org.springframework.security.access.PermissionEvaluator
import net.lshift.diffa.kernel.config.User
import net.lshift.diffa.kernel.frontend.EscalationDef

/**
 * The policy is that we will publish spaces as the replacement term for domains
 * but to avoid having to refactor a bunch of of code straight away, we'll just change
 * the path specification from /domains to /spaces and implement a redirect.
 */

/*
 * NOTE TO MAINTENANCE ENGINEER:
 *
 * In the version of Jersey that this resource was coded against (1.13), you cannot seem to specify
 * Path("/spaces/{space:.+}") on the class resource - this just results in a non-match and the framework
 * returns a 405 to the client. However, if you specify the {space:.+} regex on a method level,
 *
 * I have also tried some less greedy regexes (such as Path("/spaces/{space:(([^/]+/)+)} ),
 * but these don't seem to play well with the URI consuming rules in Jersey,
 *
 * Furthermore, the Spring PreAuthorize only seems to get woven in properly at a class level. At a method
 * level it is woven in, but for some strange reason it cannot access the method parameters using SpEL,
 * so I opted to invoke the authorization programmatically, which is reasonably terse.
 *
 * For the sake of clarity, it might be an idea to see whether Jersey 2.0 can handle this better.
 */
@Path("/spaces/")
@Component
class DomainResource {

  val log = LoggerFactory.getLogger(getClass)

  @Context var uriInfo:UriInfo = null

  @Autowired var config:Configuration = null
  @Autowired var credentialsManager:DomainCredentialsManager = null
  @Autowired var actionsClient:ActionsClient = null
  @Autowired var differencesManager:DifferencesManager = null
  @Autowired var diagnosticsManager:DiagnosticsManager = null
  @Autowired var pairPolicyClient:PairPolicyClient = null
  @Autowired var domainConfigStore:DomainConfigStore = null
  @Autowired var systemConfigStore:CachedSystemConfigStore = null
  @Autowired var changes:Changes = null
  @Autowired var changeEventRateLimiterFactory: DomainRateLimiterFactory = null
  @Autowired var reports:ReportManager = null
  @Autowired var diffStore:DomainDifferenceStore = null
  @Autowired var breakers:BreakerHelper = null
  @Autowired var permissionEvaluator:PermissionEvaluator = null

  private def getCurrentUser(space:String) : String = SecurityContextHolder.getContext.getAuthentication.getPrincipal match {
    case user:UserDetails => user.getUsername
    case token:String     => {
      systemConfigStore.getUserByToken(token) match {
        case user:User => user.getName
        case _         =>
          log.warn(formatAlertCode(space, SPURIOUS_AUTH_TOKEN) + " " + token)
          null
      }
    }
    case _                => null
  }

  private def withSpace[T](path: String, f: Long => T) =  {
    val authentication = SecurityContextHolder.getContext.getAuthentication
    val hasPermission = permissionEvaluator.hasPermission(authentication, path, "domain-user")
    if (hasPermission) {
      val space = try {
        systemConfigStore.lookupSpaceByPath(path)
      } catch {
        case ex => Space(id = path.toLong)
      }
      f(space.id)
    }
    else {
      throw new WebApplicationException(403)
    }
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // The following routes are implemented within the context of this top level resource.
  //
  // These preceding resources would ideally be implemented in an appropriate sub-resource, but due to the eager
  // matching of {space:.+}, if you want to match a trailing pattern in a sub-resource that has the same name as the
  // as a top level API pattern, then you need to specify the very specific match in this class, in order to guarantee
  // match precedence.
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @POST
  @Path("/{space:.+}/config/pairs/{id}/escalations")
  @Consumes(Array("application/json"))
  def createEscalation(@Context uri:UriInfo,
                       @PathParam("space") space:String,
                       @PathParam("id") id:String,
                       e: EscalationDef) = {
    withSpace(space, (spaceId:Long) => {
      config.createOrUpdateEscalation(spaceId, id, e)
      resourceCreated(e.name, uri)
    })
  }

  @DELETE
  @Path("/{space:.+}/config/pairs/{pairKey}/escalations/{name}")
  def deleteEscalation(@PathParam("space") space:String,
                       @PathParam("name") name: String,
                       @PathParam("pairKey") pairKey: String) = {
    withSpace(space, (id:Long) => {
      config.deleteEscalation(id, name, pairKey)
    })
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // The following routes are implemented by delegating to sub-resources.
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Path("/{space:.+}/config")
  def getConfigResource(@Context uri:UriInfo,
                        @PathParam("space") space:String) =
    withSpace(space, (id:Long) => new ConfigurationResource(config, breakers, id, getCurrentUser(space), uri))

  @Path("/{space:.+}/credentials")
  def getCredentialsResource(@Context uri:UriInfo,
                             @PathParam("space") space:String) =
    withSpace(space, (id:Long) => new CredentialsResource(credentialsManager, id, uri))

  @Path("/{space:.+}/diffs")
  def getDifferencesResource(@Context uri:UriInfo,
                             @PathParam("space") space:String) =
    withSpace(space, (id:Long) => new DifferencesResource(differencesManager, domainConfigStore, id, uri))

  @Path("/{space:.+}/escalations")
  def getEscalationsResource(@PathParam("space") space:String) =
    withSpace(space, (id:Long) => new EscalationsResource(config, diffStore, id))

  @Path("/{space:.+}/actions")
  def getActionsResource(@Context uri:UriInfo,
                         @PathParam("space") space:String) =
    withSpace(space, (id:Long) => new ActionsResource(actionsClient, id, uri))

  @Path("/{space:.+}/reports")
  def getReportsResource(@Context uri:UriInfo,
                         @PathParam("space") space:String) =
    withSpace(space, (id:Long) => new ReportsResource(domainConfigStore, reports, id, uri))

  @Path("/{space:.+}/diagnostics")
  def getDiagnosticsResource(@PathParam("space") space:String) =
    withSpace(space, (id:Long) => new DiagnosticsResource(diagnosticsManager, config, id))

  @Path("/{space:.+}/scanning")
  def getScanningResource(@PathParam("space") space:String) =
    withSpace(space, (id:Long) => new ScanningResource(pairPolicyClient, config, domainConfigStore, diagnosticsManager, id, getCurrentUser(space)))

  @Path("/{space:.+}/changes")
  def getChangesResource(@PathParam("space") space:String) = {
    withSpace(space, (id:Long) => new ChangesResource(changes, id, changeEventRateLimiterFactory))
  }

  @Path("/{space:.+}/inventory")
  def getInventoryResource(@PathParam("space") space:String) =
    withSpace(space, (id:Long) => new InventoryResource(changes, domainConfigStore, id))

  @Path("/{space:.+}/limits")
  def getLimitsResource(@PathParam("space") space:String) =
    withSpace(space, (id:Long) => new DomainServiceLimitsResource(config, id))
}