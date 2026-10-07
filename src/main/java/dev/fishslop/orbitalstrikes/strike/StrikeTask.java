package dev.fishslop.orbitalstrikes.strike;

import net.minecraft.server.level.ServerLevel;

@FunctionalInterface
public interface StrikeTask {
	/**
	 * Runs once per server tick of the level the task was scheduled in.
	 *
	 * @return {@code true} once the task is finished and can be dropped
	 */
	boolean tick(ServerLevel level);
}
