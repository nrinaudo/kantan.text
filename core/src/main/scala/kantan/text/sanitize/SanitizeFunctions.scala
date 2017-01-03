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

import java.text.Normalizer
import java.util.regex.Pattern
import kantan.text._

/** Collection of functions adapted from [[https://ftfy.readthedocs.io ftfy]]. */
trait SanitizeFunctions {
  /** Decode all three types of HTML entities/character references.
    *
    * Code by Fredrik Lundh of effbot.org. Rob Speer made a slight change
    * to it for efficiency: it won't match entities longer than 8 characters,
    * because there are no valid entities like that.
    *
    * {{{
    * scala> fixEntities("&lt;tag&gt;")
    * res1: String = <tag>
    * }}}
    */
  // TODO: this can probably be optimised by grabbing group(1) and excluding & and ; from the match.
  val fixEntities: String ⇒ String = replaceAll("""&#?\w{0,8};""") { str ⇒
    val entity = str.substring(1, str.length - 1)
    if(entity.startsWith("#")) {
      val code = entity.substring(1)

      try {
        if(code.startsWith("x")) Integer.parseInt(code.substring(1), 16).toChar.toString
        else                     code.toInt.toChar.toString
      }
      catch {
        case _: Exception ⇒ str
      }
    }
    else tools.htmlEntities.getOrElse(entity, str)
  }

  /** Strip out "ANSI" terminal escape sequences, such as those that produce colored text on Unix.
    *
    * {{{
    * scala> removeTerminalEscapes("\u001B[36;44mI'm blue, da ba dee da ba doo...\u001B[0m")
    * res1: String = I'm blue, da ba dee da ba doo...
    * }}}
    */
  val removeTerminalEscapes: String ⇒ String = removeAll("""(?smiuU)\u001B\[((?:\d|;)*)([a-zA-Z])""")

  val fixEncoding: String ⇒ String = {
    tools.encodingPatterns.head
    identity
  }

  /** Replace single-character ligatures of Latin letters, such as `ﬁ`, with the characters that they contain, as in
    * `fi`. Latin ligatures are usually not intended in text strings (though they're lovely in *rendered* text). If you
    * have such a ligature in your string, it is probably a result of a copy-and-paste glitch.
    *
    * We leave ligatures in other scripts alone to be safe. They may be intended, and removing them may lose
    * information. If you want to take apart nearly all ligatures, use NFKC normalization.
    *
    * {{{
    * scala> fixLatinLigatures("ﬂuﬃeﬆ")
    * res1: String = fluffiest
    * }}}
    */
  val fixLatinLigatures: String ⇒ String = translate(Map(
    'Ĳ' → "IJ",
    'ĳ' → "ij",
    'ﬀ' → "ff",
    'ﬁ' → "fi",
    'ﬂ' → "fl",
    'ﬃ' → "ffi",
    'ﬄ' → "ffl",
    'ﬅ' → "ſt",
    'ﬆ' → "st"
  ))

  /** The ASCII characters, katakana, and Hangul characters have alternate "halfwidth" or "fullwidth" forms that help
    * text line up in a grid.
    *
    * If you don't need these width properties, you probably want to replace these characters with their standard form,
    * which is what this function does.
    *
    * Note that this replaces the ideographic space, `U+3000`, with the ASCII space, `U+20`.
    *
    * {{{
    * scala> fixCharacterWidth("ＬＯＵＤ　ＮＯＩＳＥＳ")
    * res1: String = LOUD NOISES
    *
    * scala> fixCharacterWidth("Ｕﾀｰﾝ")
    * res2: String = Uターン
    * }}}
    */
  val fixCharacterWidth: String ⇒ String = {
    def addChar(acc: Map[Char, String], c: Char): Map[Char, String] = {
      val orig = c.toString
      val norm = Normalizer.normalize(orig, Normalizer.Form.NFKC)

      if(orig != norm) acc + (c.toChar → norm)
      else acc
    }

    translate((0xff01 until 0xfff0).foldLeft(addChar(Map.empty[Char, String], 0x3000.toChar)) { (a, c) ⇒
      addChar(a, c.toChar)
    })
  }

  /** Replace curly quotation marks with straight equivalents.
    *
    * {{{
    * scala> uncurlQuotes("\u201chere\u2019s a test\u201d")
    * res1: String = "here's a test"
    * }}}
    */
  val uncurlQuotes: String ⇒ String =
    replaceAll("""[\u2018-\u201b]""", "'") andThen replaceAll("""[\u201c-\u201f]""", "\"")

  /** Convert all line breaks to Unix style.
    *
    * This will convert the following sequences into the standard `\n` line break:
    *  - CRLF (`\r\n`), used on Windows and in some communication protocols
    *  - CR (`\r`), once used on Mac OS Classic, and now kept alive by misguided software such as Microsoft Office for
    *    Mac
    *  - LINE SEPARATOR (`\u2028`) and PARAGRAPH SEPARATOR (`\u2029`), defined by Unicode and used to sow confusion and
    *    discord
    *  - NEXT LINE (`\x85`), a C1 control character that is certainly not what you meant
    *
    * The NEXT LINE character is a bit of an odd case, because it usually won't show up if `fixEncoding` is also being
    * run. `\x85` is very common mojibake for `\u2026`, HORIZONTAL ELLIPSIS.
    *
    * {{{
    * scala> fixLineBreaks("This string is made of two things:\u20291. Unicode\u20282. Spite")
    * res1: String =
    * This string is made of two things:
    * 1. Unicode
    * 2. Spite
    * }}}
    */
  val fixLineBreaks: String ⇒ String = replaceAll("""\r\n?|\u2028|\u2029|\u0085""", "\n")

  /** Replace 16-bit surrogate codepoints with the characters they represent (when properly paired), or with
    * `\ufffd` otherwise.
    * {{{
    * scala> val high = 0xd83d.toChar
    * scala> val low = 0xdca9.toChar
    *
    * scala> fixSurrogates(high.toString + low)
    * res1: String = \uD83D\uDCA9
    *
    * scala> fixSurrogates(low.toString + high)
    * res2: String = ��
    * }}}
    */
  val fixSurrogates: String ⇒ String = {
    val surrogate = Pattern.compile("(?smiuU)[\ud800-\udfff]")

    ifMatch(surrogate)(replaceAll("(?smiuU)[\ud800-\udbff][\udc00-\udfff]") { m ⇒
      (0x10000 + (m.charAt(0).toInt - 0xd800) * 0x400 + (m.charAt(1).toInt - 0xdc00)).toChar.toString
    } andThen replaceAll(surrogate, "\ufffd"))
  }

  /** Remove all ASCII control characters except for the important ones.
    *
    * This removes characters in these ranges:
    *  - `U+0000` to `U+0008`
    *  - `U+000B`
    *  - `U+000E` to `U+001F`
    *  - `U+007F`
    *
    * It leaves alone these characters that are commonly used for formatting:
    *  - `TAB` (`U+0009`)
    *  - `LF` (`U+000A`)
    *  - `FF` (`U+000C`)
    *  - `CR` (`U+000D`)
    */
  val removeControlChars: String ⇒ String = translate(
    (0 until 32).filter(i ⇒ i != 9 && i != 10 && i != 12 && i != 13)
      .foldLeft(Map.empty[Char, String])((acc, c) ⇒ acc + (c.toChar → ""))
  )

  /** Remove a byte-order mark that was accidentally decoded as if it were part of the text.
    * {{{
    * scala> removeBom("\ufeffWhere do you want to go today?")
    * res1: String = Where do you want to go today?
    * }}}
    */
  val removeBom: String ⇒ String = stripLeft(0xfeff)
}
