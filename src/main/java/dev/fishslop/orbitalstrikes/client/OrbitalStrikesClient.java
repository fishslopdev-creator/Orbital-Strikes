package dev.fishslop.orbitalstrikes.client;

import dev.fishslop.orbitalstrikes.item.ModItems;
import dev.fishslop.orbitalstrikes.item.OrbitalCannonItem;
import dev.fishslop.orbitalstrikes.mixin.client.ItemPropertiesAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;

public class OrbitalStrikesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient(ModContainer mod) {
		// Same "cast" model predicate vanilla registers for the fishing rod, so the line disappears while cast.
		for (OrbitalCannonItem cannon : ModItems.CANNONS.values()) {
			ItemPropertiesAccessor.orbitalStrikes$register(cannon, new ResourceLocation("cast"), (stack, level, entity, seed) -> {
				if (!(entity instanceof Player player) || player.fishing == null) {
					return 0.0F;
				}

				boolean mainHand = entity.getMainHandItem() == stack;
				boolean offHand = entity.getOffhandItem() == stack && !(entity.getMainHandItem().getItem() instanceof FishingRodItem);
				return mainHand || offHand ? 1.0F : 0.0F;
			});
		}
	}
}
