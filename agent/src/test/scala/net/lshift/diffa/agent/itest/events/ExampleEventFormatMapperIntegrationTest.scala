package net.lshift.diffa.agent.itest.events

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

import org.junit.Assert._
import org.junit.Assume.assumeTrue
import net.lshift.diffa.messaging.amqp.{ConnectorHolder, AmqpProducer, AmqpConnectionChecker}
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.junit.{Before, Test}
import org.apache.http.entity.FileEntity
import org.apache.http.client.methods.HttpPost
import net.lshift.diffa.kernel.differencing.MatchState.UNMATCHED
import net.lshift.diffa.kernel.events.VersionID
import org.joda.time.format.ISODateTimeFormat
import scala.collection.JavaConversions._
import net.lshift.diffa.kernel.differencing.{DifferenceEvent}
import org.springframework.core.io.ClassPathResource
import collection.mutable.HashMap
import org.joda.time.DateTime
import net.lshift.diffa.kernel.frontend.DomainDef
import net.lshift.diffa.agent.client.{SystemConfigRestClient, DifferencesRestClient}
import net.lshift.diffa.kernel.config.DiffaPairRef
import org.apache.http.auth.{UsernamePasswordCredentials, AuthScope}
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.HttpHost
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.client.protocol.ClientContext
import org.apache.http.impl.client.{BasicAuthCache, DefaultHttpClient}

/**
 * Integration test for change events over AMQP in an example JSON format.
 */
class ExampleEventFormatMapperIntegrationTest {

  assumeTrue(AmqpConnectionChecker.isConnectionAvailable)

  private val log = LoggerFactory.getLogger(getClass)
  val targetHost = new HttpHost("localhost", 19093)
  val httpClient = new DefaultHttpClient()
  httpClient.getCredentialsProvider().setCredentials(
    new AuthScope(targetHost.getHostName, targetHost.getPort),
    new UsernamePasswordCredentials("guest", "guest"))

  val authCache = new BasicAuthCache()
  authCache.put(targetHost, new BasicScheme)
  val localContext = new BasicHttpContext()
  localContext.setAttribute(ClientContext.AUTH_CACHE, authCache);

  val serverRoot = "http://localhost:19093/diffa-agent"
  val systemConfig = new SystemConfigRestClient(serverRoot)

  val domain = DomainDef(name="domain")

  @Before
  def setup {
    systemConfig.declareDomain(domain)

    val resource = new ClassPathResource("diffa-config.xml")
    val entity = new FileEntity(resource.getFile, "application/xml")
    val post = new HttpPost(serverRoot + "/rest/" + domain.name + "/config/xml")
    post.setEntity(entity)

    val response = httpClient.execute(post, localContext)
    assertEquals(204, response.getStatusLine.getStatusCode)
  }

  @Test
  def integrationTest() {
    val connectorHolder = new ConnectorHolder()
    val queueName = "exampleChanges"
    val changeEventProducer = new AmqpProducer(connectorHolder.connector, queueName)

    val diffClient = new DifferencesRestClient(serverRoot, domain.name)

    log.info("Sending change event")
    val changeEvent = IOUtils.toString(getClass.getResourceAsStream("/event.json"))
    changeEventProducer.send(changeEvent)

    // This is the observation date in the underlying message
    val recordDate = new DateTime(2011,01,24,0,0,0,0)

    val differenceEvents = poll(diffClient, "pair", recordDate.minusDays(1), recordDate.plusDays(1), 0, 100)
    assertEquals(1, differenceEvents.length)
    val differenceEvent = differenceEvents(0)
    assertEquals(VersionID(DiffaPairRef("pair", domain.name), "5509a836-ca75-42a4-855a-71893448cc9d"), differenceEvent.objId)
    assertEquals("2011-01-24T00:00:00.000Z", ISODateTimeFormat.dateTime.print(differenceEvent.detectedAt))
    assertEquals(UNMATCHED, differenceEvent.state)
    assertEquals("479", differenceEvent.upstreamVsn)
    assertNull(differenceEvent.downstreamVsn)
  }

  def poll(diffClient: DifferencesRestClient,
           pairKey:String,
           from:DateTime, until:DateTime, offset:Int, length:Int,
           maxAttempts: Int = 10,
           sleepTimeMillis: Int = 1000): Array[DifferenceEvent] = {
    
    var attempts = 0
    while (attempts < maxAttempts) {
      Thread.sleep(1000)
      val differenceEvents = diffClient.getEvents(pairKey, from, until, offset, length)
      assertNotNull(differenceEvents)
      if (differenceEvents.length > 0) return differenceEvents
      attempts += 1
    }
    fail("Couldn't retrieve difference events after %d attempts".format(maxAttempts))
    null
  }
}