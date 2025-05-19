package plain_vs_mtc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import edu.umd.cs.mtc.TestCase;
import edu.umd.cs.mtc.TestFramework;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Controlling the order in which threads are called
 */
public class ThreadControlTests {

  // MTC Version

  class MTCThreadOrdering extends TestCase {

    AtomicInteger ai;

    void setUp() {
      ai = new AtomicInteger(0);
    }

    void thread1() {
      assertTrue(ai.compareAndSet(0, 1)); // S1
      waitForTick(3);
      assertEquals(ai.get(), 3); // S4
    }

    void thread2() {
      waitForTick(1);
      assertTrue(ai.compareAndSet(1, 2)); // S2
      waitForTick(3);
      assertEquals(ai.get(), 3); // S4
    }

    void thread3() {
      waitForTick(2);
      assertTrue(ai.compareAndSet(2, 3)); // S3
    }
  }

  @Test
  void testMTCThreadOrdering() throws Throwable {
    TestFramework.runOnce( new MTCThreadOrdering() );
  }

  // CountDown Latch version

  volatile boolean threadFailed;

  @BeforeEach
  void setUp() throws Exception {
    threadFailed = false;
  }

  @AfterEach
  void tearDown() throws Exception {
    assertFalse(threadFailed);
  }

  void unexpectedException() {
    threadFailed = true;
    fail("Unexpected exception");
  }

  @Test
  void testLatchBasedThreadOrdering() throws InterruptedException {

    var c1 = new CountDownLatch(1);
    var c2 = new CountDownLatch(1);
    var c3 = new CountDownLatch(1);
    var ai = new AtomicInteger(0);

    var t1 = new Thread(() -> {
      try {
        assertTrue(ai.compareAndSet(0, 1)); // S1
        c1.countDown();
        c3.await();
        assertEquals(ai.get(), 3); // S4
      } catch (Exception e) {
        // Can't simply catch InterruptedException because we might miss some RuntimeException
        unexpectedException();
      }
    });

    var t2 = new Thread(() -> {
      try {
        c1.await();
        assertTrue(ai.compareAndSet(1, 2)); // S2
        c2.countDown();
        c3.await();
        assertEquals(ai.get(), 3); // S4
      } catch (Exception e) {
        unexpectedException();
      }
    });

    t1.start();
    t2.start();

    c2.await();
    assertTrue(ai.compareAndSet(2, 3)); // S3
    c3.countDown();

    t1.join();
    t2.join();
  }

}
