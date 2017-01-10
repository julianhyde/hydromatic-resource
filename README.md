[![Build Status](https://travis-ci.org/julianhyde/hydromatic-resource.png)](https://travis-ci.org/julianhyde/hydromatic-resource)

# hydromatic-resource

Compiler-checked wrappers around resource and property files.

## An example

This example shows how you can create a wrapper around some resources
and properties. Suppose you have a properties file `birthday.properties`:
                                 
```properties
com.example.likes.cake=true
com.example.favorite.cake.flavor=Chocolate
```

and a resource bundle with resources in English in
`com/example/BirthdayResource_en_US.properties`:

```properties
HappyBirthday=Happy birthday, {0}! You don't look {1,number}.
TooOld={0}, you're too old for cake!
```

and resources in French in `com/example/BirthdayResource_fr_FR.properties`:

```properties
HappyBirthday=Bon anniversaire, {0}! {1,number}, quel bon âge.
```

Write the following interface as a wrapper:

```java
interface Birthday {
  @BaseMessage("Happy birthday, {0}! You don't look {1,number}.")
  Inst happyBirthday(String name, int age);
  
  @BaseMessage("{0} is too old.")
  ExInst<RuntimeException> tooOld(String name);

  @BaseMessage("Have some {0} cake.")
  Inst haveSomeCake(String flavor);

  @Default("false")
  @Resource("com.example.likes.cake")
  BoolProp likesCake();

  @Resource("com.example.favorite.cake.flavor")
  StringProp cakeFlavor(); 

  @Default("10")
  IntProp maximumAge(); 
}
```

Now you can materialize the wrapper based on the resource bundle and
properties file:

```java
Birthday birthday = Resources.create("com.example.BirthdayResource",
  "birthday.properties", Birthday.class);
```

and use it in a program:

```java
void celebrate(String name, int age) {
  if (age > birthday.maximumAge().get()) {
    throw birthday.tooOld(name).ex();
  }
  System.out.println(birthday.happyBirthday(name, age).str());
  if (birthday.likesCake()) {
    System.out.println(birthday.haveSomeCake(birthday.cakeFlavor()).str());
  }
}
```

Note that some resources and properties do not occur in the files.
The `@BaseMessage` and `@Default` annotations supply default values.
In the case of resources, Java's resource mechanism inherits
resources from more general locales: for instance, US English
('en_US') inherits from English ('en').

The wrapper converts properties to the right type, substitutes
parameters, validates that localized resources have the same number
and type of parameters as the base message, and creates exceptions
for error conditions.

Resources inherit the JVM's locale, but you can override for the
current thread:
 
```java
Resources.setThreadLocale(locale);
```

and even on the resource instance:
 
```java
throw birthday.tooOld("Fred").localize(locale).ex();
```

## Get hydromatic-resource

### From Maven

Get hydromatic-resource from
<a href="https://search.maven.org/#search%7Cga%7C1%7Cg%3Anet.hydromatic%20a%3Ahydromatic-resource-maven-plugin">Maven Central</a>:

```xml
<dependency>
  <groupId>net.hydromatic</groupId>
  <artifactId>hydromatic-resource-maven-plugin</artifactId>
  <version>0.6</version>
</dependency>
```

### Download and build

You need Java (1.6 or higher; 9 preferred), git and maven (3.2.1 or later).

```bash
$ git clone git://github.com/julianhyde/hydromatic-resource.git
$ cd hydromatic-resource
$ mvn package
```

### Make a release

Using JDK 1.7, follow instructions in
[hydromatic-parent](https://github.com/julianhyde/hydromatic-parent).

## More information

* License: Apache License, Version 2.0
* Author: Julian Hyde
* Blog: http://julianhyde.blogspot.com
* Project page: http://www.hydromatic.net/hydromatic-resource
* Source code: http://github.com/julianhyde/hydromatic-resource
* Developers list: <a href="mailto:dev@calcite.apache.org">dev at calcite.apache.org</a>
  (<a href="http://mail-archives.apache.org/mod_mbox/calcite-dev/">archive</a>,
  <a href="mailto:dev-subscribe@calcite.apache.org">subscribe</a>)
* <a href="HISTORY.md">Release notes and history</a>
