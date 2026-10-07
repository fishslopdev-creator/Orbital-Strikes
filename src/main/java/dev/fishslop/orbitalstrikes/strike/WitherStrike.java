package dev.fishslop.orbitalstrikes.strike;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Several waves of wither skulls rained down in rings, then a Wither is summoned in the middle of the crater (with
 * the usual spawn blast).
 */
public final class WitherStrike implements StrikeTask {
	private static final int WAVES = 5;
	private static final int WAVE_INTERVAL = 12;
	private static final int RINGS_PER_WAVE = 4;
	private static final double RING_SPACING = 3.5;
	private static final double SKULL_SPACING = 3.0;
	private static final double DROP_HEIGHT = 40.0;
	private static final float BLUE_SKULL_CHANCE = 0.35F;
	private static final int WITHER_DELAY = 40;

	private final Vec3 target;
	@Nullable
	private final UUID owner;
	private int age;

	public WitherStrike(Vec3 target, @Nullable UUID owner) {
		this.target = target;
		this.owner = owner;
	}

	@Override
	public boolean tick(ServerLevel level) {
		int lastWaveTick = (WAVES - 1) * WAVE_INTERVAL;

		if (this.age <= lastWaveTick && this.age % WAVE_INTERVAL == 0) {
			this.skullWave(level);
		}

		if (this.age == lastWaveTick + WITHER_DELAY) {
			WitherBoss wither = EntityType.WITHER.create(level);
			if (wither != null) {
				wither.moveTo(this.target.x, this.target.y, this.target.z, level.random.nextFloat() * 360.0F, 0.0F);
				wither.makeInvulnerable();
				level.addFreshEntity(wither);
			}
			return true;
		}

		this.age++;
		return false;
	}

	private void skullWave(ServerLevel level) {
		Player player = Strikes.owner(level, this.owner);
		level.playSound(null, this.target.x, this.target.y, this.target.z, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 10.0F, 0.6F);

		double y = this.target.y + DROP_HEIGHT;
		spawnSkull(level, player, this.target.x, y, this.target.z);
		for (int ring = 1; ring <= RINGS_PER_WAVE; ring++) {
			double radius = ring * RING_SPACING;
			int points = Strikes.pointsOnRing(radius, SKULL_SPACING);
			double offset = level.random.nextDouble() * Math.PI * 2.0;
			for (int i = 0; i < points; i++) {
				double angle = offset + 2.0 * Math.PI * i / points;
				spawnSkull(level, player, this.target.x + Math.cos(angle) * radius, y + level.random.nextDouble() * 6.0,
						this.target.z + Math.sin(angle) * radius);
			}
		}
	}

	private static void spawnSkull(ServerLevel level, @Nullable Player owner, double x, double y, double z) {
		WitherSkull skull = new WitherSkull(EntityType.WITHER_SKULL, level);
		skull.moveTo(x, y, z, 0.0F, 90.0F);
		skull.setOwner(owner);
		skull.setDangerous(level.random.nextFloat() < BLUE_SKULL_CHANCE);
		skull.setDeltaMovement(0.0, -1.5, 0.0);
		skull.xPower = 0.0;
		skull.yPower = -0.1;
		skull.zPower = 0.0;
		level.addFreshEntity(skull);
	}
}
