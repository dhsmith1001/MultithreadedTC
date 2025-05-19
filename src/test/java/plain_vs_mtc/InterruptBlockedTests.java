package plain_vs_mtc;

import java.util.concurrent.Semaphore;

import edu.umd.cs.mtc.TestCase;
import edu.umd.cs.mtc.TestFramework;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that a waiting acquire blocks interruptibly
 */
class InterruptBlockedTests {

  /* NOTES
   * - Failures in threads require additional work in setup and teardown
   * - Relies on Thread.sleep to ensure acquire has blocked
   * - Does not ensure that exceptions are definitely caused by interrupt
   * - More verbose
   * - Requires a join at the end
   * - In MTC version, get reference to a thread using getThread(1)
   */

  // Plain Version

  volatile boolean threadFailed;

  void threadShouldThrow() {
    threadFailed = true;
    fail("should throw exception");
  }

  @BeforeEach
  void setUp() throws Exception {
    threadFailed = false;
  }

  @Test
  void testInterruptedAcquire() {

    var s = new Semaphore(0);

    var t = new Thread(() -> {
      try {
        s.acquire();
        threadShouldThrow();
      } catch(InterruptedException success){}
    });
    t.start();

    try {
      Thread.sleep(50);
      t.interrupt();
      t.join();
    } catch(InterruptedException e){
      fail("Unexpected exception");
    }
  }

  @AfterEach
  void tearDown() throws Exception {
    assertFalse(threadFailed);
  }


  // MTC Version

  class MTCInterruptedAcquire extends TestCase {

    Semaphore s;

    void setUp() {
      s = new Semaphore(0);
    }

    void thread1() {
      try {
        s.acquire();
        fail("should throw exception");
      } catch(InterruptedException success){ assertTick(1); }
    }

    void thread2() {
      waitForTick(1);
      getThread(1).interrupt();
    }
  }

  @Test
  void testMTCInterruptedAcquire() throws Throwable {
    TestFramework.runOnce( new MTCInterruptedAcquire() );
  }

}
