package sanity;

import edu.umd.cs.mtc.TestCase;
import edu.umd.cs.mtc.TestFramework;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimingTests {

    class TUnitTestClockDoesNotAdvanceWhenFrozen extends TestCase {
      String s;

      void setUp() {
        s = "A";
      }

      void thread1() throws InterruptedException {
        freezeClock();
        Thread.sleep(200);
        assertEquals(s, "A", "Clock advanced while thread was sleeping");
        unfreezeClock();
      }

      void thread2() {
        waitForTick(1);
        s = "B";
      }

      void tearDown() {
        assertEquals(s, "B");
      }
    }

    @Test
    void testClockDoesNotAdvanceWhenFrozen() throws Throwable {
      TestFramework.runOnce( new TUnitTestClockDoesNotAdvanceWhenFrozen() );
    }

}
