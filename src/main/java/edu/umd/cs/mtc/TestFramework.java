package edu.umd.cs.mtc;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * This class provides static methods to perform a {@link TestCase}.
 *
 * The method {@link #runOnce(TestCase)} can be used to run a MultithreadedTestCase once.
 *
 * The method {@link #runManyTimes(TestCase, int)} can be used to run a MultithreadedTestCase multiple times
 * (to see if different interleavings produce different behaviors).
 *
 * <p>
 * Each test case starts by running the initialize method,
 * followed by all the thread methods in different threads,
 * and finally the finish method when all threads have finished.
 *
 * The thread methods are run in a new thread group,
 * and are regulated by a separate clock thread.
 *
 * The clock thread checks periodically to see if all threads are blocked.
 *
 * If all threads are blocked and at least one is waiting for a tick,
 * the clock thread advances the clock to the next desired tick.
 *
 * (A slight delay -- about a clock period -- is applied before advancing
 * the clock to ensure that this is not done prematurely and
 * any threads trying to unblock are given a chance to do so.)
 *
 * <p>
 * The clock thread also detects deadlock (when all threads are blocked,
 * none are waiting for a tick, and none are in state TIMED_WAITING),
 * and can stop a test that is going on too long
 * (a thread is in state RUNNABLE for too long.)
 *
 * <p>
 * Since the test case threads are placed in a new thread group,
 * any other threads created by these test cases will be placed
 * in this thread group by default.
 *
 * All threads in the thread group will be considered by the clock thread
 * when deciding whether to advance the clock, declare a deadlock,
 * or stop a long-running test.
 *
 * <p>
 * The framework catches exceptions thrown in the threads
 * and propagates them to the JUnit test (It also throws AssertionErrors)
 *
 * <p>
 * This class also defines a number of parameters to be used to control the tests.
 *
 * Set command line parameter -Dtunit.runLimit=<em>n</em>
 * to cause a test case to fail if at least one thread stays in a runnable state
 * for more than <em>n</em> seconds without becoming blocked or waiting for a metronome tick.
 *
 * Set command line parameter -Dtunit.clockPeriod=<em>p</em>
 * to cause the clock thread to check the status of all the threads
 * every <em>p</em> milliseconds.
 *
 * @see TestCase
 * @see #runOnce(TestCase)
 * @see #runManyTimes(TestCase, int)
 *
 * @author William Pugh
 * @author Nathaniel Ayewah
 * @since 1.0
 */
public class TestFramework {

  /**
   * Command line key for indicating the regularity (in milliseconds) with which the clock thread regulates the thread methods.
   */
  public static final String CLOCKPERIOD_KEY = "tunit.clockPeriod";

  /**
   * Command line key for indicating the time limit (in seconds) for runnable threads.
   */
  public static final String RUNLIMIT_KEY = "tunit.runLimit";

  /**
   * The default clock period in milliseconds
   */
  public static final Integer DEFAULT_CLOCKPERIOD = 10;

  /**
   * The default run limit in seconds
   */
  public static final Integer DEFAULT_RUNLIMIT = 5;

  /**
   * Change/set the system property for the clock period
   * @param v - the new value for the clock period
   */
  public static void setGlobalClockPeriod(int v) {
    if (v < 0) v = 0;
    System.setProperty(CLOCKPERIOD_KEY, Integer.toString(v));
  }

  /**
   * Change/set the system property for the run limit
   * @param v - the new value for the run limit
   */
  public static void setGlobalRunLimit(int v) {
    if (v < 0) v = 0;
    System.setProperty(RUNLIMIT_KEY, Integer.toString(v));
  }


  /**
   * Run multithreaded test case multiple times
   * using the default or global settings for clock period and run limit.
   *
   * This method adds instrumentation to count the number of times
   * failures occur (an exception is thrown).
   *
   * If the array <code>failureCount</code> is initialized to be of at least size 1,
   * it returns this count in <code>failureCount[0]</code>.
   *
   * If failures do occur, it saves the first failure,
   * and then throws it after running the test <code>count</code> times.
   *
   * @param test - The multithreaded test case to run
   * @param count - the number of times to run the test case
   * @param failureCount - if this array is initialzed to at least size 1, the number of failures is returned in <code>failureCount[0]</code>
   * @throws Throwable - if there is at least one failure -- the first failure is thrown
   */
  public static void runInstrumentedManyTimes(TestCase test, int count, int [] failureCount) throws Throwable {
    var failures = 0;
    var failed = false;
    Throwable t = null;

    System.out.println("Testing " + test.getClass());
    for (var i = 0; i < count; i++) {
      try {
        runOnce(test);
      }
      catch (Throwable e) {
        failed = true;
        failures++;
        if (t == null) {
          t = e;
        }
      }
      if (i%10 == 9) {
        if (failed) {
          System.out.print("f");
          failed = false;
        }
        else {
          System.out.print(".");
        }
        if (i%100 == 99) {
          System.out.println(" " + (i+1));
        }
      }
    }
    if (failureCount != null && failureCount.length > 0) {
      failureCount[0] = failures;
    }
    if (t!=null) {
      throw t;
    }
  }


  /**
   * Run multithreaded test case multiple times using the default or global settings for clock period and run limit.
   */
  public static void runManyTimes(TestCase test, int count) throws Throwable {
    runManyTimes(test, count, -1, -1);
  }

  /**
   * Run multithreaded test case multiple times.
   *
   * The value of this is limited, since even running a test case a thousand or
   * a million times may not expose any bugs dependent upon particular thread interleavings.
   *
   * @param test - The multithreaded test case to run
   * @param count - the number of times to run the test case
   * @param clockPeriod - The period (in ms) between checks for the clock (or null for default or global setting)
   * @param runLimit - The limit to run the test in seconds (or null for default or global setting)
   * @throws Throwable - if any of the test runs fails, the exception is thrown immediately without completing the rest of the test runs.
   */
  public static void runManyTimes(TestCase test, int count, int clockPeriod, int runLimit) throws Throwable {
    for (var i = 0; i < count; i++) {
      runOnce(test, clockPeriod, runLimit);
    }
  }


  /**
   * Run a multithreaded test case once,
   * using the default or global settings for clock period and run limit.
   */
  public static void runOnce(TestCase test) throws Throwable {
    runOnce(test, -1, -1);
  }

  /**
   * Run multithreaded test case once.
   *
   * @param test - The multithreaded test case to run
   * @param clockPeriod - The period (in ms) between checks for the clock (or null for default or global setting)
   * @param runLimit - The limit to run the test in seconds (or null for default or global setting)
   * @throws Throwable - if the test runs fails or causes an exception
   */
  public static void runOnce(TestCase test, int clockPeriod, int runLimit) throws Throwable {

    // prepare run data structures
    var fixture = proxy.get(test.getClass());
    if (fixture.run == null) {
      return; // no thread(method)s to run
    }
    var threads = new LinkedList<Thread>();
    var error = new Throwable[1];

    // choose global setting if parameter is null, or default value if there is no global setting

    if (clockPeriod < 0) {
      clockPeriod = Integer.getInteger(CLOCKPERIOD_KEY, DEFAULT_CLOCKPERIOD);
      if (clockPeriod < 0) clockPeriod = 0;
    }
    if (runLimit < 0) {
      runLimit = Integer.getInteger(RUNLIMIT_KEY, DEFAULT_RUNLIMIT);
      if (runLimit < 0) runLimit = 0;
    }

    // invoke initialize method before each run
    if (fixture.setUp != null) {
      fixture.setUp.invoke(test); // test.initialize()
    }
    test.clock = 0;

    // invoke each thread method in a separate thread and place all threads in a new thread group
    var threadGroup = startMethodThreads(test, fixture, threads, error);

    // start and add clock thread
    threads.add(startClock(test, threadGroup, error, clockPeriod, runLimit));

    // wait until all threads have ended
    waitForMethodThreads(threads, error);

    // invoke finish at the end of each run
    if (fixture.tearDown != null) {
      fixture.tearDown.invoke(test); // test.finish()
    }
  }

  /**
   * Start and return a clock thread
   * which periodically checks all the test case threads and regulates them.
   *
   * <p>
   * If all the threads are blocked and at least one is waiting for a tick,
   * the clock advances to the next tick and the waiting thread is notified.
   *
   * If none of the threads are waiting for a tick or in timed waiting,
   * a deadlock is detected.
   *
   * The clock thread times out if a thread is in runnable or
   * all are blocked and one is in timed waiting for longer than the runLimit.
   *
   * @param test - the test case the clock thread is regulating
   * @param threadGroup - the thread group containing the running thread methods
   * @param error - an array containing any Errors/Exceptions that occur in thread methods or that are thrown by the clock thread
   * @param clockPeriod - The period (in ms) between checks for the clock (or null for default or global setting)
   * @param runLimit - The limit to run the test in seconds (or null for default or global setting)
   * @return The (already started) clock thread
   */
  static Thread startClock(TestCase test, ThreadGroup threadGroup, Throwable[] error, int clockPeriod, int runLimit) {

    // Hold a reference to the current thread.
    Thread mainThread = Thread.currentThread();

    // This thread will be waiting for all the test threads to finish.
    // It should be interrupted if there is an deadlock or timeout in the clock thread.

    var t = new Thread(() -> {
      try {
        ticker(test,threadGroup,mainThread,error,clockPeriod,runLimit);
      }
      catch (Throwable e) {
        // killed
        if (test.getTrace()) {
          System.out.println("Tick thread killed");
        }
      }
    }, "Tick thread");

    t.setDaemon(true);
    t.start();
    return t;
  }

  static void ticker(TestCase test, ThreadGroup threadGroup, Thread mainThread, Throwable[] error, int clockPeriod, int runLimit) throws Throwable {
    var lastProgress = System.currentTimeMillis();
    var deadlocksDetected = 0;
    var readyToTick = 0;

    for (;;) {
      Thread.sleep(clockPeriod);

      // Attempt to get a write lock;
      // this succeeds if clock is not frozen
      if (!test.clockLock.writeLock().tryLock(1000L * runLimit, TimeUnit.MILLISECONDS)) {
        synchronized (test.lock) {
          test.failed = true;
          test.lock.notifyAll();
          if (error[0] == null) {
            error[0] = new IllegalStateException("No progress");
          }
          mainThread.interrupt();
          return;
        }
      }

      synchronized (test.lock) {
        try {

          // Get the contents of the thread group
          var tgCount = threadGroup.activeCount() + 10;
          var ths = new Thread[tgCount];

          tgCount = threadGroup.enumerate(ths, false);
          if (tgCount == 0) return; // all threads are done

          // will set to true to force a check for timeout conditions and restart the loop
          var checkProgress = false;

          // will set true if any thread is in state TIMED_WAITING
          var timedWaiting = false;

          var nextTick = Integer.MAX_VALUE;

          // examine the threads in the thread group; look for next tick
          for (var i = 0; i < tgCount; i++) {
            var t = ths[i];

            try {
              if (test.getTrace()) {
                System.out.println(t.getName() + " is in state " + t.getState());
              }
              if (t.getState() == Thread.State.RUNNABLE) {
                checkProgress = true;
              }
              if (t.getState() == Thread.State.TIMED_WAITING) {
                timedWaiting = true;
              }
            }
            catch (Throwable e) {
              // JVM may not support Thread.State
              checkProgress = false;
              timedWaiting = true;
            }

            var waitingFor = test.threads.get(t);
            if (waitingFor != null && waitingFor > test.clock) {
              nextTick = Math.min(nextTick, waitingFor);
            }
          }

          // If not waiting for anything, but a thread is in TIMED_WAITING,
          // then check progress and loop again
          if (nextTick == Integer.MAX_VALUE && timedWaiting) {
            checkProgress = true;
          }
          // Check for timeout conditions and restart the loop
          if (checkProgress) {
            if (readyToTick > 0) {
              if (test.getTrace()) {
                System.out.println("Was Ready to tick too early");
              }
              readyToTick = 0;
            }
            var now = System.currentTimeMillis();
            if (now - lastProgress > 1000L * runLimit) {
              test.failed = true;
              test.lock.notifyAll();
              if (error[0] == null) {
                error[0] = new IllegalStateException("No progress");
              }
              mainThread.interrupt();
              return;
            }
            deadlocksDetected = 0;
            continue;
          }

          // Detect deadlock
          if (nextTick == Integer.MAX_VALUE) {
            if (readyToTick > 0) {
              if (test.getTrace()) {
                System.out.println("Was Ready to tick too early");
              }
              readyToTick = 0;
            }
            if (++deadlocksDetected < 50) {
              if (deadlocksDetected % 10 == 0 && test.getTrace()) {
                System.out.println("[Detecting deadlock... " + deadlocksDetected + " trys]");
              }
              continue;
            }
            if (test.getTrace()) {
              System.out.println("Deadlock!");
            }
            var sw = new StringWriter();
            var out = new PrintWriter(sw);
            for (var e : test.threads.entrySet()) {
              var t = e.getKey();
              out.println(t.getName() + " " + t.getState());
              for (var st : t.getStackTrace()) {
                out.println("  " + st);
              }
            }
            test.failed = true;
            if (error[0] == null) {
              error[0] = new IllegalStateException("Apparent deadlock\n" + sw.toString());
            }
            mainThread.interrupt();
            return;
          }

          deadlocksDetected = 0;

          if (++readyToTick < 2) {
            continue;
          }
          readyToTick = 0;

          // Advance to next tick
          test.clock = nextTick;
          lastProgress = System.currentTimeMillis();

          // notify any threads that are waiting for this tick
          test.lock.notifyAll();
          if (test.getTrace()) {
            System.out.println("Time is now " + test.clock);
          }
        } finally {
          test.clockLock.writeLock().unlock();
        }
      }

    } // for(;;)
  }

  /**
   * Wait for all of the test case threads to complete,
   * or for one of the threads to throw an exception,
   * or for the clock thread to interrupt this (main) thread of execution.
   *
   * When the clock thread or other threads fail,
   * the error is placed in the shared error array and thrown by this method.
   *
   * @param threads - List of all the test case threads and the clock thread
   * @param error - an array containing any Errors/Exceptions that occur in thread methods or that are thrown by the clock thread
   * @throws Throwable - The first error or exception that is thrown by one of the threads
   */
  static void waitForMethodThreads(List<Thread> threads, Throwable[] error) throws Throwable {
    for (var t : threads) {
      try {
        if (t.isAlive() && error[0] != null) {
          t.interrupt(); // and hope for the best; was t.stop();
        } else {
          t.join();
        }
      }
      catch (InterruptedException e1) {
        if (error[0] != null) {
          throw error[0];
        }
        throw new AssertionError(e1);
      }
    }
    if (error[0] != null) {
      throw error[0];
    }
  }

  /**
   * Invoke each of the thread methods in a seperate thread
   * and place them all in a common (new) thread group.
   *
   * As a side-effect all the threads are placed in the 'threads' List parameter,
   * and any errors detected are placed in the 'error' array parameter.
   *
   * @param test - The test case containing the thread methods
   * @param fixture - Collection of the methods to be invoked
   * @param threads - By the time this method returns, this parameter will contain all the test case threads
   * @param error - By the time this method returns, this parameter will  contains the first error thrown by one of the threads.
   * @return - The thread group for all the newly created test case threads
   */
  static ThreadGroup startMethodThreads(TestCase test, Fixture fixture,  List<Thread> threads, Throwable[] error) {
    var threadGroup = new ThreadGroup("MTC-Threads");
    var latch = new CountDownLatch(fixture.run.length);
    var waitForRegistration = new Semaphore(0);

    for (var i = 0; i < fixture.run.length; i++) {
      var method = fixture.run[i];
      var name = fixture.id[i];
      var t = new Thread(threadGroup, () -> {
        try {
          waitForRegistration.release();
          latch.countDown();
          latch.await();

          // At this point all threads are created and released
          // (in random order?) together to run in parallel

          test.hello();
          method.invoke(test);
        }
        catch (InvocationTargetException e) {
          var cause = e.getCause();
          if (cause instanceof ThreadDeath) {
            return;
          }
          if (error[0] == null) {
            error[0] = cause;
          }
          signalError(threads);
        }
        catch (ThreadDeath ignore) {
          // ignore it
        }
        catch (Throwable e) {
          System.out.println(Thread.currentThread().getName() + " caught " + e.getMessage());
          if (error[0] == null) {
            error[0] = e;
          }
          signalError(threads);
        }
        finally {
          test.goodbye();
        }
      }, name.substring(6) + " thread");

      threads.add(t);

      // add thread to map of method threads, mapped by name
      test.putThread(name, t);

      t.start();
      waitForRegistration.acquireUninterruptibly();
    }
    return threadGroup;
  }

  /**
   * Stop all test case threads and clock thread, except the thread from which this method is called.
   * This method is used when a thread is ready to end in failure and it wants to make sure all the other threads have ended before throwing an exception.
   *
   * @param threads - List of all the test case threads and the clock thread
   */
  static void signalError(List<Thread> threads) {
    var currentThread = Thread.currentThread();
    for (var t : threads) {
      if (t != currentThread) {
        var assertionError = new AssertionError(t.getName() + " killed by " + currentThread.getName());
        assertionError.setStackTrace(t.getStackTrace());
        t.interrupt(); // was t.stop(assertionError);
      }
    }
  }

  static class Fixture {
    MethodHandle setUp, tearDown;
    MethodHandle[] run;
    String[] id;
  }

  static Fixture fixture(Class<?> type) {
    var fixture = new Fixture();
    var run = new ArrayList<MethodHandle>();
    var id = new ArrayList<String>();
    for (var m : type.getDeclaredMethods()) {
      if (m.getParameterCount() != 0) continue;
      if (!m.getReturnType().equals(Void.TYPE)) continue;
      var n = m.getName();
      if (n.startsWith("thread")) {
        id.add(n);
        run.add(methodHandle(m));
      } else if (n.equals("setUp")) {
        fixture.setUp = methodHandle(m);
      } else if (n.equals("tearDown")) {
        fixture.tearDown = methodHandle(m);
      }
    }
    if (!run.isEmpty()) {
      fixture.run = run.toArray(new MethodHandle[run.size()]);
      fixture.id = id.toArray(new String[id.size()]);
    }
    return fixture;
  }

  static final MethodHandles.Lookup lookup = MethodHandles.lookup();

  static MethodHandle methodHandle(Method m) {
    try {
      m.trySetAccessible();
      return lookup.unreflect(m);
    }
    catch (Exception e) {
      System.err.println(e.toString());
      return null;
    }
  }

  static final ClassValue<Fixture> proxy = new ClassValue<>() {
    @Override
    protected Fixture computeValue(Class<?> type) {
      return fixture(type);
    }
  };

}
