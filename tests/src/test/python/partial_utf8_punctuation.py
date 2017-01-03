from ftfy.chardata import PARTIAL_UTF8_PUNCT_RE
import codecs

# Saves all known encoding regexes in an easy to compare format.
ROOT = "../resources/kantan/text/sanitize/"

with codecs.open(ROOT + 'partial_utf8_punctuation.txt', 'w', 'utf-8') as f:
  f.write(PARTIAL_UTF8_PUNCT_RE.pattern)
