package plain_vs_mtc;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.mtc.TestCase;
import edu.umd.cs.mtc.TestFramework;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Timed offer times out if ArrayBlockingQueue is full and elements are not taken
 */
class TimeoutTests {

  /* NOTES
   * - Uses freezeClock to prevent clock from advancing
   * - This also guarantees that interrupt is on second offer
   */

  // Plain Version

  volatile boolean threadFailed;

  void threadShouldThrow() {
    threadFailed = true;
    fail("should throw exception");
  }

  void threadAssertFalse(boolean b) {
    if (b) {
      threadFailed = true;
      assertFalse(b);
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    threadFailed = false;
  }

  @Test
  void testTimedOffer() {

    var q = new ArrayBlockingQueue<Object>(2);

    var t = new Thread(() -> {
      try {
        q.put(new Object());
        q.put(new Object());
        threadAssertFalse(q.offer(new Object(), 25, TimeUnit.MILLISECONDS));
        q.offer(new Object(), 2500, TimeUnit.MILLISECONDS);
        threadShouldThrow();
      } catch (InterruptedException success){}
    });

    try {
      t.start();
      Thread.sleep(50);
      t.interrupt();
      t.join();
    } catch (Exception e) {
      fail("Unexpected exception");
    }
  }

  @AfterEach
  void tearDown() throws Exception {
    assertFalse(threadFailed);
  }

  // MTC Version

  /**
   * In this test, the first offer is allowed to timeout, the second offer is interrupted.
   * Use `freezeClock` to prevent the clock from advancing during the first offer.
   */
  class MTCTimedOffer extends TestCase {

    ArrayBlockingQueue<Object> q;

    void setUp() {
      q = new ArrayBlockingQueue<>(2);
    }

    void thread1() {
      try {
        q.put(new Object());
        q.put(new Object());

        freezeClock();
        assertFalse(q.offer(new Object(), 25, TimeUnit.MILLISECONDS));
        unfreezeClock();

        q.offer(new Object(), 2500, TimeUnit.MILLISECONDS);
        fail("should throw exception");
      } catch (InterruptedException success){ assertTick(1); }
    }

    void thread2() {
      waitForTick(1);
      getThread(1).interrupt();
    }
  }

  @Test
  void testMTCTimedOffer() throws Throwable {
    TestFramework.runOnce( new MTCTimedOffer() );
  }

}
