package dev.fishslop.orbitalstrikes.mace;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A backport of the 1.21 mace. Hitting something while falling more than 1.5 blocks is a smash attack: it adds damage
 * for every block fallen, cancels your fall damage, and knocks back everything around the target.
 *
 * <p>The bonus damage is added in {@code PlayerMixin}; the effects of the smash happen here in {@link #hurtEnemy}.
 */
public class MaceItem extends Item {
	private static final float SMASH_MIN_FALL = 1.5F;
	private static final float HEAVY_SMASH_FALL = 5.0F;
	private static final double KNOCKBACK_RANGE = 3.5;
	/** Upwards speed given by Wind Burst I, II and III. */
	private static final double[] WIND_BURST_VELOCITY = {1.0, 1.3, 1.6};

	/**
	 * Height each entity was launched from by Wind Burst. Like 1.21, falling back down only hurts for the distance
	 * below that point, not from the top of the launch.
	 */
	private static final Map<UUID, Double> WIND_BURST_LAUNCHES = new HashMap<>();

	private final Multimap<Attribute, AttributeModifier> modifiers;

	public MaceItem(Properties properties) {
		super(properties);
		ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
		// 1.21 values: 6 attack damage, 0.6 attack speed.
		builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 5.0, AttributeModifier.Operation.ADDITION));
		builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -3.4, AttributeModifier.Operation.ADDITION));
		this.modifiers = builder.build();
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
		return slot == EquipmentSlot.MAINHAND ? this.modifiers : super.getDefaultAttributeModifiers(slot);
	}

	@Override
	public int getEnchantmentValue() {
		return 15;
	}

	@Override
	public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
		// Breeze rods don't exist in 1.19.2, so blaze rods stand in for them.
		return repair.is(Items.BLAZE_ROD);
	}

	@Override
	public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
		return !player.isCreative();
	}

	public static boolean canSmash(LivingEntity attacker) {
		return attacker.fallDistance > SMASH_MIN_FALL && !attacker.isFallFlying();
	}

	/** Extra damage for a smash attack, using the 1.21 formula (plus Density). */
	public static float smashBonus(LivingEntity attacker, ItemStack stack) {
		if (!canSmash(attacker)) {
			return 0.0F;
		}

		float fall = attacker.fallDistance;
		float bonus;
		if (fall <= 3.0F) {
			bonus = 4.0F * fall;
		} else if (fall <= 8.0F) {
			bonus = 12.0F + 2.0F * (fall - 3.0F);
		} else {
			bonus = 22.0F + fall - 8.0F;
		}

		int density = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.DENSITY, stack);
		return bonus + density * 0.5F * fall;
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));

		if (attacker.level instanceof ServerLevel level && canSmash(attacker)) {
			this.smash(level, stack, target, attacker);
		}

		return true;
	}

	private void smash(ServerLevel level, ItemStack stack, LivingEntity target, LivingEntity attacker) {
		boolean heavy = attacker.fallDistance > HEAVY_SMASH_FALL;

		SoundEvent sound;
		if (!target.isOnGround()) {
			sound = SoundEvents.PLAYER_ATTACK_KNOCKBACK;
		} else {
			sound = heavy ? SoundEvents.ANVIL_LAND : SoundEvents.ZOMBIE_ATTACK_IRON_DOOR;
		}
		level.playSound(null, target.getX(), target.getY(), target.getZ(), sound, attacker.getSoundSource(), 1.0F, heavy ? 0.5F : 0.8F);

		if (target.isOnGround()) {
			BlockState ground = level.getBlockState(target.getOnPos());
			if (!ground.isAir()) {
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), target.getX(), target.getY() + 0.1, target.getZ(),
						heavy ? 80 : 40, 1.2, 0.1, 1.2, 0.15);
			}
		}

		knockBackNearby(level, attacker, target, heavy);

		// The smash breaks the fall.
		attacker.fallDistance = 0.0F;
		int windBurst = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.WIND_BURST, stack);
		if (windBurst > 0) {
			double velocity = WIND_BURST_VELOCITY[Math.min(windBurst, WIND_BURST_VELOCITY.length) - 1];
			attacker.setDeltaMovement(attacker.getDeltaMovement().with(Direction.Axis.Y, velocity));
			WIND_BURST_LAUNCHES.put(attacker.getUUID(), attacker.getY());
			level.sendParticles(ParticleTypes.CLOUD, attacker.getX(), attacker.getY(), attacker.getZ(), 20, 0.4, 0.1, 0.4, 0.05);
			level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.ENDER_DRAGON_FLAP, attacker.getSoundSource(), 1.0F, 1.6F);
		} else {
			attacker.setDeltaMovement(attacker.getDeltaMovement().with(Direction.Axis.Y, 0.01));
		}
		syncMotion(attacker);
	}

	private static void knockBackNearby(ServerLevel level, LivingEntity attacker, LivingEntity target, boolean heavy) {
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(KNOCKBACK_RANGE),
				entity -> canKnockBack(attacker, target, entity))) {
			Vec3 offset = entity.position().subtract(target.position());
			double distance = offset.length();
			if (distance < 1.0E-4) {
				continue;
			}

			double power = (KNOCKBACK_RANGE - distance) * 0.7 * (heavy ? 2.0 : 1.0)
					* (1.0 - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
			if (power <= 0.0) {
				continue;
			}

			Vec3 push = offset.scale(power / distance);
			entity.push(push.x, 0.7, push.z);
			syncMotion(entity);
		}
	}

	private static boolean canKnockBack(LivingEntity attacker, LivingEntity target, LivingEntity entity) {
		return entity != attacker
				&& entity != target
				&& !entity.isSpectator()
				&& !attacker.isAlliedTo(entity)
				&& !(entity instanceof ArmorStand armorStand && armorStand.isMarker())
				&& !(entity instanceof TamableAnimal tamable && tamable.isOwnedBy(attacker))
				&& entity.distanceToSqr(target) <= KNOCKBACK_RANGE * KNOCKBACK_RANGE;
	}

	/** Players control their own movement, so velocity changes made on the server have to be sent to them. */
	private static void syncMotion(Entity entity) {
		if (entity instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	/** Called when an entity lands; limits the fall damage after a Wind Burst launch to the drop below the launch point. */
	public static float adjustFallAfterWindBurst(LivingEntity entity, float fallDistance) {
		Double launchY = WIND_BURST_LAUNCHES.remove(entity.getUUID());
		if (launchY == null) {
			return fallDistance;
		}
		return (float) Math.min(fallDistance, Math.max(0.0, launchY - entity.getY()));
	}

	public static void clearWindBurstLaunches() {
		WIND_BURST_LAUNCHES.clear();
	}
}
