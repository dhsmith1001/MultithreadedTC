package sanity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import edu.umd.cs.mtc.TestCase;
import edu.umd.cs.mtc.TestFramework;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BasicTests {

  // Test
  class SanityMetronomeOrder extends TestCase {
    String s;

    void setUp() {
      s = "";
    }

    void thread1() {
      waitForTick(1);
      s += "A";

      waitForTick(3);
      s += "C";

      waitForTick(6);
      s += "F";
    }

    void thread2() {
      waitForTick(2);
      s += "B";

      waitForTick(5);
      s += "E";

      waitForTick(8);
      s += "H";
    }

    void thread3() {
      waitForTick(4);
      s += "D";

      waitForTick(7);
      s += "G";

      waitForTick(9);
      s += "I";
    }

    void tearDown() {
      assertEquals(s, "ABCDEFGHI", "Threads were not called in correct order");
    }
  }

  @Test
  void testMetronomeOrder() throws Throwable {
    TestFramework.runOnce( new SanityMetronomeOrder() );
  }

  // Test
  class TUnitTestTestWithNoThreads extends TestCase {
    AtomicInteger v1;

    void setUp() {
      v1 = new AtomicInteger(0);
      assertTrue(v1.compareAndSet(0, 1));
    }

    void tearDown() {
      assertTrue(v1.compareAndSet(1, 2));
    }
  }

  @Test
  void testTestWithNoThreads_tunit() throws Throwable {
    TestFramework.runOnce( new TUnitTestTestWithNoThreads() );
  }

  // Test order called is init, then thread, then finish
  class SanityInitBeforeThreadsBeforeFinish extends TestCase {
    AtomicInteger v1, v2;
    CountDownLatch c;

    void setUp() {
      v1 = new AtomicInteger(0);
      v2 = new AtomicInteger(0);
      assertTrue(v1.compareAndSet(0, 1));
      assertTrue(v2.compareAndSet(0, 1));
      c = new CountDownLatch(2);
    }

    void thread1() throws InterruptedException {
      assertTrue(v1.compareAndSet(1, 2));
      c.countDown();
      c.await();
    }

    void thread2() throws InterruptedException {
      assertTrue(v2.compareAndSet(1, 2));
      c.countDown();
      c.await();
    }

    void tearDown() {
      assertEquals(2, v1.intValue());
      assertEquals(2, v2.intValue());
    }
  }

  @Test
  void testSanityInitBeforeThreadsBeforeFinish() throws Throwable {
    TestFramework.runOnce( new SanityInitBeforeThreadsBeforeFinish() );
  }

  // Test
  class SanityWaitForTickAdvancesWhenTestsAreBlocked extends TestCase {
    CountDownLatch c;

    void setUp() {
      c = new CountDownLatch(3);
    }

    void thread1() throws InterruptedException {
      c.countDown();
      c.await();
    }

    void thread2() throws InterruptedException {
      c.countDown();
      c.await();
    }

    void thread3() {
      waitForTick(1);
      assertEquals(1, c.getCount());
      waitForTick(2); // advances quickly
      assertEquals(1, c.getCount());
      c.countDown();
    }

    void tearDown() {
      assertEquals(0, c.getCount());
    }
  }

  @Test
  void testSanityWaitForTickAdvancesWhenTestsAreBlocked() throws Throwable {
    TestFramework.runOnce( new SanityWaitForTickAdvancesWhenTestsAreBlocked() );
  }

  // Test
  class SanityWaitForTickBlocksThread extends TestCase {
      Thread t;
      public void thread1() {
        t = Thread.currentThread();
        waitForTick(2);
      }

      public void thread2() {
        waitForTick(1);
        assertEquals(Thread.State.WAITING, t.getState());
      }
    }

    public void testSanityWaitForTickBlocksThread() throws Throwable {
      TestFramework.runOnce( new SanityWaitForTickBlocksThread() );
    }

  // Test
  class SanityThreadTerminatesBeforeFinishIsCalled extends TestCase {
    Thread t1, t2;

    void thread1() {
      t1 = Thread.currentThread();
    }

    void thread2() {
      t2 = Thread.currentThread();
    }

    void tearDown() {
      assertEquals(Thread.State.TERMINATED, t1.getState());
      assertEquals(Thread.State.TERMINATED, t2.getState());
    }
  }

  @Test
  void testSanityThreadTerminatesBeforeFinishIsCalled() throws Throwable {
    TestFramework.runOnce( new SanityThreadTerminatesBeforeFinishIsCalled() );
  }

  // Test
  class SanityThreadMethodsInvokedInDifferentThreads extends TestCase {
    Thread t1, t2;

    void thread1() {
      t1 = Thread.currentThread();
      waitForTick(2);
    }

    void thread2() {
      t2 = Thread.currentThread();
      waitForTick(2);
    }

    void thread3() {
      waitForTick(1);
      assertNotSame(t1, t2);
    }
  }

  @Test
  void testSanityThreadMethodsInvokedInDifferentThreads() throws Throwable {
    TestFramework.runOnce( new SanityThreadMethodsInvokedInDifferentThreads() );
  }

  // Test
  class SanityGetThreadReturnsCorrectThread extends TestCase {
    Thread t;

    void thread1() {
      t = Thread.currentThread();
      waitForTick(2);
    }

    void thread2() {
      waitForTick(1);
      assertSame(getThread(1), t);
    }
  }

  @Test
  void testSanityGetThreadReturnsCorrectThread() throws Throwable {
    TestFramework.runOnce( new SanityGetThreadReturnsCorrectThread() );
  }

  // Test
  class SanityGetThreadByNameReturnsCorrectThread extends TestCase {
    Thread t;

    void threadFooey() {
      t = Thread.currentThread();
      waitForTick(2);
    }

    void threadBooey() {
      waitForTick(1);
      assertSame(getThreadByName("threadFooey"), t);
    }
  }

  @Test
  void testSanityGetThreadByNameReturnsCorrectThread() throws Throwable {
    TestFramework.runOnce( new SanityGetThreadByNameReturnsCorrectThread() );
  }

}
