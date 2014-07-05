package play.modules.profiler;

/**
 * Simple step instrumentation that can be used to profile Java code in the
 * context of the {@link MiniProfilerFilter}.
 * <p>
 * To start a step, use the {@link #step(String)} method. To finish a step, call
 * the {@code close} method on the {@link Step} object returned.
 * <p>
 * Typically you will do this in a {@code try/finally} block like:
 * 
 * <pre>
 * Step s = MiniProfiler.step(&quot;My Step&quot;);
 * try {
 *     // Do Stuff
 * } finally {
 *     s.close();
 * }
 * </pre>
 * 
 * Steps can be nested (e.g. starting a step while inside another step)
 * <p>
 * Profiling data is stored in a {@link ThreadLocal}.
 */
public class MiniProfiler {

    /** Thread local that contains the profiling data for the current thread */
    private static final ThreadLocal<Root> PROFILER_STEPS = new ThreadLocal<Root>();

    /**
     * Start the profiler.
     */
    public static void start() {
        PROFILER_STEPS.set(new Root());
    }

    /**
     * Stop the profiler.
     * <p>
     * This should be called in a {@code finally} block to avoid leaving data on
     * the thread.
     * 
     * @return The profiling data.
     */
    public static Profile stop() {
        Root result = PROFILER_STEPS.get();
        PROFILER_STEPS.remove();
        return result != null ? result.popData() : null;
    }

    /**
     * Start a profiling step.
     * 
     * @param stepName
     *            The name of the step.
     * @return A {@code Step} object whose {@link Step#close()} method should be
     *         called to finish the step.
     */
    public static Step step(String stepName) {
        return step(stepName, null);
    }

    public static Step step(String stepName, String tag) {
        Root root = PROFILER_STEPS.get();
        if (root != null) {
            Profile data = new Profile(root.nextId(), stepName, tag);
            return new Step(root, data);
        } else {
            return new Step(null, null);
        }
    }
}
