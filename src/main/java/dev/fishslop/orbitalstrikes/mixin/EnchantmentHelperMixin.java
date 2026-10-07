package dev.fishslop.orbitalstrikes.mixin;

import dev.fishslop.orbitalstrikes.mace.ModEnchantments;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
	@Inject(method = "getAvailableEnchantmentResults", at = @At("RETURN"))
	private static void orbitalStrikes$maceTableOptions(int power, ItemStack stack, boolean allowTreasure,
			CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
		ModEnchantments.adjustTableOptions(power, stack, allowTreasure, cir.getReturnValue());
	}
}
