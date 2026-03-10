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

    // ── Recording API ──────────────────────────────────────────────────────

    /** Begin a named phase.  Ends the previous phase automatically. */
    public static void begin(String phase) {
        long now = System.currentTimeMillis();
        endCurrent(now);
        currentPhase = phase;
        currentPhaseStart = now;
    }

    /** Explicitly end the current phase. */
    public static void end() {
        endCurrent(System.currentTimeMillis());
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

    /** Total time from JVM start to the last recorded entry. */
    public static long totalMs() {
        List<Entry> snap = getEntries();
        if (snap.isEmpty()) return 0;
        return snap.getLast().endMs() - JVM_START_MS;
    }


    /** Human-readable summary for logs. */
    public static String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Rust-MC Blame Log ===%n".formatted());
        for (Entry e : getEntries()) {
            sb.append("  %-35s %6dms%n".formatted(e.phase(), e.durationMs()));
        }
        sb.append("  %-35s %6dms%n".formatted("TOTAL (JVM start -> ready)", totalMs()));
        return sb.toString();
    }
}