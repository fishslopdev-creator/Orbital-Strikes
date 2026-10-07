package dev.fishslop.orbitalstrikes.item;

import dev.fishslop.orbitalstrikes.OrbitalStrikes;
import dev.fishslop.orbitalstrikes.strike.StrikeType;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
	public static final Map<StrikeType, OrbitalCannonItem> CANNONS = new EnumMap<>(StrikeType.class);

	private ModItems() {
	}

	public static void register() {
		for (StrikeType type : StrikeType.values()) {
			// No durability: the cannons never break.
			OrbitalCannonItem item = new OrbitalCannonItem(type, new Item.Properties()
					.stacksTo(1)
					.rarity(Rarity.EPIC)
					.fireResistant()
					.tab(CreativeModeTab.TAB_COMBAT));
			Registry.register(Registry.ITEM, OrbitalStrikes.id(type.id() + "_cannon"), item);
			CANNONS.put(type, item);
		}
	}
}
