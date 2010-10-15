/**
 * Copyright (C) 2010 LShift Ltd.
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

import org.junit.Test
import org.junit.Assert._
import net.lshift.diffa.kernel.events.VersionID
import org.joda.time.DateTime
import collection.immutable.HashSet
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import net.lshift.diffa.kernel.util.Dates._
import net.lshift.diffa.kernel.participants._

/**
 * Test cases for the digest builder.
 */
class DigestBuilderTest {
  val pair = "A-B"
  
  @Test
  def shouldNotBucketIndividualVersions {
    val builder = new DigestBuilder(IndividualGranularity)

    builder.add(VersionID(pair, "id1"), JUL_9_2010_1, JUL_9_2010_1, "vsn1")
    builder.add(VersionID(pair, "id2"), JUL_9_2010_1, JUL_9_2010_1, "vsn2")

    assertEquals(
      HashSet(VersionDigest("id1", JUL_9_2010_1, JUL_9_2010_1, "vsn1"), VersionDigest("id2", JUL_9_2010_1, JUL_9_2010_1, "vsn2")),
      HashSet(builder.digests: _*))
  }

  @Test
  def shouldBucketByDay {
    val builder = new DigestBuilder(DayGranularity)

    builder.add(VersionID(pair, "id1"), JUL_8_2010_1, JUL_8_2010_1, "vsn1")
    builder.add(VersionID(pair, "id2"), JUL_8_2010_2, JUL_8_2010_2, "vsn2")
    builder.add(VersionID(pair, "id3"), JUL_9_2010_1, JUL_9_2010_1, "vsn3")

    assertEquals(
      HashSet(
        VersionDigest("2010-07-08", JUL_8_2010, null, DigestUtils.md5Hex("vsn1" + "vsn2")),
        VersionDigest("2010-07-09", JUL_9_2010, null, DigestUtils.md5Hex("vsn3"))),
      HashSet(builder.digests: _*))
  }

  @Test
  def shouldBucketByMonth {
    val builder = new DigestBuilder(MonthGranularity)

    builder.add(VersionID(pair, "id1"), JUL_8_2010_1, JUL_8_2010_1, "vsn1")
    builder.add(VersionID(pair, "id2"), JUL_8_2010_2, JUL_8_2010_2, "vsn2")
    builder.add(VersionID(pair, "id3"), JUL_9_2010_1, JUL_9_2010_1, "vsn3")
    builder.add(VersionID(pair, "id4"), AUG_2_2010_1, AUG_2_2010_1, "vsn4")

    assertEquals(
      HashSet(
        VersionDigest("2010-07", JUL_2010, null, DigestUtils.md5Hex("vsn1" + "vsn2" + "vsn3")),
        VersionDigest("2010-08", AUG_2010, null, DigestUtils.md5Hex("vsn4"))),
      HashSet(builder.digests: _*))
  }

  @Test
  def shouldBucketByYear {
    val builder = new DigestBuilder(YearGranularity)

    builder.add(VersionID(pair, "id0"), JUN_6_2009_1, JUN_6_2009_1, "vsn0")
    builder.add(VersionID(pair, "id1"), JUL_8_2010_1, JUL_8_2010_1, "vsn1")
    builder.add(VersionID(pair, "id2"), JUL_8_2010_2, JUL_8_2010_2, "vsn2")
    builder.add(VersionID(pair, "id3"), JUL_9_2010_1, JUL_9_2010_1, "vsn3")
    builder.add(VersionID(pair, "id4"), AUG_2_2010_1, AUG_2_2010_1, "vsn4")
    builder.add(VersionID(pair, "id5"), JAN_2_2011_1, JAN_2_2011_1, "vsn5")
    builder.add(VersionID(pair, "id6"), AUG_11_2011_1, AUG_11_2011_1, "vsn6")

    assertEquals(
      HashSet(
        VersionDigest("2009", START_2009, null, DigestUtils.md5Hex("vsn0")),
        VersionDigest("2010", START_2010, null, DigestUtils.md5Hex("vsn1" + "vsn2" + "vsn3" + "vsn4")),
        VersionDigest("2011", START_2011, null, DigestUtils.md5Hex("vsn5" + "vsn6"))),
      HashSet(builder.digests: _*))
  }
}