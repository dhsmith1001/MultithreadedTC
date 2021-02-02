
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.MultithreadedTestSuite;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The same tests as Sample2_Tests, but using MultithrfeadedTestSuite
 * to detect non-static inner test classes.
 */
class Sample3_Tests extends MultithreadedTestSuite {

  class MTCBoundedBufferTest extends MultithreadedTestCase {

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

  class MTCBoundedBufferDeadlockTest extends MultithreadedTestCase {

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

    @Override
    protected void runTest() throws Throwable {
      try {
        runOnce();
        fail("Test should throw an IllegalStateException");
      } catch (IllegalStateException deadlockDetected) {}
    }
  }

}
