package dev.fishslop.orbitalstrikes.mace;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.DamageEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * Density, Breach and Wind Burst. They can only go on a mace; {@code EnchantmentHelperMixin} keeps them off other
 * items at the enchanting table.
 */
public class MaceEnchantment extends Enchantment {
	private final int maxLevel;
	private final int baseCost;
	private final int costPerLevel;
	private final int costSpread;
	private final boolean treasure;
	/** Density and Breach can't be combined with each other or with Smite / Bane of Arthropods. */
	private final boolean damageType;

	public MaceEnchantment(Rarity rarity, int maxLevel, int baseCost, int costPerLevel, int costSpread, boolean treasure, boolean damageType) {
		// BREAKABLE so the enchanting table considers the mace at all; non-mace items are filtered out.
		super(rarity, EnchantmentCategory.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
		this.maxLevel = maxLevel;
		this.baseCost = baseCost;
		this.costPerLevel = costPerLevel;
		this.costSpread = costSpread;
		this.treasure = treasure;
		this.damageType = damageType;
	}

	@Override
	public int getMaxLevel() {
		return this.maxLevel;
	}

	@Override
	public int getMinCost(int level) {
		return this.baseCost + (level - 1) * this.costPerLevel;
	}

	@Override
	public int getMaxCost(int level) {
		return this.getMinCost(level) + this.costSpread;
	}

	@Override
	public boolean isTreasureOnly() {
		return this.treasure;
	}

	@Override
	public boolean canEnchant(ItemStack stack) {
		return stack.getItem() instanceof MaceItem;
	}

	@Override
	protected boolean checkCompatibility(Enchantment other) {
		if (!super.checkCompatibility(other)) {
			return false;
		}
		if (!this.damageType) {
			return true;
		}
		boolean otherIsDamage = other instanceof DamageEnchantment damage && damage.type != 0
				|| other instanceof MaceEnchantment mace && mace.damageType;
		return !otherIsDamage;
	}
}
