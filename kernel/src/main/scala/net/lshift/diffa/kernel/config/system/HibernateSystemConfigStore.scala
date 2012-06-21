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

package net.lshift.diffa.kernel.config.system

import net.lshift.diffa.kernel.util.db.{DatabaseFacade,HibernateQueryUtils}
import net.lshift.diffa.schema.hibernate.SessionHelper._
import scala.collection.JavaConversions._
import org.slf4j.LoggerFactory
import net.lshift.diffa.kernel.util.{AlertCodes, MissingObjectException}
import org.hibernate.{Query, Session, SessionFactory}
import org.apache.commons.lang.RandomStringUtils
import net.lshift.diffa.kernel.config._
import net.lshift.diffa.schema.jooq.{DatabaseFacade => JooqDatabaseFacade}
import net.lshift.diffa.schema.tables.UserItemVisibility.USER_ITEM_VISIBILITY
import net.lshift.diffa.schema.tables.PairReports.PAIR_REPORTS
import net.lshift.diffa.schema.tables.Escalations.ESCALATIONS
import net.lshift.diffa.schema.tables.RepairActions.REPAIR_ACTIONS
import net.lshift.diffa.schema.tables.PairViews.PAIR_VIEWS
import net.lshift.diffa.schema.tables.Pair.PAIR
import net.lshift.diffa.schema.tables.EndpointViewsCategories.ENDPOINT_VIEWS_CATEGORIES
import net.lshift.diffa.schema.tables.EndpointViews.ENDPOINT_VIEWS
import net.lshift.diffa.schema.tables.EndpointCategories.ENDPOINT_CATEGORIES
import net.lshift.diffa.schema.tables.Endpoint.ENDPOINT
import net.lshift.diffa.schema.tables.ConfigOptions.CONFIG_OPTIONS
import net.lshift.diffa.schema.tables.Members.MEMBERS
import net.lshift.diffa.schema.tables.StoreCheckpoints.STORE_CHECKPOINTS
import net.lshift.diffa.schema.tables.PendingDiffs.PENDING_DIFFS
import net.lshift.diffa.schema.tables.Diffs.DIFFS
import net.lshift.diffa.schema.tables.Domains.DOMAINS
import net.lshift.diffa.kernel.lifecycle.DomainLifecycleAware
import collection.mutable.ListBuffer

class HibernateSystemConfigStore(val sessionFactory:SessionFactory,
                                 db:DatabaseFacade,
                                 jooq:JooqDatabaseFacade)
    extends SystemConfigStore with HibernateQueryUtils {

  val logger = LoggerFactory.getLogger(getClass)

  private val domainEventSubscribers = new ListBuffer[DomainLifecycleAware]

  def registerDomainEventListener(d:DomainLifecycleAware) = domainEventSubscribers += d

  def createOrUpdateDomain(d: Domain) = sessionFactory.withSession( s => {
    domainEventSubscribers.foreach(_.onDomainUpdated(d.name))
    s.saveOrUpdate(d)
  })

  def deleteDomain(domain:String) = {

    jooq.execute(t => {
      t.delete(USER_ITEM_VISIBILITY).where(USER_ITEM_VISIBILITY.DOMAIN.equal(domain)).execute()
      t.delete(ENDPOINT_VIEWS_CATEGORIES).where(ENDPOINT_VIEWS_CATEGORIES.DOMAIN.equal(domain)).execute()
      t.delete(ENDPOINT_VIEWS).where(ENDPOINT_VIEWS.DOMAIN.equal(domain)).execute()
      t.delete(PAIR_REPORTS).where(PAIR_REPORTS.DOMAIN.equal(domain)).execute()
      t.delete(ESCALATIONS).where(ESCALATIONS.DOMAIN.equal(domain)).execute()
      t.delete(REPAIR_ACTIONS).where(REPAIR_ACTIONS.DOMAIN.equal(domain)).execute()
      t.delete(PAIR_VIEWS).where(PAIR_VIEWS.DOMAIN.equal(domain)).execute()
      t.delete(PAIR).where(PAIR.DOMAIN.equal(domain)).execute()
      t.delete(ENDPOINT_CATEGORIES).where(ENDPOINT_CATEGORIES.DOMAIN.equal(domain)).execute()
      t.delete(ENDPOINT).where(ENDPOINT.DOMAIN.equal(domain)).execute()
      t.delete(CONFIG_OPTIONS).where(CONFIG_OPTIONS.DOMAIN.equal(domain)).execute()
      t.delete(MEMBERS).where(MEMBERS.DOMAIN_NAME.equal(domain)).execute()
      t.delete(STORE_CHECKPOINTS).where(STORE_CHECKPOINTS.DOMAIN.equal(domain)).execute()
      t.delete(PENDING_DIFFS).where(PENDING_DIFFS.DOMAIN.equal(domain)).execute()
      t.delete(DIFFS).where(DIFFS.DOMAIN.equal(domain)).execute()
      t.delete(DOMAINS).where(DOMAINS.NAME.equal(domain)).execute()
    })

    domainEventSubscribers.foreach(_.onDomainRemoved(domain))

    forceHibernateCacheEviction()
  }

  def doesDomainExist(name: String) = null != sessionFactory.withSession(s => s.get(classOf[Domain], name))

  def listDomains = db.listQuery[Domain]("allDomains", Map()).sortBy(_.getName)

  def listPairs = jooq.execute { t =>
    t.select().from(PAIR).fetch().map(ResultMappingUtil.recordToDomainPairDef)
  }

  def listEndpoints = db.listQuery[Endpoint]("allEndpoints", Map())


  // TODO implement create or update using JOOQ
  def createOrUpdateUser(user: User) = {
    if (updateUser(user) == 0) {
      createUser(user)
    }
  }

  def createUser(user: User) = db.execute("insertUser", Map(
    "name" -> user.name,
    "password_enc" -> user.passwordEnc,
    "email" -> user.email,
    "superuser" -> user.superuser
  ))

  def updateUser(user: User) = db.execute("updateUser", Map(
    "name" -> user.name,
    "password_enc" -> user.passwordEnc,
    "email" -> user.email,
    "superuser" -> user.superuser
  ))

  def getUserToken(username: String) = {
    sessionFactory.withSession(s => {
      val user = getUser(s, username)
      if (user.token == null) {
        // Generate token on demand
        user.token = RandomStringUtils.randomAlphanumeric(40)
      }
      user.token
    })
  }
  def clearUserToken(username: String) {
    sessionFactory.withSession(s => {
      val user = getUser(s, username)
      user.token = null

      s.saveOrUpdate(user)
    })
  }

  def deleteUser(name: String) = sessionFactory.withSession(s => {
    val user = getUser(s, name)
    s.delete(user)
  })

  def getUser(name: String) : User = sessionFactory.withSession(getUser(_,name))

  def getUserByToken(token: String) : User
    = db.singleQuery[User]("userByToken", Map("token" -> token), "user token %s".format(token))

  def listUsers : Seq[User] = db.listQuery[User]("allUsers", Map())
  def listDomainMemberships(username: String) : Seq[Member] =
    db.listQuery[Member]("membersByUser", Map("user_name" -> username))

  def containsRootUser(usernames: Seq[String]) : Boolean =
    sessionFactory.withSession(s => {
      val query: Query = s.getNamedQuery("rootUserCount")
      query.setParameterList("user_names", seqAsJavaList(usernames))

      query.uniqueResult().asInstanceOf[java.lang.Long] > 0
    })

  // TODO Add a unit test for this
  def maybeSystemConfigOption(key: String) = {
    sessionFactory.withSession(s => {
      s.get(classOf[SystemConfigOption], key) match {
        case null                       => None
        case current:SystemConfigOption => Some(current.value)
      }
    })
  }
  def setSystemConfigOption(key:String, value:String) {
    sessionFactory.withSession(s => {
      val co = s.get(classOf[SystemConfigOption], key) match {
        case null =>
          new SystemConfigOption(key = key, value = value)
        case current:SystemConfigOption =>  {
          current.value = value
          current
        }
      }
      s.saveOrUpdate(co)
    })
  }
  def clearSystemConfigOption(key:String) = sessionFactory.withSession(s => {
    s.get(classOf[SystemConfigOption], key) match {
      case null =>
      case current:SystemConfigOption =>  s.delete(current)
    }
  })

  def systemConfigOptionOrDefault(key:String, defaultVal:String) = {
    maybeSystemConfigOption(key) match {
      case Some(s)   => s
      case None      => defaultVal
    }
  }

  private def deleteByDomain[T](s:Session, domain:String, queryName:String) =
    db.listQuery[T](queryName, Map("domain_name" -> domain)).foreach(s.delete(_))
}

/**
 * Indicates that the system not configured correctly
 */
class InvalidSystemConfigurationException(msg:String) extends RuntimeException(msg)
