package dev.fishslop.orbitalstrikes;

import dev.fishslop.orbitalstrikes.strike.StrikeScheduler;
import dev.fishslop.orbitalstrikes.strike.StrikeType;
import dev.fishslop.orbitalstrikes.strike.Strikes;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Only runs on the CI server ({@code -Dorbital_strikes.smokeTest=true}): checks every mixin applies, fires each strike
 * once, then shuts the server down. Prints {@link #PASSED} if everything worked.
 */
final class SmokeTest {
	static final String PASSED = "ORBITAL STRIKES SMOKE TEST PASSED";
	private static final int RUN_TICKS = 400;

	private static int ticks = -1;

	private SmokeTest() {
	}

	static void registerIfEnabled() {
		if (!Boolean.getBoolean("orbital_strikes.smokeTest")) {
			return;
		}

		ServerLifecycleEvents.SERVER_STARTED.register(SmokeTest::start);
		ServerTickEvents.END_SERVER_TICK.register(SmokeTest::tick);
	}

	private static void start(MinecraftServer server) {
		try {
			// Loading the classes applies the mixins, which fail loudly if an injection point is missing.
			for (Class<?> target : List.of(FishingHook.class, Player.class, LivingEntity.class, Enchantment.class, EnchantmentHelper.class)) {
				OrbitalStrikes.LOGGER.info("Smoke test: loaded {}", target.getName());
			}
		} catch (Throwable t) {
			fail("mixins failed to apply", t);
		}

		ServerLevel level = server.overworld();
		BlockPos spawn = level.getSharedSpawnPos();
		StrikeType[] types = StrikeType.values();
		for (int i = 0; i < types.length; i++) {
			BlockPos column = spawn.offset(i * 80, 0, 0);
			level.getChunkAt(column);
			BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
			Strikes.launch(level, Vec3.atBottomCenterOf(ground), null, types[i]);
		}
		ticks = 0;
	}

	private static void tick(MinecraftServer server) {
		if (ticks < 0 || ++ticks < RUN_TICKS) {
			return;
		}
		ticks = -1;

		if (StrikeScheduler.failures() > 0) {
			fail(StrikeScheduler.failures() + " strike task(s) threw an exception", null);
		}

		OrbitalStrikes.LOGGER.info(PASSED);
		server.halt(false);
	}

	private static void fail(String reason, Throwable cause) {
		OrbitalStrikes.LOGGER.error("ORBITAL STRIKES SMOKE TEST FAILED: {}", reason, cause);
		// halt rather than exit: System.exit would wait on the server's shutdown hook, which waits on this thread.
		Runtime.getRuntime().halt(1);
	}
}
