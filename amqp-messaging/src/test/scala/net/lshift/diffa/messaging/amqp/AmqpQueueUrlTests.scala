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

package net.lshift.diffa.messaging.amqp

import org.junit.Assert._
import org.junit.Test

/**
 * Test cases for the AmqpQueueUrl class.
 */
class AmqpQueueUrlTests {

  @Test
  def parseUrlWithNoPortAndEmptyVHost() {
    assertEquals(AmqpQueueUrl("test"),
               AmqpQueueUrl.parse("amqp://localhost//queues/test"))
  }

  @Test
  def parseUrlWithPortAndEmptyVHost() {
    assertEquals(AmqpQueueUrl("test", port = 9001),
               AmqpQueueUrl.parse("amqp://localhost:9001//queues/test"))
  }

  @Test
  def parseUrlWithHostNoPortAndEmptyVHost() {
    assertEquals(AmqpQueueUrl("test", host = "rabbitserver"),
               AmqpQueueUrl.parse("amqp://rabbitserver//queues/test"))
  }

  @Test
  def parseUrlWithHostPortAndEmptyVHost() {
    assertEquals(AmqpQueueUrl("test", host = "rabbitserver", port = 9001),
                 AmqpQueueUrl.parse("amqp://rabbitserver:9001//queues/test"))
  }

  @Test
  def parseUrlWithNoPortAndNonEmptyVHost() {
    assertEquals(AmqpQueueUrl("test", vHost = "vhost"),
               AmqpQueueUrl.parse("amqp://localhost/vhost/queues/test"))
  }

  @Test
  def parseUrlWithPortAndNonEmptyVHost() {
    assertEquals(AmqpQueueUrl("test", port = 9001, vHost = "vhost"),
               AmqpQueueUrl.parse("amqp://localhost:9001/vhost/queues/test"))
  }

  @Test
  def parseUrlWithHostNoPortAndNonEmptyVHost() {
    assertEquals(AmqpQueueUrl("test", host = "rabbitserver", vHost = "vhost"),
               AmqpQueueUrl.parse("amqp://rabbitserver/vhost/queues/test"))
  }

  @Test
  def parseUrlWithHostPortAndNonEmptyVHost() {
    assertEquals(AmqpQueueUrl("test", host = "rabbitserver", port = 9001, vHost = "vhost"),
                 AmqpQueueUrl.parse("amqp://rabbitserver:9001/vhost/queues/test"))
  }

  @Test
  def buildStringWithNoPortAndEmptyVHost() {
    assertEquals("amqp://localhost//queues/test",
               AmqpQueueUrl("test").toString)
  }

  @Test
  def buildStringWithPortAndEmptyVHost() {
    assertEquals("amqp://localhost:9001//queues/test",
               AmqpQueueUrl("test", port = 9001).toString)
  }

  @Test
  def buildStringWithHostNoPortAndEmptyVHost() {
    assertEquals("amqp://rabbitserver//queues/test",
               AmqpQueueUrl("test", host = "rabbitserver").toString)
  }

  @Test
  def buildStringWithHostPortAndEmptyVHost() {
    assertEquals("amqp://rabbitserver:9001//queues/test",
               AmqpQueueUrl("test", host = "rabbitserver", port = 9001).toString)
  }

}