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

package kantan.text.preprocess

import java.text.Normalizer
import java.util.regex.Pattern
import kantan.text.{normalize, removeAll, replaceAll}

/** Collection of functions adapted from
  * [[http://textacy.readthedocs.io/en/latest/api_reference.html#module-textacy.preprocess textacy.preprocess]]. */
trait PreprocessFunctions {
  val normalizeVerticalWhitespace: String ⇒ String = replaceAll("""(?smiuU)((\r\n)|[\n\v])++""", "\n")

  val normalizeHorizontalWhitespace: String ⇒ String = replaceAll("""(?smiuU)(?!\n)\s++""", " ")

  val trim: String ⇒ String = _.trim

  /** Given a string, replaces one or more spacings with a single space, and one or more linebreaks with a single
    * newline. Also strips leading/trailing whitespace.
    *
    * {{{
    * scala> normalizeWhitespace("Hello, world!  Hello...\t \tworld?\n\nHello:\r\n\n\nWorld. ")
    * res1: String =
    * Hello, world! Hello... world?
    * Hello:
    * World.
    * }}}
    */
  val normalizeWhitespace: String ⇒ String =
    normalizeVerticalWhitespace andThen normalizeHorizontalWhitespace andThen trim

  /** Replaces all URLs in a string with `by`.
    *
    * {{{
    * scala> replaceUrls("*URL*")("I learned everything I know from www.stackoverflow.com and http://wikipedia.org/")
    * res1: String = I learned everything I know from *URL* and *URL*
    * }}}
    */
  def replaceUrls(by: String): String ⇒ String = {
    val shortUrl = Pattern.compile(
      """(?smi)""" +
      """(?:^|(?<![\w/.]))""" +
      // optional scheme
      """(?:(?:https?://)?)""" +
      // domain
      """(?:\w-?)*?\w+(?:\.[a-z]{2,12}){1,3}""" +
      """/""" +
      // hash
      """[^\s.,?!'\"|+]{2,12}""" +
      """(?:$|(?![\w?!+&/]))""")

    val url = Pattern.compile(
      """(?smiuU)""" +
      """(?:^|(?<![\w/.]))""" +
      // protocol identifier
      """(?:(?:https?://|ftp://|www\d{0,3}\.))""" +
      // user:pass authentication
      """(?:\S+(?::\S*)?@)?""" +
      """(?:""" +
      // IP address exclusion
      // private & local networks
      """(?!(?:10|127)(?:\.\d{1,3}){3})""" +
      """(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})""" +
      """(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})""" +
      // IP address dotted notation octets
      // excludes loopback network 0.0.0.0
      // excludes reserved space >= 224.0.0.0
      // excludes network & broadcast addresses
      // (first & last IP address of each class)
      """(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])""" +
      """(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}""" +
      """(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))""" +
      """|""" +
      // host name
      """(?:(?:[a-z\u00a1-\uffff0-9]-?)*[a-z\u00a1-\uffff0-9]+)""" +
      // domain name
      """(?:\.(?:[a-z\u00a1-\uffff0-9]-?)*[a-z\u00a1-\uffff0-9]+)*""" +
      // TLD identifier
      """(?:\.(?:[a-z\u00a1-\uffff]{2,}))""" +
      """)""" +
      // port number
      """(?::\d{2,5})?""" +
      // resource path
      """(?:/\S*)?""" +
      """(?:$|(?![\w?!+&/]))""")

    str ⇒ url.matcher(shortUrl.matcher(str).replaceAll(by)).replaceAll(by)
  }

  /** Replaces all emails in a string with `by`.
    *
    * {{{
    * scala> replaceEmails("*EMAIL*")("I can be reached at username@example.com through next Friday.")
    * res1: String = I can be reached at *EMAIL* through next Friday.
    * }}}
    */
  def replaceEmails(by: String): String ⇒ String = replaceAll(
    """(?smiuU)(?:^|(?<=[^\w@.)]))""" +
    """([\w+-](\.(?!\.))?)*?[\w+-]@(?:\w-?)*?\w+(\.([a-z]{2,})){1,3}(?:$|(?=\b))""", by)

  /** Replaces all phone numbers in a string with `by`.
    *
    * {{{
    * scala> replacePhoneNumbers("*PHONE*")("I can be reached at 555-123-4567 through next Friday.")
    * res1: String = I can be reached at *PHONE* through next Friday.
    * }}}
    */
  def replacePhoneNumbers(by: String): String ⇒ String = replaceAll(
    """(?smiuU)(?:^|(?<=[^\w)]))""" +
    """(\+?1[ .-]?)?(\(?\d{3}\)?[ .-]?)?\d{3}[ .-]?\d{4}""" +
    """(\s?(?:ext\.?|[#x-])\s?\d{2,6})?(?:$|(?=\W))""",
    by)

  /** Replaces all numbers in a string with `by`.
    *
    * {{{
    * scala> replaceNumbers("*NUM*")("I owe ₡1,000.99 to 123 people for 2 +1 reasons.")
    * res1: String = I owe ₡*NUM* to *NUM* people for *NUM* *NUM* reasons.
    * }}}
    */
  def replaceNumbers(by: String): String ⇒ String = replaceAll(
    """(?smiuU)(?:^|(?<=[^\w,.]))""" +
    """[+–-]?(([1-9]\d{0,2}(,\d{3})+(\.\d*)?)|([1-9]\d{0,2}""" +
    """([ .]\d{3})+(,\d*)?)|(\d*?[.,]\d+)|\d+)(?:$|(?=\b))""",
    by
  )

  /** Removes all punctuation from a string (replace punct marks with empty string).
    *
    * {{{
    * scala> removePunct("I can't. No, I won't! It's a matter of \"principle\"; of -- what's the word? -- conscience.")
    * res1: String = I cant No I wont Its a matter of principle of  whats the word  conscience
    * }}}
    */
  val removePunct: String ⇒ String = removeAll("""(?smiuU)\p{Punct}++""")

  /** Replaces all currency symbols in a string with `by`.
    *
    * {{{
    * scala> replaceCurrencySymbols("*CUR* ")("¢1.00 equals £0.19 equals €0.22.")
    * res1: String = *CUR* 1.00 equals *CUR* 0.19 equals *CUR* 0.22.
    * }}}
    */
  // TODO: do we want to support normalisation ($ → USD, ...)?
  def replaceCurrencySymbols(by: String): String ⇒ String = replaceAll("""(?smiuU)\p{Sc}++""", by)

  /** Removes all diacritics from a string.
    *
    * {{{
    * scala> removeDiacritics("El niño se asustó -- qué miedo!")
    * res1: String = El nino se asusto -- que miedo!
    * }}}
    */
  val removeDiacritics: String ⇒ String = normalize(Normalizer.Form.NFD) andThen removeAll(
    """(?smiuU)(\p{InCombiningDiacriticalMarks}|""" +
    """\p{InCombiningDiacriticalMarksForSymbols}|""" +
    """\p{InVariationSelectors})++""")
}
