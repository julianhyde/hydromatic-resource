/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package net.hydromatic.resource;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Defining wrapper classes around resources that allow the compiler to check
 * whether the resources exist and have the argument types that your code
 * expects.
 */
public class Resources {
  private static final ThreadLocal<Locale> MAP_THREAD_TO_LOCALE =
      new ThreadLocal<Locale>();

  private Resources() {}

  /** Returns the preferred locale of the current thread, or
   * the default locale if the current thread has not called {@link
   * #setThreadLocale}.
   *
   * @return Locale */
  protected static Locale getThreadOrDefaultLocale() {
    Locale locale = getThreadLocale();
    if (locale == null) {
      return Locale.getDefault();
    } else {
      return locale;
    }
  }

  /** Sets the locale for the current thread.
   *
   * @param locale Locale */
  public static void setThreadLocale(Locale locale) {
    MAP_THREAD_TO_LOCALE.set(locale);
  }

  /** Returns the preferred locale of the current thread, or null if the
   * thread has not called {@link #setThreadLocale}.
   *
   * @return Locale */
  public static Locale getThreadLocale() {
    return MAP_THREAD_TO_LOCALE.get();
  }

  public static <T> T create(final String base, Class<T> clazz) {
    //noinspection unchecked
    return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
        new Class[] {clazz},
        new InvocationHandler() {
          public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
            if (method.equals(BuiltinMethod.OBJECT_TO_STRING.method)) {
              return toString();
            }
            final Class<?> returnType = method.getReturnType();
            final Class[] types = {
              String.class, Locale.class, Method.class, Object[].class
            };
            final Constructor<?> constructor =
                returnType.getConstructor(types);
            return constructor.newInstance(
                base,
                Resources.getThreadOrDefaultLocale(),
                method,
                args != null ? args : new Object[0]);
          }
        });
  }

  /** Applies all validations to all resource methods in the given
   * resource object. */
  public static void validate(Object o) {
    validate(o, EnumSet.allOf(Validation.class));
  }

  /** Applies the given validations to all resource methods in the given
   * resource object. */
  public static void validate(Object o, EnumSet<Validation> validations) {
    int count = 0;
    for (Method method : o.getClass().getMethods()) {
      if (!Modifier.isStatic(method.getModifiers())
          && Inst.class.isAssignableFrom(method.getReturnType())) {
        ++count;
        final Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
          args[i] = zero(parameterTypes[i]);
        }
        try {
          Inst inst = (Inst) method.invoke(o, args);
          inst.validate(validations);
        } catch (IllegalAccessException e) {
          throw new RuntimeException("in " + method, e);
        } catch (InvocationTargetException e) {
          throw new RuntimeException("in " + method, e.getCause());
        }
      }
    }
    if (count == 0 && validations.contains(Validation.AT_LEAST_ONE)) {
      throw new AssertionError("resource object " + o
          + " contains no resources");
    }
  }

  private static Object zero(Class<?> clazz) {
    return clazz == String.class ? ""
        : clazz == byte.class ? (byte) 0
        : clazz == char.class ? (char) 0
        : clazz == short.class ? (short) 0
        : clazz == int.class ? 0
        : clazz == long.class ? 0L
        : clazz == float.class ? 0F
        : clazz == double.class ? 0D
        : clazz == boolean.class ? false
        : null;
  }

  /** Returns whether two objects are equal or are both null. */
  private static boolean equal(Object o0, Object o1) {
    return o0 == o1 || o0 != null && o0.equals(o1);
  }

  /** Resource instance. It contains the resource method (which
   * serves to identify the resource), the locale with which we
   * expect to render the resource, and any arguments. */
  public static class Inst {
    protected final String base;
    private final Locale locale;
    protected final Method method;
    protected final Object[] args;

    public Inst(String base, Locale locale, Method method, Object... args) {
      this.base = base;
      this.locale = locale;
      this.method = method;
      this.args = args;
    }

    @Override public boolean equals(Object obj) {
      return this == obj
          || obj != null
          && obj.getClass() == this.getClass()
          && locale == ((Inst) obj).locale
          && method == ((Inst) obj).method
          && Arrays.equals(args, ((Inst) obj).args);
    }

    @Override public int hashCode() {
      return Arrays.asList(locale, method, Arrays.asList(args)).hashCode();
    }

    public ResourceBundle bundle() {
      return ResourceBundle.getBundle(base, locale);
    }

    public Inst localize(Locale locale) {
      return new Inst(base, locale, method, args);
    }

    public void validate(EnumSet<Validation> validations) {
      final ResourceBundle bundle = bundle();
      for (Validation validation : validations) {
        final String key = key();
        switch (validation) {
        case BUNDLE_HAS_RESOURCE:
          if (!bundle.containsKey(key)) {
            throw new AssertionError("key '" + key
                + "' not found for resource '" + method.getName()
                + "' in bundle '" + bundle + "'");
          }
          break;
        case MESSAGE_SPECIFIED:
          final BaseMessage annotation1 =
              method.getAnnotation(BaseMessage.class);
          if (annotation1 == null) {
            throw new AssertionError("resource '" + method.getName()
                + "' must specify BaseMessage");
          }
          break;
        case EVEN_QUOTES:
          String message = method.getAnnotation(BaseMessage.class).value();
          if (countQuotesIn(message) % 2 == 1) {
            throw new AssertionError("resource '" + method.getName()
                + "' should have even number of quotes");
          }
          break;
        case MESSAGE_MATCH:
          final BaseMessage annotation2 =
              method.getAnnotation(BaseMessage.class);
          if (annotation2 != null) {
            final String value = annotation2.value();
            final String value2 = bundle.containsKey(key)
                ? bundle.getString(key)
                : null;
            if (!equal(value, value2)) {
              throw new AssertionError("message for resource '"
                  + method.getName()
                  + "' is different between class and resource file");
            }
          }
          break;
        case ARGUMENT_MATCH:
          String raw = raw();
          MessageFormat format = new MessageFormat(raw);
          final Format[] formats = format.getFormatsByArgumentIndex();
          final List<Class> types = new ArrayList<Class>();
          final Class<?>[] parameterTypes = method.getParameterTypes();
          for (int i = 0; i < formats.length; i++) {
            Format format1 = formats[i];
            Class parameterType = parameterTypes[i];
            final Class<?> e;
            if (format1 instanceof NumberFormat) {
              e = parameterType == short.class
                  || parameterType == int.class
                  || parameterType == long.class
                  || parameterType == float.class
                  || parameterType == double.class
                  || Number.class.isAssignableFrom(parameterType)
                  ? parameterType
                  : Number.class;
            } else if (format1 instanceof DateFormat) {
              e = Date.class;
            } else {
              e = String.class;
            }
            types.add(e);
          }
          final List<Class<?>> parameterTypeList =
              Arrays.asList(parameterTypes);
          if (!types.equals(parameterTypeList)) {
            throw new AssertionError("type mismatch in method '"
                + method.getName() + "' between message format elements "
                + types + " and method parameters " + parameterTypeList);
          }
          break;
        }
      }
    }

    private int countQuotesIn(String message) {
      int count = 0;
      for (int i = 0, n = message.length(); i < n; i++) {
        if (message.charAt(i) == '\'') {
          ++count;
        }
      }
      return count;
    }

    public String str() {
      String message = raw();
      MessageFormat format = new MessageFormat(message);
      format.setLocale(locale);
      return format.format(args);
    }

    public String raw() {
      try {
        return bundle().getString(key());
      } catch (MissingResourceException e) {
        // Resource is not in the bundle. (It is probably missing from the
        // .properties file.) Fall back to the base message.
        return method.getAnnotation(BaseMessage.class).value();
      }
    }

    private String key() {
      final Resource resource = method.getAnnotation(Resource.class);
      if (resource != null) {
        return resource.value();
      } else {
        final String name = method.getName();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
      }
    }

    public Map<String, String> getProperties() {
      // At present, annotations allow at most one property per resource. We
      // could design new annotations if any resource needed more.
      final Property property = method.getAnnotation(Property.class);
      if (property == null) {
        return Collections.emptyMap();
      } else {
        return Collections.singletonMap(property.name(), property.value());
      }
    }
  }

  /** Sub-class of {@link Inst} that can throw an exception. Requires caused
   * by exception.*/
  public static class ExInstWithCause<T extends Exception> extends Inst {
    public ExInstWithCause(String base, Locale locale, Method method,
        Object... args) {
      super(base, locale, method, args);
    }

    @Override public Inst localize(Locale locale) {
      return new ExInstWithCause<T>(base, locale, method, args);
    }

    public T ex(Throwable cause) {
      try {
        //noinspection unchecked
        final Class<T> exceptionClass = getExceptionClass();
        Constructor<T> constructor;
        final String str = str();
        boolean causeInConstructor = false;
        try {
          constructor = exceptionClass.getConstructor(String.class,
              Throwable.class
          );
          causeInConstructor = true;
        } catch (NoSuchMethodException nsmStringThrowable) {
          try {
            constructor = exceptionClass.getConstructor(String.class);
          } catch (NoSuchMethodException nsmString) {
            // Ignore nsmString to encourage users to have (String,
            // Throwable) constructors.
            throw nsmStringThrowable;
          }
        }
        if (causeInConstructor) {
          return constructor.newInstance(str, cause);
        }
        T ex = constructor.newInstance(str);
        if (cause != null) {
          try {
            ex.initCause(cause);
          } catch (IllegalStateException iae) {
            // Sorry, unable to add cause via constructor and via initCause
          }
        }
        return ex;
      } catch (InstantiationException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        if (e.getCause() instanceof Error) {
          throw (Error) e.getCause();
        } else if (e.getCause() instanceof RuntimeException) {
          throw (RuntimeException) e.getCause();
        } else {
          throw new RuntimeException(e);
        }
      }
    }

    private Class<T> getExceptionClass() {
      // Get exception type from ExInstWithCause<MyException> type parameter
      // ExInstWithCause might be one of super classes.
      // That is why we need findTypeParameters to find ExInstWithCause in
      // superclass chain

      Type type = method.getGenericReturnType();
      TypeFactory typeFactory = TypeFactory.defaultInstance();
      JavaType[] args = typeFactory.findTypeParameters(
          typeFactory.constructType(type), ExInstWithCause.class);
      if (args == null) {
        throw new IllegalStateException("Unable to find superclass"
            + " ExInstWithCause for " + type);
      }
      // Update this if ExInstWithCause gets more type parameters
      // For instance if B and C are added ExInstWithCause<A, B, C>,
      // code below should be updated
      if (args.length != 1) {
        throw new IllegalStateException("ExInstWithCause should have"
            + " exactly one type parameter");
      }
      return (Class<T>) args[0].getRawClass();
    }

    protected void validateException(Callable<Exception> exSupplier) {
      Throwable cause = null;
      try {
        //noinspection ThrowableResultOfMethodCallIgnored
        final Exception ex = exSupplier.call();
        if (ex == null) {
          cause = new NullPointerException();
        }
      } catch (AssertionError e) {
        cause = e;
      } catch (RuntimeException e) {
        cause = e;
      } catch (Exception e) {
        // This can never happen since exSupplier should be just a ex() call.
        // catch(Exception) is required since Callable#call throws Exception.
        // Just in case we get exception somehow, we will rethrow it as a part
        // of AssertionError below.
        cause = e;
      }
      if (cause != null) {
        AssertionError assertionError = new AssertionError(
            "error instantiating exception for resource '"
                + method.getName() + "'");
        assertionError.initCause(cause);
        throw assertionError;
      }
    }

    @Override public void validate(EnumSet<Validation> validations) {
      super.validate(validations);
      if (validations.contains(Validation.CREATE_EXCEPTION)) {
        validateException(
            new Callable<Exception>() {
              public Exception call() throws Exception {
                return ex(new NullPointerException("test"));
              }
            });
      }
    }
  }

  /** Sub-class of {@link Inst} that can throw an exception without caused
   * by. */
  public static class ExInst<T extends Exception> extends ExInstWithCause<T> {
    public ExInst(String base, Locale locale, Method method, Object... args) {
      super(base, locale, method, args);
    }

    public T ex() {
      return ex(null);
    }

    @Override
    public void validate(EnumSet<Validation> validations) {
      super.validate(validations);
      if (validations.contains(Validation.CREATE_EXCEPTION)) {
        validateException(
            new Callable<Exception>() {
              public Exception call() throws Exception {
                return ex();
              }
            });
      }
    }
  }

  /** Types of validation that can be performed on a resource. */
  public enum Validation {
    /** Checks that each method's resource key corresponds to a resource in the
     * bundle. */
    BUNDLE_HAS_RESOURCE,

    /** Checks that there is at least one resource in the bundle. */
    AT_LEAST_ONE,

    /** Checks that the base message annotation is on every resource. */
    MESSAGE_SPECIFIED,

    /** Checks that every message contains even number of quotes. */
    EVEN_QUOTES,

    /** Checks that the base message matches the message in the bundle. */
    MESSAGE_MATCH,

    /** Checks that it is possible to create an exception. */
    CREATE_EXCEPTION,

    /** Checks that the parameters of the method are consistent with the
     * format elements in the base message. */
    ARGUMENT_MATCH,
  }

  /** The message in the default locale. */
  @Retention(RetentionPolicy.RUNTIME)
  public @interface BaseMessage {
    String value();
  }

  /** The name of the property in the resource file. */
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Resource {
    String value();
  }

  /** Property of a resource. */
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Property {
    String name();
    String value();
  }

  /**
   * <code>ShadowResourceBundle</code> is an abstract base class for
   * {@link ResourceBundle} classes which are backed by a properties file. When
   * the class is created, it loads a properties file with the same name as the
   * class.
   *
   * <p> In the standard scheme (see {@link ResourceBundle}), if
   * you call <code>{@link ResourceBundle#getBundle}("foo.MyResource")</code>,
   * it first looks for a class called <code>foo.MyResource</code>, then
   * looks for a file called <code>foo/MyResource.properties</code>. If it finds
   * the file, it creates a {@link PropertyResourceBundle} and loads the class.
   * The problem is if you want to load the <code>.properties</code> file
   * into a dedicated class; <code>ShadowResourceBundle</code> helps with this
   * case.
   *
   * <p> You should create a class as follows:<blockquote>
   *
   * <pre>package foo;
   *class MyResource extends ShadowResourceBundle {
   *    public MyResource() throws java.io.IOException {
   *    }
   *}</pre>
   *
   * </blockquote>
   *
   * Then when you call
   * {@link ResourceBundle#getBundle ResourceBundle.getBundle("foo.MyResource")},
   * it will find the class before the properties file, but still automatically
   * load the properties file based upon the name of the class.
   */
  public abstract static class ShadowResourceBundle extends ResourceBundle {
    private PropertyResourceBundle bundle;

    /**
     * Creates a <code>ShadowResourceBundle</code>, and reads resources from
     * a <code>.properties</code> file with the same name as the current class.
     * For example, if the class is called <code>foo.MyResource_en_US</code>,
     * reads from <code>foo/MyResource_en_US.properties</code>, then
     * <code>foo/MyResource_en.properties</code>, then
     * <code>foo/MyResource.properties</code>.
     *
     * @throws IOException on error
     */
    protected ShadowResourceBundle() throws IOException {
      super();
      Class clazz = getClass();
      InputStream stream = openPropertiesFile(clazz);
      if (stream == null) {
        throw new IOException("could not open properties file for "
            + getClass());
      }
      MyPropertyResourceBundle previousBundle =
          new MyPropertyResourceBundle(stream);
      bundle = previousBundle;
      stream.close();
      // Now load properties files for parent locales, which we deduce from
      // the names of our super-class, and its super-class.
      while (true) {
        clazz = clazz.getSuperclass();
        if (clazz == null
            || clazz == ShadowResourceBundle.class
            || !ResourceBundle.class.isAssignableFrom(clazz)) {
          break;
        }
        stream = openPropertiesFile(clazz);
        if (stream == null) {
          continue;
        }
        MyPropertyResourceBundle newBundle =
            new MyPropertyResourceBundle(stream);
        stream.close();
        previousBundle.setParentTrojan(newBundle);
        previousBundle = newBundle;
      }
    }

    /**
     * Opens the properties file corresponding to a given class. The code is
     * copied from {@link ResourceBundle}.
     */
    private static InputStream openPropertiesFile(Class clazz) {
      final ClassLoader loader = clazz.getClassLoader();
      final String resName = clazz.getName().replace('.', '/') + ".properties";
      return (InputStream) java.security.AccessController.doPrivileged(
          new java.security.PrivilegedAction() {
            public Object run() {
              if (loader != null) {
                return loader.getResourceAsStream(resName);
              } else {
                return ClassLoader.getSystemResourceAsStream(resName);
              }
            }
          }
      );
    }

    public Enumeration<String> getKeys() {
      return bundle.getKeys();
    }

    protected Object handleGetObject(String key) {
      return bundle.getObject(key);
    }

    /**
     * Returns the instance of the <code>baseName</code> resource bundle
     * for the given locale.
     *
     * <p> This method should be called from a derived class, with the proper
     * casting:<blockquote>
     *
     * <pre>class MyResource extends ShadowResourceBundle {
     *    ...
     *
     *    /&#42;&#42;
     *      &#42; Retrieves the instance of {&#64;link MyResource} appropriate
     *      &#42; to the given locale.
     *      &#42;&#42;/
     *    public static MyResource instance(Locale locale) {
     *       return (MyResource) instance(
     *           MyResource.class.getName(), locale,
     *           ResourceBundle.getBundle(MyResource.class.getName(), locale));
     *    }
     *    ...
     * }</pre></blockquote>
     *
     * @param baseName Base name
     * @param locale Locale
     * @param bundle Resource bundle
     * @return Resource bundle
     */
    protected static ShadowResourceBundle instance(
        String baseName, Locale locale, ResourceBundle bundle) {
      if (bundle instanceof PropertyResourceBundle) {
        throw new ClassCastException(
            "ShadowResourceBundle.instance('" + baseName + "','"
            + locale + "') found "
            + baseName + "_" + locale + ".properties but not "
            + baseName + "_" + locale + ".class");
      }
      return (ShadowResourceBundle) bundle;
    }
  }

  /** Resource bundle based on properties. */
  static class MyPropertyResourceBundle extends PropertyResourceBundle {
    public MyPropertyResourceBundle(InputStream stream) throws IOException {
      super(stream);
    }

    void setParentTrojan(ResourceBundle parent) {
      super.setParent(parent);
    }
  }

  enum BuiltinMethod {
    OBJECT_TO_STRING(Object.class, "toString");

    public final Method method;

    BuiltinMethod(Class clazz, String methodName, Class... argumentTypes) {
      this.method = lookupMethod(clazz, methodName, argumentTypes);
    }

    /**
     * Finds a method of a given name that accepts a given set of arguments.
     * Includes in its search inherited methods and methods with wider argument
     * types.
     *
     * @param clazz Class against which method is invoked
     * @param methodName Name of method
     * @param argumentTypes Types of arguments
     *
     * @return A method with the given name that matches the arguments given
     * @throws RuntimeException if method not found
     */
    public static Method lookupMethod(Class clazz, String methodName,
        Class... argumentTypes) {
      try {
        //noinspection unchecked
        return clazz.getMethod(methodName, argumentTypes);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(
            "while resolving method '" + methodName
                + Arrays.toString(argumentTypes)
                + "' in class " + clazz, e);
      }
    }
  }
}

// End Resources.java
