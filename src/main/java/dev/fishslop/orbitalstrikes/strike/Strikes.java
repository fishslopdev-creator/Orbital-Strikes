package dev.fishslop.orbitalstrikes.strike;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class Strikes {
	/** Ticks the targeting beam is shown before the payload arrives. */
	public static final int WARMUP_TICKS = 30;

	private Strikes() {
	}

	/**
	 * Marks the target with a beam, then delivers the strike once the warmup is over.
	 *
	 * @param owner the player credited for the strike's kills, if any
	 */
	public static void launch(ServerLevel level, Vec3 target, @Nullable UUID owner, StrikeType type) {
		level.playSound(null, target.x, target.y, target.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 8.0F, 0.6F);
		StrikeScheduler.schedule(level, 0, new TargetingBeam(target, type, WARMUP_TICKS));
		StrikeScheduler.schedule(level, WARMUP_TICKS, type.create(target, owner));
	}

	@Nullable
	public static Player owner(ServerLevel level, @Nullable UUID owner) {
		return owner == null ? null : level.getPlayerByUUID(owner);
	}

	public static void spawnTnt(ServerLevel level, double x, double y, double z, @Nullable LivingEntity owner, int fuse, double velocityY) {
		PrimedTnt tnt = new PrimedTnt(level, x, y, z, owner);
		tnt.setFuse(fuse);
		// The constructor gives the TNT a random sideways kick; strikes should fall straight.
		tnt.setDeltaMovement(0.0, velocityY, 0.0);
		level.addFreshEntity(tnt);
	}

	/** Number of points to place on a ring so neighbours are roughly {@code spacing} blocks apart. */
	public static int pointsOnRing(double radius, double spacing) {
		return Math.max(1, (int) Math.round(2.0 * Math.PI * radius / spacing));
	}
}
