package dev.fishslop.orbitalstrikes.mixin;

import dev.fishslop.orbitalstrikes.mace.MaceItem;
import dev.fishslop.orbitalstrikes.mace.ModEnchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
	/** Lets the anvil and /enchant put Smite, Bane of Arthropods and Fire Aspect on a mace. */
	@Inject(method = "canEnchant", at = @At("RETURN"), cancellable = true)
	private void orbitalStrikes$allowOnMace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && stack.getItem() instanceof MaceItem && ModEnchantments.vanillaAllowedOnMace((Enchantment) (Object) this)) {
			cir.setReturnValue(true);
		}
	}
}
