package plain_vs_mtc;

import java.util.concurrent.atomic.AtomicInteger;

import edu.umd.cs.mtc.TestCase;
import edu.umd.cs.mtc.TestFramework;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * compareAndSet in one thread enables another waiting for value to succeed
 */
class CompareAndSetTests {

  /* NOTES
   * - Plain version requires a join before final asserts
   * - MTC version does not need to check if thread is alive
   */

  // Plain Version

  @Test
  void testCompareAndSet() throws InterruptedException {

    var ai = new AtomicInteger(1);

    var t = new Thread(() -> {
      while (!ai.compareAndSet(2, 3)) Thread.yield();
    });
    t.start();

    assertTrue(ai.compareAndSet(1, 2));

    t.join(2500);
    assertFalse(t.isAlive());

    assertEquals(ai.get(), 3);
  }

  // MTC Version

  class MTCCompareAndSet extends TestCase {

    AtomicInteger ai;

    void setUp() {
      ai = new AtomicInteger(1);
    }

    void thread1() {
      while (!ai.compareAndSet(2, 3)) Thread.yield();
    }

    void thread2() {
      assertTrue(ai.compareAndSet(1, 2));
    }

    void tearDown() {
      assertEquals(ai.get(), 3);
    }
  }

  @Test
  void testMTCCompareAndSet() throws Throwable {
    TestFramework.runOnce( new MTCCompareAndSet() );
  }

}
