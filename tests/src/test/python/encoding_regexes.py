from ftfy.chardata import ENCODING_REGEXES
import codecs

# Saves all known encoding regexes in an easy to compare format.
ROOT = "../resources/kantan/text/sanitize/"

with codecs.open(ROOT + 'encoding_regexes.txt', 'w', 'utf-8') as f:
  for encoding in ENCODING_REGEXES:
    if(encoding == "latin-1"):
      f.write("iso-8859-1")
    else:
      f.write(encoding)
    f.write(" ")
    f.write(ENCODING_REGEXES[encoding].pattern)
    f.write("\n")
