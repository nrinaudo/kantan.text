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

import java.io._
import java.nio.charset.Charset
import org.scalatest.FunSuite

class SloppyCharsetTests extends FunSuite {
  def loadString(res: String, charset: Charset): String = {
    def go(in: Reader, buf: Array[Char], out: StringBuilder): String = in.read(buf) match {
      case -1 ⇒
        in.close()
        out.result()
      case i ⇒ go(in, buf, out.append(new String(buf, 0, i)))
    }

    go(new InputStreamReader(io.resource(res), charset), new Array[Char](256), new StringBuilder)
  }

  def loadBytes(res: String): Array[Byte] = {
    val bytes = new Array[Byte](256)
    val in    = getClass.getResourceAsStream(s"/kantan/text/sanitize/$res")
    in.read(bytes)
    in.close()
    bytes
  }

  val raw       = "encoded-latin-1.txt"
  val rawString = loadString(raw, Charset.forName("iso-8859-1"))
  val rawBytes  = loadBytes(raw)

  SloppyCharset.knownValues.foreach {
    case (name, charset) ⇒
      test(s"$name should encode characters as expected") {
        assert(loadBytes(s"encoded-$name.txt").deep == rawString.getBytes(charset).deep)
      }

      test(s"$name should decode bytes as expected") {
        assert(loadString(s"decoded-$name.txt", Charset.forName("utf-8")) == new String(rawBytes, charset))
      }
  }
}
