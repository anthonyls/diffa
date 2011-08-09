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

import java.io.Closeable
import net.lshift.diffa.kernel.events.VersionID
import org.joda.time.{LocalDate, DateTimeZone, DateTime}
import org.slf4j.LoggerFactory
import net.lshift.diffa.participant.scanning.ScanConstraint
import net.lshift.diffa.kernel.config.{DiffaPairRef, Pair => DiffaPair}

/**
 * Store used for caching version correlation information between a pair of participants.
 */
trait VersionCorrelationStore extends Closeable {

  val logger = LoggerFactory.getLogger(classOf[VersionCorrelationStore])

  type UpstreamVersionHandler = (VersionID, Map[String, String], DateTime, String) => Unit
  type DownstreamVersionHandler = (VersionID, Map[String, String], DateTime, String, String) => Unit

  /**
   * The unique key for the upstream and downstream participant pair
   */
  val pair: DiffaPairRef

  /**
   * Opens a new writer, giving access to write operations on the store.
   */
  def openWriter(): ExtendedVersionCorrelationWriter

  /**
   * Retrieves all of the unmatched version that have been stored.
   * @param usConstraints constraints on the upstream participant's entities
   * @param dsConstraints constraints on the downstream participant's entities
   */
  def unmatchedVersions(usConstraints:Seq[ScanConstraint], dsConstraints:Seq[ScanConstraint]) : Seq[Correlation]

  /**
   * Retrieves the current pairing information for the given pairKey/id.
   */
  def retrieveCurrentCorrelation(id:VersionID):Option[Correlation]

  /**
   * Queries for all upstream versions for the given pair based on the given constraints.
   */
  def queryUpstreams(constraints:Seq[ScanConstraint], handler:UpstreamVersionHandler):Unit = {
    queryUpstreams(constraints).foreach(c => {
      val version = VersionID(DiffaPairRef(c.pairing, c.domain), c.id)
      val attributes = c.upstreamAttributes.toMap
      if (logger.isTraceEnabled) {
        logger.trace("US: version = %s; attributes = %s; lastUpdate = %s; uvsn = %s".format(version, attributes, c.lastUpdate, c.upstreamVsn))
      }
      handler(version, attributes, c.lastUpdate, c.upstreamVsn)
    })
  }

  /**
   * Queries for all downstream versions for the given pair based on the given constraints.
   */
  def queryDownstreams(constraints:Seq[ScanConstraint], handler:DownstreamVersionHandler) : Unit = {
    queryDownstreams(constraints).foreach(c => {
      val version = VersionID(DiffaPairRef(c.pairing, c.domain), c.id)
      val attributes = c.downstreamAttributes.toMap
      if (logger.isTraceEnabled) {
        logger.trace("DS: version = %s; attributes = %s; lastUpdate = %s; uvsn = %s; dvsn = %s".format(version, attributes, c.lastUpdate, c.upstreamVsn, c.downstreamDVsn))
      }
      handler(version, c.downstreamAttributes.toMap, c.lastUpdate, c.downstreamUVsn, c.downstreamDVsn)
    })
  }

  /**
   * Queries for all upstream versions for the given pair based on the given constraints.
   */
  def queryUpstreams(constraints:Seq[ScanConstraint]) : Seq[Correlation]

  /**
   * Queries for all downstream versions for the given pair based on the given constraints.
   */
  def queryDownstreams(constraints:Seq[ScanConstraint]) : Seq[Correlation]
}

object VersionCorrelationStore {
  val schemaVersionKey = "correlationStore.schemaVersion"
  val currentSchemaVersion = 1
}

/**
 * Issues write commands to an underlying correlation store.
 */
trait LimitedVersionCorrelationWriter {

  /**
   *   Stores the details of an upstream version.
   */
  def storeUpstreamVersion(id:VersionID, attributes:Map[String,TypedAttribute], lastUpdated:DateTime, vsn:String):Correlation

  /**
   * Stores the details of a downstream version.
   */
  def storeDownstreamVersion(id:VersionID, attributes:Map[String,TypedAttribute], lastUpdated:DateTime, uvsn:String, dvsn:String):Correlation

  /**
   * Clears the upstream version for the given pairKey/id.
   */
  def clearUpstreamVersion(id:VersionID):Correlation

  /**
   * Clears the downstream version for the given pairKey/id.
   */
  def clearDownstreamVersion(id:VersionID):Correlation
}

/**
 * Provides the ability to batch writes to the underlying store.
 * Extends the high level <code>LimitedVersionCorrelationWriter</code> interface by adding low level commands
 * to commit queued up write commands into the store.
 */
trait ExtendedVersionCorrelationWriter extends LimitedVersionCorrelationWriter {
  /**
   * Indicates whether there are any pending changes to be flushed to the store.
   */
  def isDirty: Boolean

  /**
   * Flushes any pending changes to the store, making them permanent.
   */
  def flush(): Unit

  /**
   * Rolls back any pending changes to the store and deletes any temporary data.
   */
  def rollback() : Unit
}

/**
 * Creates a specific implementation of a VersionCorrelationStore for a given pair key.
 */
trait VersionCorrelationStoreFactory extends Closeable {

  def apply(pair: DiffaPairRef): VersionCorrelationStore

  def remove(pair: DiffaPairRef): Unit
}

abstract class TypedAttribute { def value:String }
case class StringAttribute(value:String) extends TypedAttribute
case class DateTimeAttribute(date:DateTime) extends TypedAttribute {
  def value = date.withZone(DateTimeZone.UTC).toString()
}
case class DateAttribute(date:LocalDate) extends TypedAttribute {
  def value = date.toString()
}
case class IntegerAttribute(int:Int) extends TypedAttribute {
  def value = int.toString
}

