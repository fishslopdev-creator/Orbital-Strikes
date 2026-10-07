package dev.fishslop.orbitalstrikes.strike;

import com.mojang.math.Vector3f;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** A coloured beam from the sky down to the target, plus a marker ring on the ground. */
public final class TargetingBeam implements StrikeTask {
	private static final int BEAM_HEIGHT = 80;
	private static final double MARKER_RADIUS = 3.0;

	private final Vec3 target;
	private final DustParticleOptions dust;
	private final int duration;
	private int age;

	public TargetingBeam(Vec3 target, StrikeType type, int duration) {
		this.target = target;
		this.dust = new DustParticleOptions(new Vector3f(type.red(), type.green(), type.blue()), 2.5F);
		this.duration = duration;
	}

	@Override
	public boolean tick(ServerLevel level) {
		if (this.age % 2 == 0) {
			for (double dy = 0.0; dy < BEAM_HEIGHT; dy += 1.0) {
				level.sendParticles(this.dust, this.target.x, this.target.y + dy, this.target.z, 1, 0.05, 0.25, 0.05, 0.0);
			}

			// The marker ring shrinks onto the target as the strike gets closer.
			double radius = MARKER_RADIUS * (1.0 - (double) this.age / this.duration) + 0.5;
			int points = Strikes.pointsOnRing(radius, 0.5);
			for (int i = 0; i < points; i++) {
				double angle = 2.0 * Math.PI * i / points;
				level.sendParticles(this.dust, this.target.x + Math.cos(angle) * radius, this.target.y + 0.2,
						this.target.z + Math.sin(angle) * radius, 1, 0.0, 0.0, 0.0, 0.0);
			}
		}

		if (this.age == this.duration - 1) {
			level.sendParticles(ParticleTypes.FLASH, this.target.x, this.target.y + 1.0, this.target.z, 1, 0.0, 0.0, 0.0, 0.0);
		}

		return ++this.age >= this.duration;
	}
}
