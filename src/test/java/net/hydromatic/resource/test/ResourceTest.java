/*
 * Licensed to Julian Hyde under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership. Julian Hyde
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hydromatic.resource.test;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Properties;

import net.hydromatic.resource.Resources;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import static net.hydromatic.resource.Resources.*;

/**
 * Tests for the {@link Resources} framework.
 */
public class ResourceTest {
  final FooResource fooResource =
      Resources.create("net.hydromatic.resource.test.ResourceTest",
          FooResource.class);

  @Test public void testSimple() {
    assertThat(fooResource.helloWorld().str(), equalTo("hello, world!"));
    assertThat(fooResource.differentMessageInPropertiesFile().str(),
        equalTo("message in properties file"));
    assertThat(fooResource.onlyInClass().str(),
        equalTo("only in class"));
    assertThat(fooResource.onlyInPropertiesFile().str(),
        equalTo("message in properties file"));
  }

  @Test public void testProperty() {
    assertThat(fooResource.helloWorld().getProperties().size(), equalTo(0));
    assertThat(fooResource.withProperty(0).str(), equalTo("with properties 0"));
    assertThat(fooResource.withProperty(0).getProperties().size(), equalTo(1));
    assertThat(fooResource.withProperty(0).getProperties().get("prop"),
        equalTo("my value"));
    assertThat(fooResource.withProperty(1000).getProperties().get("prop"),
        equalTo("my value"));
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  @Test public void testException() {
    assertThat(fooResource.illArg("xyz").ex().getMessage(),
        equalTo("bad arg xyz"));
    assertThat(fooResource.illArg("xyz").ex().getCause(), nullValue());
    final Throwable npe = new NullPointerException();
    assertThat(fooResource.illArg("").ex(npe).getCause(), equalTo(npe));
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  @Test public void testSuperChainException() {
    assertThat(fooResource.exceptionSuperChain().ex().getMessage(),
        equalTo("super chain exception"));
    assertThat(fooResource.exceptionSuperChain().ex().getClass().getName(),
        equalTo(IllegalStateException.class.getName()));
  }

  /** Tests that get validation error if bundle does not contain resource. */
  @Test public void testValidateBundleHasResource() {
    try {
      fooResource.onlyInClass().validate(
          EnumSet.of(Validation.BUNDLE_HAS_RESOURCE));
      fail("should have thrown");
    } catch (AssertionError e) {
      assertThat(e.getMessage(),
          startsWith(
              "key 'OnlyInClass' not found for resource 'onlyInClass' in "
              + "bundle 'java.util.PropertyResourceBundle@"));
    }
  }

  @Test public void testValidateAtLeastOne() {
    // succeeds - has several resources
    Resources.validate(fooResource, EnumSet.of(Validation.AT_LEAST_ONE));

    // fails validation - has no resources
    try {
      Resources.validate("foo", EnumSet.of(Validation.AT_LEAST_ONE));
      fail("should have thrown");
    } catch (AssertionError e) {
      assertThat(e.getMessage(),
          equalTo("resource object foo contains no resources"));
    }
  }

  @Test public void testValidateMessageSpecified() {
    try {
      Resources.validate(fooResource, EnumSet.of(Validation.MESSAGE_SPECIFIED));
      fail("should have thrown");
    } catch (AssertionError e) {
      assertThat(e.getMessage(),
          equalTo("resource 'onlyInPropertiesFile' must specify BaseMessage"));
    }
  }

  @Test public void testValidateMessageMatchDifferentMessageInPropertiesFile() {
    try {
      fooResource.differentMessageInPropertiesFile().validate(
          EnumSet.of(Validation.MESSAGE_MATCH));
      fail("should have thrown");
    } catch (AssertionError e) {
      assertThat(e.getMessage(),
          equalTo(
              "message for resource 'differentMessageInPropertiesFile' is different between class and resource file"));
    }
  }

  @Test public void testValidateOddQuotes() {
    try {
      fooResource.oddQuotes().validate(EnumSet.of(Validation.EVEN_QUOTES));
      fail("should have thrown");
    } catch (AssertionError e) {
      assertThat(e.getMessage(),
          equalTo("resource 'oddQuotes' should have even number of quotes"));
    }
  }

  @Test public void testValidateCreateException() {
    try {
      fooResource.myException().validate(
          EnumSet.of(Validation.CREATE_EXCEPTION));
      fail("should have thrown");
    } catch (AssertionError e) {
      assertThat(e.getMessage(),
          equalTo("error instantiating exception for resource 'myException'"));
      assertThat(e.getCause().getMessage(),
          equalTo(
              "java.lang.NoSuchMethodException: net.hydromatic.resource.test.ResourceTest$MyException.<init>(java.lang.String, java.lang.Throwable)"));
    }
  }

  @Test public void testValidateCauselessFail() {
    try {
      fooResource.causelessFail().validate(
          EnumSet.of(Validation.CREATE_EXCEPTION));
      fail("should have thrown");
    } catch (AssertionError e) {
      assertThat(e.getMessage(),
          equalTo("error instantiating exception for resource "
                  + "'causelessFail'"));
      assertThat(e.getCause().getMessage(),
          equalTo(
              "Cause is required, message = can't be used causeless"));
    }
  }

  @Test public void testValidateExceptionWithCause() {
    fooResource.exceptionWithCause().validate(
        EnumSet.of(Validation.CREATE_EXCEPTION));
  }

  @Test public void testValidateMatchArguments() {
    try {
      Resources.validate(fooResource, EnumSet.of(Validation.ARGUMENT_MATCH));
      fail("should have thrown");
    } catch (AssertionError e) {
      assertThat(e.getMessage(),
          equalTo(
              "type mismatch in method 'mismatchedArguments' between message format elements [class java.lang.String, int] and method parameters [class java.lang.String, int, class java.lang.String]"));
    }
  }

  @Test public void testDeduceExceptionType() throws NoSuchMethodException {
    // Parse "ExInst<MyException>" --> "MyException"
    final Method method = FooResource.class.getMethod("myException");
    assertThat(
        Resources.ExInstWithCause.getExceptionClass(
            method.getGenericReturnType()),
        equalTo((Class) MyException.class));
  }

  @Test public void testDeduceExceptionType2() throws NoSuchMethodException {
    // Parse "MyExInst<NumberFormatException>" --> "NumberFormatException"
    final Method method =
        FooResource.class.getMethod("customParameterizedExceptionClass");
    assertThat(
        Resources.ExInstWithCause.getExceptionClass(
            method.getGenericReturnType()),
        equalTo((Class) NumberFormatException.class));
  }

  @Test public void testDeduceExceptionType3() throws NoSuchMethodException {
    // Parse "MyExInstImpl extends MyExInst<IllegalStateException>"
    // --> "IllegalStateException"
    final Method method = FooResource.class.getMethod("exceptionSuperChain");
    assertThat(
        Resources.ExInstWithCause.getExceptionClass(
            method.getGenericReturnType()),
        equalTo((Class) IllegalStateException.class));
  }

  @Test public void testIntPropEmpty() {
    final FooResource r = Resources.create(FooResource.class);
    final IntProp p = r.intPropNoDefault();
    try {
      final int actual = p.get();
      fail("expected error, got " + actual);
    } catch (RuntimeException e) {
      assertThat(e.getMessage(),
          is("Property IntPropNoDefault has no default value"));
    }
    assertThat(p.get(1), is(1));
    assertThat(p.isSet(), is(false));
  }

  @Test public void testIntProp() {
    final Properties properties = new Properties();
    final FooResource r = Resources.create(properties, FooResource.class);

    final IntProp p = r.intPropNoDefault();
    final IntProp p5 = r.intPropDefaultFive();
    assertThat(p.hasDefault(), is(false));
    assertThat(p5.hasDefault(), is(true));

    try {
      final int actual = p.defaultValue();
      fail("expected error, got " + actual);
    } catch (NoDefaultValueException e) {
      assertThat(e.getMessage(),
          is("Property IntPropNoDefault has no default value"));
    }
    assertThat(p5.defaultValue(), is(-50));

    try {
      final int actual = p.get();
      fail("expected error, got " + actual);
    } catch (NoDefaultValueException e) {
      assertThat(e.getMessage(),
          is("Property IntPropNoDefault is not set and has no default value"));
    }
    assertThat(p.get(1), is(1));
    assertThat(p.isSet(), is(false));

    assertThat(p5.get(), is(-50));
    assertThat(p5.get(1), is(1));
    assertThat(p5.isSet(), is(false));

    properties.setProperty("OtherProperty", "111");
    assertThat(p.isSet(), is(false));
    assertThat(p5.isSet(), is(false));

    properties.setProperty("IntPropNoDefault", "3 ");
    try {
      final int actual = p.get();
      fail("expected error, got " + actual);
    } catch (NumberFormatException e) {
      assertThat(e.getMessage(),
          is("For input string: \"3 \""));
    }
    try {
      final int actual = p.get(1);
      fail("expected error, got " + actual);
    } catch (NumberFormatException e) {
      assertThat(e.getMessage(),
          is("For input string: \"3 \""));
    }
    assertThat(p.isSet(), is(true));

    properties.setProperty("IntPropNoDefault", "3");
    assertThat(p.get(), is(3));
    assertThat(p.get(1), is(3));
    assertThat(p.isSet(), is(true));

    properties.setProperty("IntPropDefaultFive", "-50");
    assertThat(p5.get(), is(-50));
    assertThat(p5.get(1), is(-50));
    assertThat(p5.isSet(), is(true));
  }

  @Test public void testDoubleProp() {
    final Properties properties = new Properties();
    final FooResource r = Resources.create(properties, FooResource.class);

    final DoubleProp p = r.doublePropNoDefault();
    final DoubleProp pHalf = r.doublePropDefaultHalf();
    assertThat(p.hasDefault(), is(false));
    assertThat(pHalf.hasDefault(), is(true));

    try {
      final double actual = p.defaultValue();
      fail("expected error, got " + actual);
    } catch (NoDefaultValueException e) {
      assertThat(e.getMessage(),
          is("Property DoublePropNoDefault has no default value"));
    }
    assertThat(pHalf.defaultValue(), is(0.5d));

    try {
      final double actual = p.get();
      fail("expected error, got " + actual);
    } catch (NoDefaultValueException e) {
      assertThat(e.getMessage(),
          is("Property DoublePropNoDefault is not set and has no default "
              + "value"));
    }
    assertThat(p.get(1d), is(1d));
    assertThat(p.isSet(), is(false));

    assertThat(pHalf.get(), is(0.5d));
    assertThat(pHalf.get(1), is(1d));
    assertThat(pHalf.isSet(), is(false));

    properties.setProperty("OtherProperty", "111");
    assertThat(p.isSet(), is(false));
    assertThat(pHalf.isSet(), is(false));

    // Trailing spaces are OK for parsing doubles
    properties.setProperty("DoublePropNoDefault", "3 ");
    assertThat(p.get(), is(3d));
    assertThat(p.get(1), is(3d));
    assertThat(p.isSet(), is(true));

    properties.setProperty("DoublePropNoDefault", "3z");
    try {
      final double actual = p.get();
      fail("expected error, got " + actual);
    } catch (NumberFormatException e) {
      assertThat(e.getMessage(),
          is("For input string: \"3z\""));
    }
    try {
      final double actual = p.get(1);
      fail("expected error, got " + actual);
    } catch (NumberFormatException e) {
      assertThat(e.getMessage(),
          is("For input string: \"3z\""));
    }
    assertThat(p.isSet(), is(true));

    properties.setProperty("DoublePropNoDefault", "-3.25");
    assertThat(p.get(), is(-3.25d));
    assertThat(p.get(1), is(-3.25d));
    assertThat(p.isSet(), is(true));

    properties.setProperty("DoublePropDefaultHalf", "-8.50");
    assertThat(pHalf.get(), is(-8.5d));
    assertThat(pHalf.get(1), is(-8.5d));
    assertThat(pHalf.isSet(), is(true));
  }

  @Test public void testBooleanProp() {
    final Properties properties = new Properties();
    final FooResource r = Resources.create(properties, FooResource.class);

    final BooleanProp p = r.booleanPropNoDefault();
    final BooleanProp pTrue = r.booleanPropDefaultTrue();
    final BooleanProp pBad = r.booleanPropBadDefault();
    assertThat(p.hasDefault(), is(false));
    assertThat(pTrue.hasDefault(), is(true));
    assertThat(pBad.hasDefault(), is(true));

    try {
      final boolean actual = p.defaultValue();
      fail("expected error, got " + actual);
    } catch (NoDefaultValueException e) {
      assertThat(e.getMessage(),
          is("Property BooleanPropNoDefault has no default value"));
    }
    assertThat(pTrue.defaultValue(), is(true));
    assertThat(pBad.defaultValue(), is(false));

    try {
      final boolean actual = p.get();
      fail("expected error, got " + actual);
    } catch (NoDefaultValueException e) {
      assertThat(e.getMessage(),
          is("Property BooleanPropNoDefault is not set and has no default "
              + "value"));
    }
    assertThat(p.get(true), is(true));
    assertThat(p.get(false), is(false));
    assertThat(p.isSet(), is(false));

    assertThat(pTrue.get(), is(true));
    assertThat(pTrue.get(false), is(false));
    assertThat(pTrue.get(true), is(true));
    assertThat(pTrue.isSet(), is(false));

    assertThat(pBad.get(), is(false));
    assertThat(pBad.get(true), is(true));
    assertThat(pBad.get(false), is(false));
    assertThat(pBad.isSet(), is(false));

    properties.setProperty("OtherProperty", "111");
    assertThat(p.isSet(), is(false));
    assertThat(pTrue.isSet(), is(false));
    assertThat(pBad.isSet(), is(false));

    // Boolean properties are lenient in parsing.
    // Everything that is not "true" or "TRUE" is false.
    // Never throws.
    properties.setProperty("BooleanPropNoDefault", "3 ");
    assertThat(pBad.get(), is(false));
    assertThat(pBad.get(true), is(true));
    assertThat(p.isSet(), is(true));

    properties.setProperty("BooleanPropNoDefault", "false");
    assertThat(p.get(), is(false));
    assertThat(p.get(false), is(false));
    assertThat(p.get(true), is(false));
    assertThat(p.isSet(), is(true));

    properties.setProperty("BooleanPropDefaultTrue", "false");
    assertThat(pTrue.get(), is(false));
    assertThat(pTrue.get(true), is(false));
    assertThat(pTrue.get(false), is(false));
    assertThat(pTrue.isSet(), is(true));

    properties.setProperty("BooleanPropDefaultTrue", "true");
    assertThat(pTrue.get(), is(true));
    assertThat(pTrue.get(true), is(true));
    assertThat(pTrue.get(false), is(true));
    assertThat(pTrue.isSet(), is(true));

    properties.setProperty("BooleanPropBadDefault", "false");
    assertThat(pBad.get(), is(false));
    assertThat(pBad.get(true), is(false));
    assertThat(pBad.get(false), is(false));
    assertThat(pBad.isSet(), is(true));
  }

  @Test public void testStringProp() {
    final Properties properties = new Properties();
    final FooResource r = Resources.create(properties, FooResource.class);

    final StringProp p = r.stringPropNoDefault();
    final StringProp p5 = r.stringPropDefaultXyz();
    assertThat(p.hasDefault(), is(false));
    assertThat(p5.hasDefault(), is(true));

    try {
      final String actual = p.defaultValue();
      fail("expected error, got " + actual);
    } catch (NoDefaultValueException e) {
      assertThat(e.getMessage(),
          is("Property StringPropNoDefault has no default value"));
    }
    assertThat(p5.defaultValue(), is("xyz"));

    try {
      final String actual = p.get();
      fail("expected error, got " + actual);
    } catch (NoDefaultValueException e) {
      assertThat(e.getMessage(),
          is("Property StringPropNoDefault is not set and has no default "
              + "value"));
    }
    assertThat(p.get(""), is(""));
    assertThat(p.get("a b"), is("a b"));
    assertThat(p.isSet(), is(false));

    assertThat(p5.get(), is("xyz"));
    assertThat(p5.get("a b"), is("a b"));
    assertThat(p5.isSet(), is(false));

    properties.setProperty("OtherProperty", "111");
    assertThat(p.isSet(), is(false));
    assertThat(p5.isSet(), is(false));

    properties.setProperty("StringPropNoDefault", "3 ");
    assertThat(p.get(), is("3 "));
    assertThat(p.isSet(), is(true));

    properties.setProperty("StringPropNoDefault", "3 ");
    assertThat(p.get(), is("3 "));
    assertThat(p.get("1"), is("3 "));
    assertThat(p.isSet(), is(true));

    properties.setProperty("StringPropDefaultXyz", "-50");
    assertThat(p5.get(), is("-50"));
    assertThat(p5.get("1"), is("-50"));
    assertThat(p5.isSet(), is(true));
  }

  @Test public void testPropPath() {
    final Properties properties = new Properties();
    final FooResource r = Resources.create(properties, FooResource.class);

    final IntProp p = r.intPropPathDefault();
    assertThat(p.hasDefault(), is(true));

    assertThat(p.defaultValue(), is(56));

    assertThat(p.get(), is(56));
    assertThat(p.get(1), is(1));
    assertThat(p.isSet(), is(false));

    properties.setProperty("OtherProperty", "111");
    assertThat(p.isSet(), is(false));

    // Setting its method name has no effect
    properties.setProperty("IntPropPathDefault", "3");
    assertThat(p.get(), is(56));
    assertThat(p.get(1), is(1));
    assertThat(p.isSet(), is(false));

    properties.setProperty("com.example.my.int.property", "3");
    assertThat(p.get(), is(3));
    assertThat(p.get(1), is(3));
    assertThat(p.isSet(), is(true));

    properties.setProperty("com.example.my.int.property", "3 ");
    try {
      final int actual = p.get();
      fail("expected error, got " + actual);
    } catch (NumberFormatException e) {
      assertThat(e.getMessage(),
          is("For input string: \"3 \""));
    }
    try {
      final int actual = p.get(1);
      fail("expected error, got " + actual);
    } catch (NumberFormatException e) {
      assertThat(e.getMessage(),
          is("For input string: \"3 \""));
    }
    assertThat(p.isSet(), is(true));
  }

  @Test public void testBadDefaultProp() {
    final Properties properties = new Properties();

    final BadIntResource r =
        Resources.create(properties, BadIntResource.class);
    try {
      final IntProp p = r.intPropBadDefault();
      fail("expected error, got " + p);
    } catch (NumberFormatException e) {
      assertThat(e.getMessage(),
          is("For input string: \"a3\""));
    }

    final BadDoubleResource r2 =
        Resources.create(properties, BadDoubleResource.class);
    try {
      final DoubleProp p = r2.doublePropBadDefault();
      fail("expected error, got " + p);
    } catch (NumberFormatException e) {
      assertThat(e.getMessage(),
          is("For input string: \"1.5xx\""));
    }

  }

  // TODO: check that each resource in the bundle is used by precisely
  //  one method

  /** Exception that cannot be thrown by {@link ExInst} because it does not have
   * a (String, Throwable) constructor, nor does it have a (String)
   * constructor. */
  public static class MyException extends RuntimeException {
    public MyException() {
      super();
    }
  }

  /** Abstract class used to test identification of exception classes via
   * superclass chains */
  public abstract static class MyExInst<W extends Exception> extends ExInst<W> {
    public MyExInst(String base, Locale locale, Method method, Object... args) {
      super(base, locale, method, args);
    }
  }

  public static class MyConcreteExInst<W extends Exception> extends ExInst<W> {
    public MyConcreteExInst(String base, Locale locale, Method method,
        Object... args) {
      super(base, locale, method, args);
    }
  }

  /** Subtype of ExInst, however exception type is not directly
   * passed to ExInst. The test must still detect the correct class. */
  public static class MyExInstImpl extends MyExInst<IllegalStateException> {
    public MyExInstImpl(String base, Locale locale, Method method,
        Object... args) {
      super(base, locale, method, args);
    }
  }

  /** Exception that always requires cause
   */
  public static class MyExceptionRequiresCause extends RuntimeException {
    public MyExceptionRequiresCause(String message, Throwable cause) {
      super(message, cause);
      if (cause == null) {
        throw new IllegalArgumentException("Cause is required, "
            + "message = " + message);
      }
    }
  }

  /** A resource object to be tested. Has one of each flaw. */
  public interface FooResource {
    @BaseMessage("hello, world!")
    Inst helloWorld();

    @BaseMessage("message in class")
    Inst differentMessageInPropertiesFile();

    @BaseMessage("only in class")
    Inst onlyInClass();

    Inst onlyInPropertiesFile();

    @BaseMessage("with properties {0,number}")
    @Property(name = "prop", value = "my value")
    Inst withProperty(int x);

    @BaseMessage("bad arg {0}")
    ExInst<IllegalArgumentException> illArg(String s);

    @BaseMessage("should return inst")
    String shouldReturnInst();

    @BaseMessage("exception isn''t throwable")
    ExInst<MyException> myException();

    @BaseMessage("Can't use odd quotes")
    Inst oddQuotes();

    @BaseMessage("can''t be used causeless")
    ExInst<MyExceptionRequiresCause> causelessFail();

    @BaseMessage("should work since cause is provided")
    ExInstWithCause<MyExceptionRequiresCause> exceptionWithCause();

    @BaseMessage("argument {0} does not match {1,number,#}")
    Inst mismatchedArguments(String s, int i, String s2);

    @BaseMessage("custom parameterized exception class")
    MyConcreteExInst<NumberFormatException> customParameterizedExceptionClass();

    @BaseMessage("super chain exception")
    MyExInstImpl exceptionSuperChain();

    IntProp intPropNoDefault();

    @Default("-50")
    IntProp intPropDefaultFive();

    @Default("56")
    @Resource("com.example.my.int.property")
    IntProp intPropPathDefault();

    StringProp stringPropNoDefault();

    @Default("xyz")
    StringProp stringPropDefaultXyz();

    BooleanProp booleanPropNoDefault();

    @Default("true")
    BooleanProp booleanPropDefaultTrue();

    @Default("null")
    BooleanProp booleanPropBadDefault();

    DoubleProp doublePropNoDefault();

    @Default("0.5")
    DoubleProp doublePropDefaultHalf();

  }

  interface BadIntResource {
    @Default("a3")
    IntProp intPropBadDefault();
  }

  interface BadDoubleResource {
    @Default("1.5xx")
    DoubleProp doublePropBadDefault();
  }
}

// End ResourceTest.java
