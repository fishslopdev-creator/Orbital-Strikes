package dev.fishslop.orbitalstrikes.mace;

import dev.fishslop.orbitalstrikes.OrbitalStrikes;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;

public final class ModEnchantments {
	/** +0.5 smash damage per block fallen, per level. */
	public static final Enchantment DENSITY = new MaceEnchantment(Enchantment.Rarity.UNCOMMON, 5, 5, 8, 20, false, true);
	/** Each level makes the target's armor 15% less effective. */
	public static final Enchantment BREACH = new MaceEnchantment(Enchantment.Rarity.RARE, 4, 15, 9, 50, false, true);
	/** A smash attack launches you back up into the air. */
	public static final Enchantment WIND_BURST = new MaceEnchantment(Enchantment.Rarity.RARE, 3, 15, 9, 50, true, false);

	private ModEnchantments() {
	}

	public static void register() {
		Registry.register(Registry.ENCHANTMENT, OrbitalStrikes.id("density"), DENSITY);
		Registry.register(Registry.ENCHANTMENT, OrbitalStrikes.id("breach"), BREACH);
		Registry.register(Registry.ENCHANTMENT, OrbitalStrikes.id("wind_burst"), WIND_BURST);
	}

	/** Vanilla enchantments that 1.21 also allows on the mace (on top of Unbreaking and Mending, which already fit). */
	public static boolean vanillaAllowedOnMace(Enchantment enchantment) {
		return enchantment == Enchantments.SMITE || enchantment == Enchantments.BANE_OF_ARTHROPODS || enchantment == Enchantments.FIRE_ASPECT;
	}

	/**
	 * Adjusts the enchanting table's options: the mace-only enchantments are removed from other items, and the mace
	 * also gets them plus Smite, Bane of Arthropods and Fire Aspect.
	 */
	public static void adjustTableOptions(int power, ItemStack stack, boolean allowTreasure, List<EnchantmentInstance> options) {
		if (stack.is(Items.BOOK)) {
			return;
		}

		boolean mace = stack.getItem() instanceof MaceItem;
		if (!mace) {
			options.removeIf(option -> option.enchantment instanceof MaceEnchantment);
			return;
		}

		for (Enchantment enchantment : List.of(DENSITY, BREACH, WIND_BURST, Enchantments.SMITE, Enchantments.BANE_OF_ARTHROPODS, Enchantments.FIRE_ASPECT)) {
			if (options.stream().anyMatch(option -> option.enchantment == enchantment)
					|| enchantment.isTreasureOnly() && !allowTreasure
					|| !enchantment.isDiscoverable()) {
				continue;
			}

			for (int level = enchantment.getMaxLevel(); level >= enchantment.getMinLevel(); level--) {
				if (power >= enchantment.getMinCost(level) && power <= enchantment.getMaxCost(level)) {
					options.add(new EnchantmentInstance(enchantment, level));
					break;
				}
			}
		}
	}
}
