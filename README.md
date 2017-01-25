## [Dash][dash] docset generator for [ClojureDocs.org][clojuredocs]

Performs the following:

* Mirror clojuredocs.org/clojure_core (around 17mb) using wget
* Copy html content to default dash docset template
* Parse all functions from clojure_core.html
* Populate searchIndex in docSet.dsidx (sqlite db)

## Installation

Install the following dependencies:

    brew install wget
    brew install leiningen

## Usage

Generate with

    lein run

Import the generated clojure-docs.docset into [Dash][dash].

## License

Copyright © 2013 Lokeshwaran

Distributed under the Eclipse Public License, the same as Clojure.

[clojuredocs]: http://clojuredocs.org
[dash]: http://kapeli.com/dash
[httrack]: http://www.httrack.com
[releases]: https://github.com/dlokesh/clojuredocs-docset/releases
