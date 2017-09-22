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

package kantan.text

import java.text.Normalizer
import java.util.Locale
import java.util.regex.{Matcher, Pattern}

trait CoreFunctions {
  // - Unicode ---------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  def normalize(form: Normalizer.Form): String ⇒ String = Normalizer.normalize(_, form)

  // - Case ------------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  def lowerCase(locale: Locale): String ⇒ String = _.toLowerCase(locale)
  def upperCase(locale: Locale): String ⇒ String = _.toUpperCase(locale)

  // - Regex -----------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  def replaceAll(pattern: Pattern, by: String): String ⇒ String = str ⇒ pattern.matcher(str).replaceAll(by)
  def replaceAll(str: String, by: String): String ⇒ String      = replaceAll(Pattern.compile(str), by)
  def removeAll(pattern: Pattern): String ⇒ String              = replaceAll(pattern, "")
  def removeAll(str: String): String ⇒ String                   = removeAll(Pattern.compile(str))

  def ifMatch(pattern: Pattern)(f: String ⇒ String): String ⇒ String = str ⇒ {
    if(pattern.matcher(str).find()) f(str)
    else str
  }

  def replaceAll(pattern: Pattern)(f: String ⇒ String): String ⇒ String = {
    def go(matcher: Matcher, found: Boolean, acc: StringBuffer): String =
      if(found) {
        matcher.appendReplacement(acc, f(matcher.group(0)))
        go(matcher, matcher.find(), acc)
      }
      else {
        matcher.appendTail(acc)
        acc.toString
      }

    str ⇒
      {
        val matcher = pattern.matcher(str)

        if(matcher.find()) go(matcher, true, new StringBuffer)
        else str
      }
  }

  def replaceAll(str: String)(f: String ⇒ String): String ⇒ String = replaceAll(Pattern.compile(str))(f)

  // - Misc. -----------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  // TODO: this can be thoroughly optimised, if only by not doing anything if no matching char is found.
  def translate(map: Map[Char, String]): String ⇒ String = str ⇒ {
    val out = new StringBuilder
    str.foreach { c ⇒
      out.append(map.getOrElse(c, c.toString))
    }
    out.result()
  }

  def stripLeft(c: Char): String ⇒ String = str ⇒ {
    def go(curr: Int, len: Int): String =
      if(curr >= len) ""
      else if(str.charAt(curr) != c) str.substring(curr)
      else go(curr + 1, len)

    go(0, str.length)
  }
}
