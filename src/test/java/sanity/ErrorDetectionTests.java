package sanity;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import edu.umd.cs.mtc.TestCase;
import edu.umd.cs.mtc.TestFramework;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ErrorDetectionTests {

  boolean trace = false;

  class TUnitTestDeadlockDetected extends TestCase {
    ReentrantLock lockA;
    ReentrantLock lockB;

    void setUp() {
      lockA = new ReentrantLock();
      lockB = new ReentrantLock();
    }

    void threadA() {
      lockA.lock();
      waitForTick(1);
      lockB.lock();
    }

    void threadB() {
      lockB.lock();
      waitForTick(1);
      lockA.lock();
    }
  }

  @Test
  void testDeadlockDetected() throws Throwable {
    try {
      TestFramework.runOnce( new TUnitTestDeadlockDetected() );
      fail("should throw exception");
    } catch (IllegalStateException success) {
      if (trace) success.printStackTrace();
    }
  }

  // - - - -

  class TUnitTestMissingUnfreeze extends TestCase {

    void thread1() throws InterruptedException {
      freezeClock();
      Thread.sleep(200);
    }

    void thread2() {
      waitForTick(1);
    }
  }

  @Test
  void testMissingUnfreeze_tunit() throws Throwable {
    try {
      // Set test to timeout after 2 seconds
      TestFramework.runOnce( new TUnitTestMissingUnfreeze(), -1, 2 );
      fail("should throw exception");
    } catch (IllegalStateException success) {
      if (trace) success.printStackTrace();
    }
  }

  // - - - -

  class TUnitTestLiveLockTimesOut extends TestCase {
    AtomicInteger ai;

    void setUp() {
      ai = new AtomicInteger(1);
      if (false) ai.compareAndSet(1, 2); // dead-code example
    }

    void thread1() {
      while(!ai.compareAndSet(2, 3)) Thread.yield();
    }

    void thread2() {
      while(!ai.compareAndSet(3, 2)) Thread.yield();
    }

    void tearDown() {
      assertTrue(ai.get() == 2 || ai.get() == 3);
    }
  }

  @Test
  void testLiveLockTimesOut() throws Throwable {
    try {
      // Set test to timeout after 2 seconds
      TestFramework.runOnce( new TUnitTestLiveLockTimesOut(), -1, 2 );
      fail("should throw exception");
    } catch (IllegalStateException success) {
      if (trace) success.printStackTrace();
    }
  }

}
