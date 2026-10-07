package dev.fishslop.orbitalstrikes.strike;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Drops concentric rings of TNT from high above the target, one ring per tick working outwards, so the whole area
 * goes up together a few seconds later.
 */
public final class NukeStrike implements StrikeTask {
	private static final int RINGS = 10;
	private static final double RING_SPACING = 3.0;
	private static final double TNT_SPACING = 2.5;
	private static final double DROP_HEIGHT = 60.0;
	private static final double DROP_SPEED = -2.0;
	private static final int CENTER_STACK = 6;
	/** Long enough for TNT to fall {@link #DROP_HEIGHT} blocks and land before going off. */
	private static final int BASE_FUSE = 70;

	private final Vec3 target;
	@Nullable
	private final UUID owner;
	private int ring;

	public NukeStrike(Vec3 target, @Nullable UUID owner) {
		this.target = target;
		this.owner = owner;
	}

	@Override
	public boolean tick(ServerLevel level) {
		Player player = Strikes.owner(level, this.owner);
		double y = this.target.y + DROP_HEIGHT;

		if (this.ring == 0) {
			level.playSound(null, this.target.x, this.target.y, this.target.z, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 16.0F, 0.5F);
			for (int i = 0; i < CENTER_STACK; i++) {
				Strikes.spawnTnt(level, this.target.x, y + i * 1.5, this.target.z, player, BASE_FUSE + level.random.nextInt(10), DROP_SPEED);
			}
		} else {
			double radius = this.ring * RING_SPACING;
			int points = Strikes.pointsOnRing(radius, TNT_SPACING);
			// Rotate each ring a little so the rings don't line up into spokes.
			double offset = level.random.nextDouble() * Math.PI * 2.0;
			for (int i = 0; i < points; i++) {
				double angle = offset + 2.0 * Math.PI * i / points;
				Strikes.spawnTnt(level, this.target.x + Math.cos(angle) * radius, y, this.target.z + Math.sin(angle) * radius,
						player, BASE_FUSE + level.random.nextInt(15), DROP_SPEED);
			}
		}

		return ++this.ring > RINGS;
	}
}
