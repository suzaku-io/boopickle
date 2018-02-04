# Shapeless bindings

BooPickle provides bindings for shapeless. You can use them by adding the following dependency to your Scala project:

<pre><code class="lang-scala">"io.suzaku" %% "boopickle-shapeless" % "{{ book.version }}"</code></pre>

This module can be used instead of the default macro implementation for automatically deriving picklers of case classes, ADT hierarchies and tuples. You can use it in your code by importing:

```scala
import boopickle.shapeless.Default._ // instead of boopickle.Default._
```
