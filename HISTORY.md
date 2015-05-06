# hydromatic-resource release history

For a full list of releases, see <a href="https://github.com/julianhyde/hydromatic-resource/releases">github</a>.

## <a href="https://github.com/julianhyde/hydromatic-resource/releases/tag/hydromatic-resource-0.5">0.5</a> / 2015-05-06

* [<a href="https://github.com/julianhyde/hydromatic-resource/issues/1">RESOURCE-1</a>]
  Javadoc generation fails under JDK 1.8
* Add `Resources.create(Class)` method
* Improve javadoc

## <a href="https://github.com/julianhyde/hydromatic-resource/releases/tag/hydromatic-resource-0.4">0.4</a> / 2015-03-05

* Publish releases to <a href="http://search.maven.org/">Maven Central</a>
  (previous releases are in <a href="http://www.conjars.org/">Conjars</a>)
* Sign jars
* Use <a href="https://github.com/julianhyde/hydromatic-parent">net.hydromatic parent POM</a>

## <a href="https://github.com/julianhyde/hydromatic-resource/releases/tag/hydromatic-resource-0.3">0.3</a> / 2014-12-17

* Fix maven-release-plugin git integration
* Skip overwrite of the destination file if the produced contents are the same
  (Vladimir Sitnikov)
* Upgrade dependencies; fix license

## <a href="https://github.com/julianhyde/hydromatic-resource/releases/tag/hydromatic-resource-0.2">0.2</a> / 2014-03-23

* Eclipse integration: Add m2e lifecycle mapping.
* [<a href="https://issues.apache.org/jira/browse/CALCITE-192">CALCITE-192</a>]
  Remove dependency of Resources on Jackson
* Remove template, and instead copy `Resources.java` source file out of jar
* Deduce exception class out of generic type parameter
  (based on <a href="https://github.com/julianhyde/optiq/commit/d35efc5e454059c90d1192b969df0ae4f741e987">optiq commit d35efc</a> by Vladimir Sitnikov)
* Validate that every message has even number of quotes
  (based on <a href="https://github.com/julianhyde/optiq/commit/d35efc5e454059c90d1192b969df0ae4f741e987">optiq commit d35efc</a> by Vladimir Sitnikov)
* Add package javadoc

## <a href="https://github.com/julianhyde/hydromatic-resource/releases/tag/hydromatic-resource-0.1">0.1</a> / 2014-03-22

* Create maven plugin
* Add history, readme, set up build and checkstyle
* Initial code

## Origins

Initial code from
<a href="https://github.com/julianhyde/eigenbase-resgen">eigenbase-resgen</a>
project.
