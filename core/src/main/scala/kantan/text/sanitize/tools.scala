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
import java.util.regex.Pattern

private[sanitize] object tools {
  private[sanitize] val supportedEncodings: List[String] = List(
      "iso-8859-1",
      "sloppy-windows-1252",
      "macroman",
      "cp437",
      "sloppy-windows-1251"
    )

    private[sanitize] val encodingPatterns: Map[String, Pattern] = {
      val builder = Map.newBuilder[String, Pattern]

      val latin1Table = (128 until 256).map(_.toChar).mkString + '\u001a'

      builder += "ascii" → Pattern.compile("""^[\x00-\x7f]*$""")

      builder.result()
    }

    // Unsafe, but acceptable here: we don't even want to start if the entities are not available.
    private[sanitize] val htmlEntities: Map[String, String] = {
      val in = new DataInputStream(this.getClass.getResourceAsStream("/kantan/text/sanitize/htmlentities.dat"))
      try {
        val builder = Map.newBuilder[String, String]
        (0 until in.readInt()).foreach { _ ⇒ builder += (in.readUTF() → in.readUTF()) }
        builder.result()
      }
      finally { in.close() }
    }
}
