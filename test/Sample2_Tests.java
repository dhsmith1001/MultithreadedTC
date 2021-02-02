
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

import edu.umd.cs.mtc.MultithreadedTestCase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The same tests as Sample1_Tests, but using MultithreadedTestCase instead of TestCase.
 *
 * This means we can eliminate the junit methods for each test,
 * and just provide a 'runTest()' method, if needed.
 *
 * The inner test case classes must be declared 'static' for junit
 * to detect the @Test annotated method in MultithreadedTestCase.
 */
class Sample2_Tests {

  static class MTCBoundedBufferTest extends MultithreadedTestCase {

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

  static class MTCBoundedBufferDeadlockTest extends MultithreadedTestCase {

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
