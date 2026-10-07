package dev.fishslop.orbitalstrikes.mixin;

import dev.fishslop.orbitalstrikes.item.OrbitalCannonItem;
import dev.fishslop.orbitalstrikes.strike.StrikeType;
import dev.fishslop.orbitalstrikes.strike.Strikes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
	@Unique
	private static final int ORBITAL_STRIKES$COOLDOWN = 40;

	@Shadow
	@Nullable
	private Entity hookedIn;

	@Shadow
	@Nullable
	public abstract Player getPlayerOwner();

	/** Reeling in an orbital cannon (or a renamed rod) calls the strike in on the bobber. */
	@Inject(method = "retrieve", at = @At("HEAD"))
	private void orbitalStrikes$callStrike(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
		FishingHook self = (FishingHook) (Object) this;
		if (!(self.level instanceof ServerLevel level)) {
			return;
		}

		Player player = this.getPlayerOwner();
		StrikeType type = StrikeType.fromStack(stack);
		if (player == null || type == null || player.getCooldowns().isOnCooldown(stack.getItem())) {
			return;
		}

		Vec3 target = this.hookedIn != null ? this.hookedIn.position() : self.position();
		Strikes.launch(level, target, player.getUUID(), type);
		player.getCooldowns().addCooldown(stack.getItem(), ORBITAL_STRIKES$COOLDOWN);
		player.displayClientMessage(Component.translatable("message.orbital_strikes.inbound", type.displayName()), true);
	}

	/** Vanilla drops the bobber unless a plain fishing rod is held; accept the cannons as well. */
	@Redirect(method = "shouldStopFishing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
	private boolean orbitalStrikes$keepFishingWithCannon(ItemStack stack, Item item) {
		return stack.is(item) || stack.getItem() instanceof OrbitalCannonItem;
	}
}
