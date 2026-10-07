package dev.fishslop.orbitalstrikes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.fishslop.orbitalstrikes.strike.StrikeType;
import dev.fishslop.orbitalstrikes.strike.Strikes;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /orbitalstrike <nuke|stab|wither|dog> [pos]} for operators. Without a position the strike lands on the block the
 * executor is looking at (up to 256 blocks away).
 */
public final class OrbitalStrikeCommand {
	private static final double LOOK_RANGE = 256.0;

	private OrbitalStrikeCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("orbitalstrike").requires(source -> source.hasPermission(2));

		for (StrikeType type : StrikeType.values()) {
			root.then(Commands.literal(type.id())
					.executes(ctx -> fire(ctx.getSource(), type, lookTarget(ctx.getSource())))
					.then(Commands.argument("pos", Vec3Argument.vec3())
							.executes(ctx -> fire(ctx.getSource(), type, Vec3Argument.getVec3(ctx, "pos")))));
		}

		dispatcher.register(root);
	}

	private static Vec3 lookTarget(CommandSourceStack source) {
		Entity entity = source.getEntity();
		if (entity == null) {
			return source.getPosition();
		}

		Vec3 eye = entity.getEyePosition();
		Vec3 end = eye.add(entity.getLookAngle().scale(LOOK_RANGE));
		HitResult hit = source.getLevel().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, entity));
		return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
	}

	private static int fire(CommandSourceStack source, StrikeType type, Vec3 target) {
		UUID owner = source.getEntity() instanceof Player player ? player.getUUID() : null;
		Strikes.launch(source.getLevel(), target, owner, type);
		source.sendSuccess(Component.translatable("command.orbital_strikes.fired",
				type.displayName(), (int) target.x, (int) target.y, (int) target.z), true);
		return 1;
	}
}
