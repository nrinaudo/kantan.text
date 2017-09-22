/*
 * Copyright 2016 Nicolas Rinaudo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kantan.text.sanitize

import java.nio.charset.{Charset, CharsetDecoder, CharsetEncoder}
import scala.collection.JavaConverters._
import sun.nio.cs.SingleByte
import sun.nio.cs.SingleByte.{Decoder, Encoder}

@SuppressWarnings(Array("org.wartremover.warts.Null"))
class SloppyCharset(charset: Charset)
    extends Charset(s"sloppy-${charset.name()}", charset.aliases().asScala.map(s ⇒ s"sloppy-$s").toArray) {

  private val b2c = {
    // All bytes as they'd be encoded in ISO-LATIN-1.
    val sloppyChars = new String((-128 until 128).map(_.toByte).toArray, "iso-8859-1").toCharArray

    // All bytes encoded using the specified charset. Any character that is undefined (\uFFFD) is replaced by its
    // ISO-LATIN-1 value.
    new String((-128 until 128).map(_.toByte).toArray, charset).zipWithIndex.filter(_._1 != '\uFFFD').foreach {
      case (c, i) ⇒ sloppyChars(i) = c
    }

    // For ftfy's own purposes, we're going to allow byte 1A, the "Substitute"
    // control code, to encode the Unicode replacement character U+FFFD.
    sloppyChars(26 + 128) = '\uFFFD'

    sloppyChars
  }

  private val c2b      = new Array[Char](1536)
  private val c2bIndex = new Array[Char](256)
  SingleByte.initC2B(b2c, null: Array[Char], c2b, c2bIndex)

  override def newDecoder(): CharsetDecoder = new Decoder(this, b2c)

  override def newEncoder(): CharsetEncoder = new Encoder(this, c2b, c2bIndex)

  override def contains(cs: Charset): Boolean = false
}

object SloppyCharset {
  val knownValues: Map[String, Charset] =
    ((1250 until 1259).map(i ⇒ s"windows-$i") ++
      List(3, 6, 7, 8, 11).map(i ⇒ s"iso-8859-$i")).foldLeft(Map.empty[String, Charset]) { (acc, name) ⇒
      acc + (s"sloppy-$name" → new SloppyCharset(Charset.forName(name)))
    }
}
