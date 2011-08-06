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

package net.lshift.diffa.kernel.differencing

import net.lshift.diffa.kernel.events.VersionID
import reflect.BeanProperty
import net.lshift.diffa.kernel.participants.ParticipantType
import org.joda.time.{Interval, DateTime}
import net.lshift.diffa.kernel.config.{Pair => DiffaPair}

/**
 * A SessionManager provides a stateful view of the differencing of pairs, and provides mechanisms for polling
 * for changes in the difference states of those pairs.
 */
trait SessionManager {

  def defaultDateBounds() = {
    // TODO: [#147] We're currently ignoring these bounds within the DefaultSessionManager, and passing non-null
    //       values in here is only serving to make the sessionIds unstable.

    (null, null)
  }

  /**
   * Calls start/3 with default time bounds
   */
  def start(scope:SessionScope) : String = {
    val (from, until) = defaultDateBounds()
    start(scope, from, until)
  }

  /**
   * Calls start/4 with default time bounds
   */
  def start(scope: SessionScope, listener: DifferencingListener) : String = {
    val (from, until) = defaultDateBounds()
    start(scope, from, until, listener)
  }


  /**
   *  Requests a new session be created for the given pair. The created session will remain active whilstever
   * regular polling occurs on the retrieve* methods.
   * @param scope The requested scope for the session, detailing the pairs that are to be inspected.
   * @param start The lower time bound of the session
   * @param end The upper time bound of the session
   * @return the id of the session
   */
  def start(scope:SessionScope, start:DateTime, end:DateTime):String

  /**
   * Requests a new session be created for the given pair. Any generated events should be provided to the listener.
   * In contrast to start/3, this does not return a sessionId because the state of the session is tied to
   * the listener reference that has been passed in.
   * The created session will remain active until <code>end</code> is called with the listener reference.
   * @param scope The requested scope for the session, detailing the pairs that are to be inspected.
   * @param start The lower time bound of the session
   * @param end The upper time bound of the session
   */
  def start(scope:SessionScope, start:DateTime, end:DateTime, listener:DifferencingListener): String

  /**
   * Retrieves a version for the given session.
   */
  def retrieveSessionVersion(sessionID:String):String

  /**
   *  Retrieves all events known to this session in the given interval. Will only include unmatched events.
   *  @throws InvalidSessionIDException if the requested session does not exist or has expired.
   */
  def retrieveAllEventsInInterval(sessionID:String, interval:Interval) : Seq[SessionEvent]

  /**
   *
   * Retrieves all events known to this session. Will only include unmatched events.
   * This pages the results to only contain events that are contained with the specified interval
   * and returns a subset of the underlying data set that corresponds to the offset and length specified.
   * @throws InvalidSessionIDException if the requested session does not exist or has expired.
   */
  def retrievePagedEvents(sessionId:String, pairKey:String, interval:Interval, offset:Int, length:Int) : Seq[SessionEvent]

  /**
   * Count the number of events for the given pair within the given interval.
   */
  def countEvents(sessionId:String, pairKey:String, interval:Interval) : Int

  /**
   * Retrieves any additional information that the session manager knows about an event (eg, mismatched hashes,
   * differing content bodies). This information may be retrieved by the manager on demand from remote sources, so
   * should generally only be called on explicit user request.
   */
  def retrieveEventDetail(sessionID:String, evtSeqId:String, t:ParticipantType.ParticipantType) : String

  /**
   * Indicates that the given session is no longer required.
   */
  def end(sessionID:String)

  /**
   * Indicates that the given session is no longer required.
   */
  def end(pair:DiffaPair, listener: DifferencingListener)

  /**
   * Informs the session manager that a pair has been updated.
   */
  def onUpdatePair(pair:DiffaPair)
}

class InvalidSessionIDException extends Exception
class InvalidSequenceNumberException(val id:String) extends Exception(id)
class SequenceOutOfDateException extends Exception

case class SessionEvent(
  @BeanProperty var seqId:String,
  @BeanProperty var objId:VersionID,
  @BeanProperty var detectedAt:DateTime,
  @BeanProperty var state:MatchState,
  @BeanProperty var upstreamVsn:String,
  @BeanProperty var downstreamVsn:String) {

 def this() = this(null, null, null, MatchState.UNMATCHED, null, null)
    
}
