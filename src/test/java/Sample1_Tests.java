
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

import edu.umd.cs.mtc.TestCase;
import edu.umd.cs.mtc.TestFramework;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Sample1_Tests {

  // -- EXAMPLE 1 --

  class MTCBoundedBufferTest extends TestCase {

    ArrayBlockingQueue<Integer> buf;

    void setUp() {
      buf = new ArrayBlockingQueue<>(1);
    }

    void threadPutPut() throws InterruptedException {
      buf.put(42);
      buf.put(17);
      assertTick(1);
    }

    void threadTakeTake() throws InterruptedException {
      waitForTick(1);
      assertEquals(42, buf.take());
      assertEquals(17, buf.take());
    }

    void tearDown() {
      assertTrue(buf.isEmpty());
    }
  }

  @Test
  void testMTCBoundedBuffer() throws Throwable {
    TestFramework.runOnce( new MTCBoundedBufferTest() );
  }

  // -- EXAMPLE 2 --

  /**
   * Can we implement the Bounded Buffer using CountDownLatch?
   * Nope, this causes a deadlock!
   *
   * But MTC can detect deadlocks.
   * So we'll use the CountDownLatch version
   * to demonstrate MTC's deadlock detection capabilities.
   */
  class MTCBoundedBufferDeadlockTest extends TestCase {

    ArrayBlockingQueue<Integer> buf;
    CountDownLatch c;

    void setUp() {
      buf = new ArrayBlockingQueue<>(1);
      c = new CountDownLatch(1);
    }

    void threadPutPut() throws InterruptedException {
      buf.put(42);
      buf.put(17);
      c.countDown();
    }

    void thread2() throws InterruptedException {
      c.await();
      assertEquals(Integer.valueOf(42), buf.take());
      assertEquals(Integer.valueOf(17), buf.take());
    }
  }

  @Test
  void testMTCBoundedBufferDeadlock() throws Throwable {
    try {
      TestFramework.runOnce( new MTCBoundedBufferDeadlockTest() );
      fail("Test should throw an IllegalStateException");
    } catch (IllegalStateException deadlockDetected) {}
  }

}
