package dev.fishslop.orbitalstrikes.item;

import dev.fishslop.orbitalstrikes.OrbitalStrikes;
import dev.fishslop.orbitalstrikes.mace.MaceItem;
import dev.fishslop.orbitalstrikes.strike.StrikeType;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
	public static final Map<StrikeType, OrbitalCannonItem> CANNONS = new EnumMap<>(StrikeType.class);
	public static final Item MACE = new MaceItem(new Item.Properties().durability(500).rarity(Rarity.EPIC).tab(CreativeModeTab.TAB_COMBAT));
	public static final Item HEAVY_CORE = new Item(new Item.Properties().rarity(Rarity.EPIC).tab(CreativeModeTab.TAB_MATERIALS));

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

		Registry.register(Registry.ITEM, OrbitalStrikes.id("mace"), MACE);
		Registry.register(Registry.ITEM, OrbitalStrikes.id("heavy_core"), HEAVY_CORE);
	}
}
