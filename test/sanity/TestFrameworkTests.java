package sanity;

import edu.umd.cs.mtc.TestCase;
import edu.umd.cs.mtc.TestFramework;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TestFrameworkTests {

  class TUnitTestRunThreeTimes extends TestCase {
    int i = 0;

    void thread1() {
      i++;
    }

    void thread2() {
      waitForTick(1);
      i++;
    }
  }

  @Test
  void testRunThreeTimes() throws Throwable {
    var test = new TUnitTestRunThreeTimes();
    TestFramework.runManyTimes( test, 3 );
    assertEquals(test.i, 6);
  }

}
