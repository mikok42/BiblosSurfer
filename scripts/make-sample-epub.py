#!/usr/bin/env python3
"""Generate the sample EPUB used by the UI-test stub and the publication tests.

The file is committed, so this script only needs running when the sample content changes.

    python3 scripts/make-sample-epub.py

Writes ios/BiblosSurfer/Resources/SampleBook.epub
"""

from __future__ import annotations

import pathlib
import zipfile

TITLE = "The Sample Voyage"
AUTHOR = "Ferdek Test"
LANGUAGE = "en"
IDENTIFIER = "urn:uuid:6a1b0c1e-0000-4000-8000-biblossurfer01"

CHAPTERS = [
    (
        "chapter1.xhtml",
        "Chapter One",
        [
            "The ship left harbour before dawn.",
            "Nobody on board could name the sea they were crossing.",
            "By the third day the water had turned the colour of slate, and the captain "
            "stopped writing in the log.",
        ],
    ),
    (
        "chapter2.xhtml",
        "Chapter Two",
        [
            "Land appeared on the ninth morning.",
            "It was smaller than the charts promised and quieter than anyone expected.",
            "They anchored anyway, because the alternative was another week of slate-coloured "
            "water and an empty log.",
        ],
    ),
    (
        "chapter3.xhtml",
        "Chapter Three",
        [
            "What they found there is the reason this book exists.",
            "It is also the reason the crew never sailed together again.",
            "The last page of the log holds a single sentence, written in a hand nobody "
            "recognised.",
        ],
    ),
]

CONTAINER_XML = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>
"""

STYLESHEET = """body { font-family: serif; line-height: 1.5; margin: 1em; }
h1 { font-size: 1.4em; margin-bottom: 1em; }
p { margin: 0 0 1em 0; }
"""


def chapter_xhtml(title: str, paragraphs: list[str]) -> str:
    body = "\n".join(f"    <p>{p}</p>" for p in paragraphs)
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"
      xml:lang="{LANGUAGE}" lang="{LANGUAGE}">
  <head>
    <title>{title}</title>
    <link rel="stylesheet" type="text/css" href="style.css"/>
  </head>
  <body>
    <h1>{title}</h1>
{body}
  </body>
</html>
"""


def content_opf() -> str:
    manifest = "\n".join(
        f'    <item id="{href.split(".")[0]}" href="{href}" media-type="application/xhtml+xml"/>'
        for href, _, _ in CHAPTERS
    )
    spine = "\n".join(f'    <itemref idref="{href.split(".")[0]}"/>' for href, _, _ in CHAPTERS)
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="pub-id"
         xml:lang="{LANGUAGE}">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="pub-id">{IDENTIFIER}</dc:identifier>
    <dc:title>{TITLE}</dc:title>
    <dc:creator>{AUTHOR}</dc:creator>
    <dc:language>{LANGUAGE}</dc:language>
    <meta property="dcterms:modified">2026-08-21T00:00:00Z</meta>
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
    <item id="css" href="style.css" media-type="text/css"/>
{manifest}
  </manifest>
  <spine>
{spine}
  </spine>
</package>
"""


def nav_xhtml() -> str:
    items = "\n".join(
        f'        <li><a href="{href}">{title}</a></li>' for href, title, _ in CHAPTERS
    )
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"
      xml:lang="{LANGUAGE}" lang="{LANGUAGE}">
  <head>
    <title>Contents</title>
  </head>
  <body>
    <nav epub:type="toc" id="toc">
      <h1>Contents</h1>
      <ol>
{items}
      </ol>
    </nav>
  </body>
</html>
"""


def main() -> None:
    destination = (
        pathlib.Path(__file__).resolve().parent.parent
        / "ios"
        / "BiblosSurfer"
        / "Resources"
        / "SampleBook.epub"
    )
    destination.parent.mkdir(parents=True, exist_ok=True)

    with zipfile.ZipFile(destination, "w") as epub:
        # The mimetype entry must be first and stored uncompressed.
        epub.writestr(
            zipfile.ZipInfo("mimetype"), "application/epub+zip", compress_type=zipfile.ZIP_STORED
        )
        epub.writestr("META-INF/container.xml", CONTAINER_XML, zipfile.ZIP_DEFLATED)
        epub.writestr("OEBPS/content.opf", content_opf(), zipfile.ZIP_DEFLATED)
        epub.writestr("OEBPS/nav.xhtml", nav_xhtml(), zipfile.ZIP_DEFLATED)
        epub.writestr("OEBPS/style.css", STYLESHEET, zipfile.ZIP_DEFLATED)
        for href, title, paragraphs in CHAPTERS:
            epub.writestr(f"OEBPS/{href}", chapter_xhtml(title, paragraphs), zipfile.ZIP_DEFLATED)

    print(f"wrote {destination} ({destination.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
