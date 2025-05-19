package edu.umd.cs.mtc;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static edu.umd.cs.mtc.TestFramework.*;

public abstract class MultithreadedTestSuite {

  @TestFactory
  List<DynamicTest> innerTestClasses() {
    return buildTestSuite(this);
  }

  static List<DynamicTest> buildTestSuite(Object self) {
    var suite = new ArrayList<DynamicTest>();
    for (var ic : self.getClass().getDeclaredClasses()) {
      if (Modifier.isAbstract(ic.getModifiers())) continue;
      if (ic.isMemberClass() && MultithreadedTestCase.class.isAssignableFrom(ic)) {
        var test = executable(self,ic);
        if (test != null) {
          suite.add(test);
        }
      }
    }
    return suite;
  }

  static DynamicTest executable(Object self, Class<?> ic) {
    var runTest = testMethod(ic);
    if (runTest != null) {
      var testCase = newInstance(self,ic);
      if (testCase != null) {
        return DynamicTest.dynamicTest(
          ic.getSimpleName(),
          () -> {
            try { runTest.invoke(testCase); }
            catch (Throwable t) { uncheck(t); }
          });
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  static <T extends Throwable,V> V uncheck(Throwable e) throws T { throw (T)e; }

  static MethodHandle testMethod(Class<?> c) {
    while (!c.equals(Object.class)) {
      for (var m : c.getDeclaredMethods()) {
        if (m.getAnnotation(Test.class) != null) {
          return methodHandle(m);
        }
      }
      c = c.getSuperclass();
    }
    return null;
  }

  static <T> T newInstance(Object o, Class<T> c) {
    try {
      var ctor = c.getDeclaredConstructor(o.getClass());
      ctor.trySetAccessible();
      return ctor.newInstance(o);
    }
    catch (Exception e) {
      System.err.println(e.toString());
      return null;
    }
  }

}
