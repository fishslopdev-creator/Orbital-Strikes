package dev.fishslop.orbitalstrikes.item;

import dev.fishslop.orbitalstrikes.strike.StrikeType;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A fishing rod that calls in an orbital strike wherever its bobber is when you reel it in. The strike itself is
 * triggered from {@code FishingHookMixin}, so the cast/reel behaviour is exactly the vanilla rod's.
 */
public class OrbitalCannonItem extends FishingRodItem {
	private final StrikeType strikeType;

	public OrbitalCannonItem(StrikeType strikeType, Properties properties) {
		super(properties);
		this.strikeType = strikeType;
	}

	public StrikeType strikeType() {
		return this.strikeType;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.orbital_strikes." + this.strikeType.id()).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("tooltip.orbital_strikes.usage").withStyle(ChatFormatting.DARK_GRAY));
	}
}
