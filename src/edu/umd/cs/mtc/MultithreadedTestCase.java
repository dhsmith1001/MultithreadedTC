package edu.umd.cs.mtc;

import org.junit.jupiter.api.Test;

/**
 * Extends {@link TestCase} with a basic implementation of a @Test method.
 *
 * <p>
 * When using this class, the default behavior is to run the test once.
 * To change this default behavior, override {@link #runTest()}.
 *
 * For example, to run a test 20 times, override:
 *
 * <pre><code>
 *  protected void runTest() throws Throwable {
 *    TestFramework.runManyTimes(this, 20);
 *  }
 * </code></pre>
 *
 * This class also provides convenience api's to the {@link TestFramework}
 * <code>run{Once,ManyTimes}</code> functions.
 *
 * @see TestFramework#runOnce(TestCase)
 * @see TestFramework#runOnce(TestCase, int, int)
 * @see TestFramework#runManyTimes(TestCase, int)
 * @see TestFramework#runManyTimes(TestCase, int, int, int)
 *
 */
public class MultithreadedTestCase extends TestCase {

  @Test
  void run() throws Throwable {
    runTest();
  }

  /**
   * This is the method that runs this test.
   * By default the test is just run once,
   * To change the way the test is run, simply override this method.
   */
  protected void runTest() throws Throwable {
    runOnce();
  }

  /** <code>TestFramework.runOnce(this)</code> */
  protected void runOnce() throws Throwable {
    TestFramework.runOnce(this);
  }

  /** <code>TestFramework.runOnce(this, clockPeriod, runLimit)</code> */
  protected void runOnce(int clockPeriod, int runLimit) throws Throwable {
    TestFramework.runOnce(this,clockPeriod,runLimit);
  }

  /** <code>TestFramework.runManyTimes(this, count)</code> */
  protected void runManyTimes(int count) throws Throwable {
    TestFramework.runManyTimes(this,count);
  }

  /** <code>TestFramework.runManyTimes(this, count, clockPeriod, runLimit)</code> */
  protected void runManyTimes(int count, int clockPeriod, int runLimit) throws Throwable {
    TestFramework.runManyTimes(this,count,clockPeriod,runLimit);
  }

}
