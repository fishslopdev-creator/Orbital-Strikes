package dev.fishslop.orbitalstrikes.strike;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Drops waves of wolves in rings around the target. They're tamed to whoever called the strike (if they're online)
 * and get a few seconds of Resistance V so they survive the landing.
 */
public final class DogStrike implements StrikeTask {
	private static final int WAVES = 3;
	private static final int WAVE_INTERVAL = 10;
	private static final int RINGS_PER_WAVE = 3;
	private static final double RING_SPACING = 2.5;
	private static final double DOG_SPACING = 2.0;
	private static final double DROP_HEIGHT = 30.0;
	private static final float PUPPY_CHANCE = 0.2F;
	private static final int LANDING_PROTECTION_TICKS = 200;

	private final Vec3 target;
	@Nullable
	private final UUID owner;
	private int age;

	public DogStrike(Vec3 target, @Nullable UUID owner) {
		this.target = target;
		this.owner = owner;
	}

	@Override
	public boolean tick(ServerLevel level) {
		if (this.age % WAVE_INTERVAL == 0) {
			this.dogWave(level, this.age / WAVE_INTERVAL);
		}

		return ++this.age > (WAVES - 1) * WAVE_INTERVAL;
	}

	private void dogWave(ServerLevel level, int wave) {
		Player player = Strikes.owner(level, this.owner);
		level.playSound(null, this.target.x, this.target.y, this.target.z, SoundEvents.WOLF_HOWL, SoundSource.NEUTRAL, 10.0F, 1.0F);

		// Each wave is offset half a ring so the dogs from different waves don't land on top of each other.
		double y = this.target.y + DROP_HEIGHT + wave * 4.0;
		double waveOffset = wave * RING_SPACING / WAVES;
		for (int ring = 0; ring < RINGS_PER_WAVE; ring++) {
			double radius = 1.0 + waveOffset + ring * RING_SPACING;
			int points = Strikes.pointsOnRing(radius, DOG_SPACING);
			double offset = level.random.nextDouble() * Math.PI * 2.0;
			for (int i = 0; i < points; i++) {
				double angle = offset + 2.0 * Math.PI * i / points;
				spawnDog(level, player, this.target.x + Math.cos(angle) * radius, y, this.target.z + Math.sin(angle) * radius);
			}
		}
	}

	private static void spawnDog(ServerLevel level, @Nullable Player owner, double x, double y, double z) {
		Wolf wolf = EntityType.WOLF.create(level);
		if (wolf == null) {
			return;
		}

		wolf.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
		if (owner != null) {
			wolf.tame(owner);
			wolf.setCollarColor(DyeColor.byId(level.random.nextInt(DyeColor.values().length)));
		}
		if (level.random.nextFloat() < PUPPY_CHANCE) {
			wolf.setBaby(true);
		}
		wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, LANDING_PROTECTION_TICKS, 4));
		level.addFreshEntity(wolf);
	}
}
