package dev.fishslop.orbitalstrikes.mixin.client;

import dev.fishslop.orbitalstrikes.item.OrbitalCannonItem;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FishingHookRenderer.class)
public abstract class FishingHookRendererMixin {
	/** Vanilla draws the line from the other hand unless the main hand holds a plain fishing rod. */
	@Redirect(
			method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
	private boolean orbitalStrikes$lineFromCannon(ItemStack stack, Item item) {
		return stack.is(item) || stack.getItem() instanceof OrbitalCannonItem;
	}
}
