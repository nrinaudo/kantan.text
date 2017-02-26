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
  // - Commonly used encodings -----------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  val Utf8: Charset = Charset.forName("UTF-8")
  val Cp1252: Charset = Charset.forName("windows-1252")
  val SloppyCp1252: Charset = SloppyCharset.knownValues("sloppy-windows-1252")
  val Latin1: Charset = Charset.forName("iso-8859-1")

  /** List of encodings supported by `kantan.text.sanitize`.
    *
    * These are by far the most commonly mangled encodings. We could support more, but the rate of false positives
    * would rise to unacceptable levels.
    */
  val supportedEncodings: List[(String, Charset)] = List(
    "iso-8859-1"          → Charset.forName("iso-8859-1"),
    "sloppy-windows-1252" → SloppyCharset.knownValues("sloppy-windows-1252"),
    "macroman"            → Charset.forName("MacRoman"),
    "cp437"               → Charset.forName("cp437"),
    "sloppy-windows-1251" → SloppyCharset.knownValues("sloppy-windows-1251")
  )

  /** Patterns used to detect mangled encodings. */
  val encodingPatterns: Map[String, Pattern] = {
    val builder     = Map.newBuilder[String, Pattern]
    val latin1Table = ((128 until 256).map(_.toChar).mkString + '\u001a'.toString).getBytes(Latin1)

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
            // Now, find out if it's UTF-8 (or close enough). Otherwise,
            // remember the encoding for later.
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

        // The next most likely case is that this is Latin-1 that was intended to
        // be read as Windows-1252, because those two encodings in particular are
        // easily confused.

        else if(possible1Byte.contains("iso-8859-1")) {
          // This text is in the intersection of Latin-1 and
          // Windows-1252, so it's probably legit.
          if(possible1Byte.contains("windows-1252")) text
          else {

            // Otherwise, it means we have characters that are in Latin-1 but
            // not in Windows-1252. Those are C1 control characters. Nobody
            // wants those. Assume they were meant to be Windows-1252. Don't
            // use the sloppy codec, because bad Windows-1252 characters are
            // a bad sign.
            val fixed = try { new String(text.getBytes(Latin1), Cp1252) }
            catch { case _: Exception ⇒ text }

            if(fixed != text) fixed
            else              text
          }
        }

        // The cases that remain are mixups between two different single-byte
        // encodings, and not the common case of Latin-1 vs. Windows-1252.
        //
        // These cases may be unsolvable without adding false positives, though
        // I have vague ideas about how to optionally address them in the future.
        //
        // Return the text unchanged; the plan is empty.

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
