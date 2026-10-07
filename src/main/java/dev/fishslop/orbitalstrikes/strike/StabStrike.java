package dev.fishslop.orbitalstrikes.strike;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A thin, fast drill: every few ticks it finds the bottom of the shaft and detonates TNT right inside it, punching
 * straight down until it reaches bedrock (or anything else TNT can't break) or the bottom of the world.
 */
public final class StabStrike implements StrikeTask {
	private static final int INTERVAL = 3;
	private static final int TNT_PER_STEP = 3;
	private static final int MAX_STEPS = 200;
	/** Blast resistance from which TNT can no longer break a block (obsidian, bedrock, ...). */
	private static final float UNBREAKABLE_RESISTANCE = 1200.0F;
	/** Give up after this many steps without getting any deeper (e.g. stuck under water). */
	private static final int MAX_STUCK_STEPS = 6;

	private final int x;
	private final int z;
	@Nullable
	private final UUID owner;
	private int cursorY;
	private int lastBottom = Integer.MAX_VALUE;
	private int stuckSteps;
	private int steps;
	private int cooldown;

	public StabStrike(Vec3 target, @Nullable UUID owner) {
		this.x = (int) Math.floor(target.x);
		this.z = (int) Math.floor(target.z);
		this.cursorY = (int) Math.floor(target.y) + 1;
		this.owner = owner;
	}

	@Override
	public boolean tick(ServerLevel level) {
		if (this.cooldown-- > 0) {
			return false;
		}
		this.cooldown = INTERVAL;

		int minY = level.getMinBuildHeight();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(this.x, Math.min(this.cursorY, level.getMaxBuildHeight() - 1), this.z);
		while (pos.getY() > minY && isPassable(level, pos)) {
			pos.move(Direction.DOWN);
		}

		BlockState bottom = level.getBlockState(pos);
		if (pos.getY() <= minY || bottom.getBlock().getExplosionResistance() >= UNBREAKABLE_RESISTANCE) {
			return true;
		}

		if (pos.getY() >= this.lastBottom) {
			if (++this.stuckSteps >= MAX_STUCK_STEPS) {
				return true;
			}
		} else {
			this.stuckSteps = 0;
		}
		this.lastBottom = pos.getY();
		this.cursorY = pos.getY();

		Player player = Strikes.owner(level, this.owner);
		for (int i = 0; i < TNT_PER_STEP; i++) {
			// Inside the bottom block, with the shortest fuse, so the blast eats downwards instead of out of the hole.
			Strikes.spawnTnt(level, this.x + 0.5, pos.getY() + 0.1, this.z + 0.5, player, 1, 0.0);
		}

		if (this.steps % 4 == 0) {
			level.playSound(null, this.x + 0.5, pos.getY(), this.z + 0.5, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 4.0F, 1.5F);
		}

		return ++this.steps >= MAX_STEPS;
	}

	private static boolean isPassable(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.isAir() || state.getCollisionShape(level, pos).isEmpty();
	}
}
