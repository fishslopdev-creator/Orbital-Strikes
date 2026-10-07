package dev.fishslop.orbitalstrikes.strike;

import dev.fishslop.orbitalstrikes.OrbitalStrikes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;

/**
 * Runs strike tasks spread out over several ticks. Tasks never hold a reference to their level, so the weak keys let
 * unloaded levels be collected.
 */
public final class StrikeScheduler {
	private static final Map<ServerLevel, List<Entry>> TASKS = new WeakHashMap<>();

	private StrikeScheduler() {
	}

	public static void schedule(ServerLevel level, int delayTicks, StrikeTask task) {
		TASKS.computeIfAbsent(level, l -> new ArrayList<>()).add(new Entry(delayTicks, task));
	}

	public static void tick(ServerLevel level) {
		List<Entry> entries = TASKS.get(level);
		if (entries == null || entries.isEmpty()) {
			return;
		}

		// Iterate over a copy: tasks may schedule follow-up tasks while running.
		for (Entry entry : new ArrayList<>(entries)) {
			if (entry.delay > 0) {
				entry.delay--;
				continue;
			}

			boolean done;
			try {
				done = entry.task.tick(level);
			} catch (RuntimeException e) {
				OrbitalStrikes.LOGGER.error("Orbital strike task failed, cancelling it", e);
				done = true;
			}

			if (done) {
				entries.remove(entry);
			}
		}
	}

	public static void clear() {
		TASKS.clear();
	}

	private static final class Entry {
		private final StrikeTask task;
		private int delay;

		private Entry(int delay, StrikeTask task) {
			this.delay = delay;
			this.task = task;
		}
	}
}
