package dev.fishslop.orbitalstrikes.strike;

import dev.fishslop.orbitalstrikes.item.OrbitalCannonItem;
import java.util.UUID;
import java.util.function.BiFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public enum StrikeType {
	/** Concentric rings of TNT rained down from the sky. */
	NUKE("nuke", "Nuke", ChatFormatting.RED, 1.0F, 0.25F, 0.1F, NukeStrike::new),
	/** A drill of TNT that punches a shaft straight down to bedrock. */
	STAB("stab", "Stab", ChatFormatting.AQUA, 0.2F, 0.8F, 1.0F, StabStrike::new),
	/** A barrage of wither skulls, followed by a freshly summoned Wither. */
	WITHER("wither", "Wither", ChatFormatting.DARK_GRAY, 0.15F, 0.1F, 0.2F, WitherStrike::new),
	/** A pack of tamed wolves dropped from orbit. */
	DOG("dog", "Dog", ChatFormatting.GOLD, 0.95F, 0.85F, 0.6F, DogStrike::new);

	private final String id;
	private final String renameTrigger;
	private final ChatFormatting formatting;
	private final float red;
	private final float green;
	private final float blue;
	private final BiFunction<Vec3, UUID, StrikeTask> factory;

	StrikeType(String id, String renameTrigger, ChatFormatting formatting, float red, float green, float blue,
			BiFunction<Vec3, UUID, StrikeTask> factory) {
		this.id = id;
		this.renameTrigger = renameTrigger;
		this.formatting = formatting;
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.factory = factory;
	}

	public String id() {
		return this.id;
	}

	public ChatFormatting formatting() {
		return this.formatting;
	}

	public float red() {
		return this.red;
	}

	public float green() {
		return this.green;
	}

	public float blue() {
		return this.blue;
	}

	public Component displayName() {
		return Component.translatable("strike.orbital_strikes." + this.id).withStyle(this.formatting);
	}

	public StrikeTask create(Vec3 target, @Nullable UUID owner) {
		return this.factory.apply(target, owner);
	}

	/**
	 * Works out which strike a rod calls in: either one of this mod's cannons, or (like the original command-block
	 * version) a plain fishing rod renamed in an anvil to "Nuke", "Stab", "Wither" or "Dog".
	 */
	@Nullable
	public static StrikeType fromStack(ItemStack stack) {
		if (stack.getItem() instanceof OrbitalCannonItem cannon) {
			return cannon.strikeType();
		}

		if (stack.is(Items.FISHING_ROD) && stack.hasCustomHoverName()) {
			String name = stack.getHoverName().getString().trim();
			for (StrikeType type : values()) {
				if (type.renameTrigger.equalsIgnoreCase(name)) {
					return type;
				}
			}
		}

		return null;
	}
}
