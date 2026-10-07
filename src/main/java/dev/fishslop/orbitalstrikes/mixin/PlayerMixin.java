package dev.fishslop.orbitalstrikes.mixin;

import dev.fishslop.orbitalstrikes.mace.MaceItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Player.class)
public abstract class PlayerMixin {
	/** Adds the mace's smash damage to the main hit (after the critical-hit multiplier, like 1.21). */
	@ModifyArg(method = "attack",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
			index = 1)
	private float orbitalStrikes$maceSmashDamage(float damage) {
		Player self = (Player) (Object) this;
		ItemStack weapon = self.getMainHandItem();
		return weapon.getItem() instanceof MaceItem ? damage + MaceItem.smashBonus(self, weapon) : damage;
	}
}
