/*
 * Copyright 2017 Nicolas Rinaudo
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
import kantan.text._

object tools {
  val Utf8: Charset = Charset.forName("UTF-8")
  val Cp1252: Charset = Charset.forName("windows-1252")
  val SloppyCp1252: Charset = SloppyCharset.knownValues("sloppy-windows-1252")
  val Latin1: Charset = Charset.forName("iso-8859-1")

  val supportedEncodings: List[(String, Charset)] = List(
    "iso-8859-1"          → Charset.forName("iso-8859-1"),
    "sloppy-windows-1252" → SloppyCharset.knownValues("sloppy-windows-1252"),
    "macroman"            → Charset.forName("MacRoman"),
    "cp437"               → Charset.forName("cp437"),
    "sloppy-windows-1251" → SloppyCharset.knownValues("sloppy-windows-1251")
  )

  val encodingPatterns: Map[String, Pattern] = {
    val builder     = Map.newBuilder[String, Pattern]
    val latin1Table = ((128 until 256).map(_.toChar).mkString + '\u001a').getBytes(Latin1)

    builder += "ascii" → Pattern.compile("^[\u0000-\u007f]*$")

    supportedEncodings.foreach { case (name, encoding) ⇒
      builder += name → Pattern.compile(s"^[\u0000-\u0019\u001b-\u007f${new String(latin1Table, encoding)}]*$$")
    }

    builder.result()
  }

  def possibleEncoding(text: String, encoding: String): Boolean =
    encodingPatterns.get(encoding).exists(_.matcher(text).matches())

  val partialUtf8PunctuationPattern: Pattern = {
    Pattern.compile(s"â€[${new String((128 until 192).map(_.toChar).mkString.getBytes(Latin1), SloppyCp1252)}]")
  }

  val fixPartialUtf8Punctuation: String ⇒ String = replaceAll(partialUtf8PunctuationPattern) { str ⇒
    new String(str.getBytes(SloppyCp1252), Utf8)
  }

  def fixOneStep(text: String): String = {
    def findEncodingFromPattern(encodings: List[(String, Charset)], acc: Set[String]): Either[Set[String], String] =
      encodings match {
        case h :: t ⇒
          if(possibleEncoding(text, h._1)) {
            try {
              val bytes = text.getBytes(h._2)

              // TODO: check for ALTERED_UTF8
              // TODO: check for 'sloppy-'
              // TODO: check for utf-8 variant

              Right(new String(bytes, Utf8))
            }
            catch {
              case _: Exception ⇒ findEncodingFromPattern(t, acc + h._1)
            }

          }
          else findEncodingFromPattern(t, acc)
        case Nil ⇒
          Left(acc)
      }

    // ASCII text, no need to fix.
    if(possibleEncoding(text, "ascii")) text

    // Suppose the text was supposed to be UTF-8, but it was decoded using
    // a single-byte encoding instead. When these cases can be fixed, they
    // are usually the correct thing to do, so try them next.
    else findEncodingFromPattern(supportedEncodings, Set.empty) match {
      case Left(possible1Byte) ⇒

        // Look for a-hat-euro sequences that remain, and fix them in isolation.
        val fixed = fixPartialUtf8Punctuation(text)
        if(fixed != text) fixed

        // TODO: check if latin-1 meant as windows-1252
        else text

      case Right(fixed) ⇒ fixed
    }
  }

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
