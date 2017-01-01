import ftfy.bad_codecs
import codecs

# Saves the expected encoding and decoding result for the first 256 bytes, by sloppy encoding.

ROOT = "../resources/kantan/text/sanitize/"

def write_encoded(name):
  str = ''.join(list(bytearray(range(256)).decode('latin-1')))

  with codecs.open(ROOT + 'encoded-' + name + '.txt', 'w', name, 'replace') as f:
    f.write(str)

def write_decoded(name):
  str = ''.join(list(bytearray(range(256)).decode(name)))

  with codecs.open(ROOT + 'decoded-' + name + '.txt', 'w', 'utf-8', 'replace') as f:
    f.write(str)

encodings = (
    ['windows-%s' % num for num in range(1250, 1259)] +
    ['iso-8859-%s' % num for num in (3, 6, 7, 8, 11)] + ['cp874']
)

write_encoded('latin-1')
for encoding in encodings:
  write_encoded('sloppy-' + encoding)
  write_decoded('sloppy-' + encoding)
