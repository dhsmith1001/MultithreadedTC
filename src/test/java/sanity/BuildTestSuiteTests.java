package sanity;

import java.util.concurrent.atomic.AtomicInteger;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.MultithreadedTestSuite;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Some simple tests with different kinds of inner classes all to be detected.
 */
class BuildTestSuiteTests extends MultithreadedTestSuite {
  
  // 'abstract' classes are skipped by MultithreadedTestSuite#buildTestSuite()
  
  abstract class InnerTestCase extends MultithreadedTestCase {
    AtomicInteger value1, value2;
    
    // set-up and tear-down should be called before each test.
    
    void initialize() {
      value1 = new AtomicInteger(0);
      value2 = new AtomicInteger(0);
    }
    void finish() {
      assertTrue(value1.compareAndSet(3, 4));
      assertTrue(value2.compareAndSet(3, 4));
    }
  }

  // Test
  @SuppressWarnings("unused")
  private class PrivateClass extends InnerTestCase {
      
    void setUp() {
      initialize();
      assertTrue(value1.compareAndSet(0, 1));
      assertTrue(value2.compareAndSet(0, 1));
    }
    void thread1() {
      assertTrue(value1.compareAndSet(1, 2));
    }     
    void thread2() {
      assertTrue(value2.compareAndSet(1, 2));
    }      
    void tearDown() {
      assertTrue(value1.compareAndSet(2, 3));
      assertTrue(value2.compareAndSet(2, 3));
      finish();
    }
  }
    
  // Test
  class PackageClass extends InnerTestCase {
      
    void setUp() {
      initialize();
      assertTrue(value1.compareAndSet(0, 1));
      assertTrue(value2.compareAndSet(0, 1));
    }
    void thread1() {
      assertTrue(value1.compareAndSet(1, 2));
    }     
    void thread2() {
      assertTrue(value2.compareAndSet(1, 2));
    }
    void tearDown() {
      assertTrue(value1.compareAndSet(2, 3));
      assertTrue(value2.compareAndSet(2, 3));
      finish();
    }
  }
  
  // Test
  protected class ProtectedClass extends InnerTestCase {
      
    void setUp() {
      initialize();
      assertTrue(value1.compareAndSet(0, 1));
      assertTrue(value2.compareAndSet(0, 1));
    }

    void thread1() {
      assertTrue(value1.compareAndSet(1, 2));
    }
      
    void thread2() {
      assertTrue(value2.compareAndSet(1, 2));
    }
      
    void tearDown() {
      assertTrue(value1.compareAndSet(2, 3));
      assertTrue(value2.compareAndSet(2, 3));
      finish();
    }
  }

  // Test
  public class PublicClass extends InnerTestCase {
      
    void setUp() {
      initialize();
      assertTrue(value1.compareAndSet(0, 1));
      assertTrue(value2.compareAndSet(0, 1));
    }
    void thread1() {
      assertTrue(value1.compareAndSet(1, 2));
    }
    void thread2() {
      assertTrue(value2.compareAndSet(1, 2));
    }    
    void tearDown() {
      assertTrue(value1.compareAndSet(2, 3));
      assertTrue(value2.compareAndSet(2, 3));
      finish();
    }
  }
  
  // Test
  public class PublicClassPublicConstructor extends InnerTestCase {
    public PublicClassPublicConstructor() {}
    
    void setUp() {
      initialize();
      assertTrue(value1.compareAndSet(0, 1));
      assertTrue(value2.compareAndSet(0, 1));
    }
    void thread1() {
      assertTrue(value1.compareAndSet(1, 2));
    }      
    void thread2() {
      assertTrue(value2.compareAndSet(1, 2));
    }
    void tearDown() {
      assertTrue(value1.compareAndSet(2, 3));
      assertTrue(value2.compareAndSet(2, 3));
      finish();
    }
  }

  // Test
  public class PublicClassPrivateConstructor extends InnerTestCase {
    private PublicClassPrivateConstructor() {}
    
    void setUp() {
      initialize();
      assertTrue(value1.compareAndSet(0, 1));
      assertTrue(value2.compareAndSet(0, 1));
    }
    void thread1() {
      assertTrue(value1.compareAndSet(1, 2));
    }
    void thread2() {
      assertTrue(value2.compareAndSet(1, 2));
    }
    void tearDown() {
      assertTrue(value1.compareAndSet(2, 3));
      assertTrue(value2.compareAndSet(2, 3));
      finish();
    }
  }

  // Test
  @SuppressWarnings("unused")
  private class PrivateClassPrivateConstructor extends InnerTestCase {
    private PrivateClassPrivateConstructor() {}
    
    void setUp() {
      initialize();
      assertTrue(value1.compareAndSet(0, 1));
      assertTrue(value2.compareAndSet(0, 1));
    }
    void thread1() {
      assertTrue(value1.compareAndSet(1, 2));
    }
    void thread2() {
      assertTrue(value2.compareAndSet(1, 2));
    }
    void tearDown() {
      assertTrue(value1.compareAndSet(2, 3));
      assertTrue(value2.compareAndSet(2, 3));
      finish();
    }
  }

  // Test
  @SuppressWarnings("unused")
  private class PrivateClassPublicConstructor extends InnerTestCase {
    public PrivateClassPublicConstructor() {}
    
    void setUp() {
      initialize();
      assertTrue(value1.compareAndSet(0, 1));
      assertTrue(value2.compareAndSet(0, 1));
    }
    void thread1() {
      assertTrue(value1.compareAndSet(1, 2));
    }
    void thread2() {
      assertTrue(value2.compareAndSet(1, 2));
    }
    void tearDown() {
      assertTrue(value1.compareAndSet(2, 3));
      assertTrue(value2.compareAndSet(2, 3));
      finish();
    }
  }

}
