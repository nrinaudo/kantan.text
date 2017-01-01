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

import java.io.DataInputStream
import java.nio.charset.Charset
import java.util.regex.Pattern

object tools {
  val supportedEncodings: Map[String, Charset] = Map(
    "iso-8859-1"          → Charset.forName("iso-8859-1"),
    "sloppy-windows-1252" → SloppyCharset.knownValues("sloppy-windows-1252"),
    "macroman"            → Charset.forName("MacRoman"),
    "cp437"               → Charset.forName("cp437"),
    "sloppy-windows-1251" → SloppyCharset.knownValues("sloppy-windows-1251")
  )

  val encodingPatterns: Map[String, Pattern] = {
    val builder     = Map.newBuilder[String, Pattern]
    val latin1Table = ((128 until 256).map(_.toChar).mkString + '\u001a').getBytes("iso-8859-1")

    builder += "ascii" → Pattern.compile("^[\u0000-\u007f]*$")

    supportedEncodings.foreach { case (name, encoding) ⇒
      builder += name → Pattern.compile(s"^[\u0000-\u0019\u001b-\u007f${new String(latin1Table, encoding)}]*$$")
    }

    builder.result()
  }

  def possibleEncoding(text: String, encoding: String): Boolean =
    encodingPatterns.get(encoding).exists(_.matcher(text).matches())

  // Unsafe, but acceptable here: we don't even want to start if the entities are not available.
  val htmlEntities: Map[String, String] = {
    val in = new DataInputStream(this.getClass.getResourceAsStream("/kantan/text/sanitize/htmlentities.dat"))
    try {
      val builder = Map.newBuilder[String, String]
      (0 until in.readInt()).foreach { _ ⇒ builder += (in.readUTF() → in.readUTF()) }
      builder.result()
    }
    finally { in.close() }
  }
}
