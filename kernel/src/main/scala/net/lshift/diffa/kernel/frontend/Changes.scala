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

package net.lshift.diffa.kernel.frontend

import net.lshift.diffa.kernel.matching.MatchingManager
import org.slf4j.{Logger, LoggerFactory}
import net.lshift.diffa.kernel.events._
import net.lshift.diffa.kernel.actors.PairPolicyClient
import net.lshift.diffa.kernel.config.DomainConfigStore

/**
 * Front-end for reporting changes.
 */
class Changes(val domainConfig:DomainConfigStore,
              val changeEventClient:PairPolicyClient,
              val mm:MatchingManager) {
  private val log:Logger = LoggerFactory.getLogger(getClass)

  /**
   * Indicates that a change has occurred within a participant. Locates the appropriate policy for the pair the
   * event is targeted for, and provides the event to the policy.
   */
  def onChange(domain:String, endpoint:String, evt:ChangeEvent) {
    log.debug("Received change event for %s %s: %s".format(domain, endpoint, evt))

    domainConfig.listPairsForEndpoint(domain, endpoint).foreach(pair => {
      val pairEvt = evt match {
        case UpstreamChangeEvent(id, attributes, lastUpdate, vsn) => UpstreamPairChangeEvent(VersionID(pair.asRef, id), attributes, lastUpdate, vsn)
        case DownstreamChangeEvent(id, attributes, lastUpdate, vsn) => DownstreamPairChangeEvent(VersionID(pair.asRef, id), attributes, lastUpdate, vsn)
        case DownstreamCorrelatedChangeEvent(id, attributes, lastUpdate, uvsn, dvsn) =>
          DownstreamCorrelatedPairChangeEvent(VersionID(pair.asRef, id), attributes, lastUpdate, uvsn, dvsn)
      }

      // TODO: Write a test to enforce that the matching manager processes first. This is necessary to ensure
      //    that the SessionManager doesn't emit spurious events.

      // If there is a matcher available, notify it first
      mm.getMatcher(pair) match {
        case None =>
        case Some(matcher) => matcher.onChange(pairEvt, () => {})
      }

      // Propagate the change event to the corresponding policy
      changeEventClient.propagateChangeEvent(pairEvt)
    })
  }
}