package com.alexxiconify.rustmc.util;

import com.alexxiconify.rustmc.RustMC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Records wall-clock timestamps for every significant loading phase from JVM start
 * through game-ready.  Used to build the "Blame Chart" in ModMenu's Developer tab.
 * <p>
 * All times are stored as absolute {@code System.currentTimeMillis()} so delta
 * calculations are trivial.  Thread-safe via copy-on-write snapshot.
 */
public final class BlameLog {

    private BlameLog() {}

    public record Entry(String phase, long startMs, long endMs) {
        /** Duration in milliseconds. */
        public long durationMs() { return endMs - startMs; }
    }

    private static final List<Entry> entries = Collections.synchronizedList(new ArrayList<>());
    private static final long JVM_START_MS = System.currentTimeMillis(); // captured at class-load time
    private static volatile long currentPhaseStart = 0;
    private static volatile String currentPhase = null;
    /** Frozen when the final phase ends — prevents idle time from inflating startup totals. */
    private static volatile long gameReadyMs = 0;

    private static final String BLAME_LINE_FORMAT = "  %-35s %6dms%n";

    // ── Recording API ──────────────────────────────────────────────────────

    /** Begin a named phase.  Ends the previous phase automatically. */
    public static void begin(String phase) {
        long now = System.currentTimeMillis();
        endCurrent(now);
        currentPhase = phase;
        currentPhaseStart = now;
    }

    /** Explicitly end the current phase and freeze the wall clock. */
    public static void end() {
        long now = System.currentTimeMillis();
        endCurrent(now);
        // Freeze — subsequent wallClockMs() calls return this instead of "now"
        gameReadyMs = now;
    }


    private static void endCurrent(long now) {
        if (currentPhase != null && currentPhaseStart > 0) {
            entries.add(new Entry(currentPhase, currentPhaseStart, now));
            RustMC.LOGGER.debug("[Blame] {} took {}ms", currentPhase, now - currentPhaseStart);
            currentPhase = null;
            currentPhaseStart = 0;
        }
    }

    // ── Query API ──────────────────────────────────────────────────────────

    /** Returns a snapshot of all recorded entries. */
    public static List<Entry> getEntries() {
        endCurrent(System.currentTimeMillis()); // flush in-progress phase
        return List.copyOf(entries);
    }

    /** Sum of all tracked phase durations. */
    public static long trackedMs() {
        long sum = 0;
        for (Entry e : getEntries()) sum += e.durationMs();
        return sum;
    }

    /**
     * Wall clock time from JVM start to game ready.
     * Returns frozen timestamp if game is ready, otherwise returns live time.
     * This prevents idle time at the title screen from inflating startup totals.
     */
    public static long wallClockMs() {
        long end = gameReadyMs > 0 ? gameReadyMs : System.currentTimeMillis();
        return end - JVM_START_MS;
    }


    /**
     * Returns entries with gap entries inserted for any untracked time between phases.
     * Useful for the blame chart to show WHERE time is being lost.
     */
    public static List<Entry> getEntriesWithGaps() {
        List<Entry> snap = getEntries();
        if (snap.isEmpty()) return snap;

        List<Entry> result = new ArrayList<>();
        long prevEnd = JVM_START_MS;

        for (Entry e : snap) {
            long gap = e.startMs() - prevEnd;
            if (gap > 200) { // Only show gaps > 200ms
                result.add(new Entry("⚠ Untracked Gap", prevEnd, e.startMs()));
            }
            result.add(e);
            prevEnd = e.endMs();
        }


        return result;
    }


    /** Human-readable summary for logs. */
    public static String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Rust-MC Blame Log ===%n".formatted());
        long tracked = 0;
        for (Entry e : getEntries()) {
            sb.append(BLAME_LINE_FORMAT.formatted(e.phase(), e.durationMs()));
            tracked += e.durationMs();
        }
        long wall = wallClockMs();
        long untracked = wall - tracked;
        sb.append(BLAME_LINE_FORMAT.formatted("TRACKED TOTAL", tracked));
        if (untracked > 500) {
            sb.append(BLAME_LINE_FORMAT.formatted("UNTRACKED (gaps)", untracked));
        }
        sb.append(BLAME_LINE_FORMAT.formatted("WALL CLOCK (JVM → game ready)", wall));
        return sb.toString();
    }
}