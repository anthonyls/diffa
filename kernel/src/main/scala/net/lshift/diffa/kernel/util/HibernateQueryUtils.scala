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

package net.lshift.diffa.kernel.util

import net.lshift.diffa.kernel.util.SessionHelper._
import net.lshift.diffa.kernel.config._
import net.lshift.diffa.kernel.config.{Pair => DiffaPair}
import org.hibernate.{NonUniqueResultException, Query, Session, SessionFactory}
import org.slf4j.{LoggerFactory, Logger}
import scala.collection.JavaConversions._
import scala.collection.Map

/**
 * Mixin providing a bunch of useful query utilities for stores.
 */
trait HibernateQueryUtils {
  def sessionFactory:SessionFactory

  protected val log:Logger = LoggerFactory.getLogger(getClass)

  /**
   * Executes a list query in the given session, forcing the result type into a typed list of the given
   * return type.
   */
  def listQuery[ReturnType](s: Session, queryName: String, params: Map[String, Any]): Seq[ReturnType] = {
    sessionFactory.withSession(s => {
      val query: Query = s.getNamedQuery(queryName)
      params foreach {case (param, value) => query.setParameter(param, value)}
      query.list map (item => item.asInstanceOf[ReturnType])
    })
  }

  /**
   * Executes a query that is expected to return a single result in the given session. Throws an exception if the
   * requested object is not available.
   */
  def singleQuery[ReturnType](s: Session, queryName: String, params: Map[String, Any], entityName: String): ReturnType = {
    singleQueryOpt(s, queryName, params) match {
      case None => throw new MissingObjectException(entityName)
      case Some(x) => x
    }
  }

  /**
   * Executes a query that may return a single result in the current session. Returns either None or Some(x) for the
   * object.
   */
  def singleQueryOpt[ReturnType](s:Session, queryName: String, params: Map[String, Any]): Option[ReturnType] = {

    val query: Query = s.getNamedQuery(queryName)
    params foreach {case (param, value) => query.setParameter(param, value)}
    try {
      query.uniqueResult match {
        case null => None
        case r: ReturnType => Some(r)
      }
    }
    catch {
      case e: NonUniqueResultException => {
        log.warn("Non unique result for: " + queryName)
        params.foreach(p => log.debug("Key: [" + p._1 + "], Value: [" + p._2 + "]" ))
        log.debug("Logging stack trace for non unique result", e)
        None
      }
    }
  }

  /**
   * Returns a domain by its name
   */
  def getDomain(name: String) = sessionFactory.withSession(s => {
    singleQuery[Domain](s, "domainByName", Map("domain_name" -> name), "domain %s".format(name))
  })

  /**
   * This is un-protected call to set a configuration option.
   * It is up to the calling context to establish this is authorized.
   */
  def writeConfigOption(domainName:String, key:String, value:String) = sessionFactory.withSession(s => {
    val domain = getDomain(domainName)
    val scopedKey = DomainScopedKey(key, domain)
    val co = s.get(classOf[ConfigOption], scopedKey) match {
      case null =>
        new ConfigOption(domain = domain, key = key, value = value)
      case current:ConfigOption =>  {
        current.value = value
        current
      }
    }
    s.saveOrUpdate(co)
  })

  /**
   * This is un-protected call to clear a configuration option.
   * It is up to the calling context to establish this is authorized.
   */
  def deleteConfigOption(domain:String, key:String) = sessionFactory.withSession(s => {
    val scopedKey = DomainScopedKey(key, getDomain(domain))
    s.get(classOf[ConfigOption], scopedKey) match {
      case null =>
      case current:ConfigOption =>  s.delete(current)
    }
  })

  def getEndpoint(s: Session, domain:String, name: String) = singleQuery[Endpoint](s, "endpointByName", Map("name" -> name), "endpoint %s".format(name))

  def getUser(s: Session, name: String) = singleQuery[User](s, "userByName", Map("name" -> name), "user %s".format(name))

  def getPair(s: Session, domain:String, key: String) = singleQuery[DiffaPair](s, "pairByKey", Map("key" -> key), "pair %s".format(key))

  def getRepairAction(s: Session, domain:String, name: String, pairKey: String) =
    singleQuery[RepairAction](s, "repairActionsByNameAndPair",
                              Map("name" -> name, "pair_key" -> pairKey, "domain_name" -> domain),
                              "repair action %s for pair %s in domain %s".format(name, pairKey, domain))

  def getEscalation(s: Session, domain:String, name: String, pairKey: String) =
    singleQuery[Escalation](s, "escalationsByNameAndPair",
                            Map("name" -> name, "pair_key" -> pairKey, "domain_name" -> domain),
                            "esclation %s for pair %s in domain %s".format(name, pairKey, domain))

}