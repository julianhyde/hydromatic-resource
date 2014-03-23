# hydromatic-resource release history

For a full list of releases, see <a href="https://github.com/julianhyde/hydromatic-resource/releases">github</a>.

## <a href="https://github.com/julianhyde/hydromatic-resource/releases/tag/hydromatic-resource-0.2">0.2</a> / 2014-03-23

* Eclipse integration: Add m2e lifecycle mapping.
* Fix <a href="https://github.com/julianhyde/optiq/issues/192">optiq #192</a>,
 "Remove dependency of Resources on Jackson".
* Remove template, and instead copy `Resources.java` source file out of jar.
* Deduce exception class out of generic type parameter
  (based on <a href="https://github.com/julianhyde/optiq/commit/d35efc5e454059c90d1192b969df0ae4f741e987">optiq commit d35efc</a> by Vladimir Sitnikov)
* Validate that every message has even number of quotes
  (based on <a href="https://github.com/julianhyde/optiq/commit/d35efc5e454059c90d1192b969df0ae4f741e987">optiq commit d35efc</a> by Vladimir Sitnikov)
* Add package javadoc.

## <a href="https://github.com/julianhyde/hydromatic-resource/releases/tag/hydromatic-resource-0.1">0.1</a> / 2014-03-22

* Create maven plugin.
* Add history, readme, set up build and checkstyle.
* Initial code.

## Origins

Initial code from
<a href="https://github.com/julianhyde/eigenbase-resgen">eigenbase-resgen</a>
project.
