package dev.fishslop.orbitalstrikes.mixin;

import dev.fishslop.orbitalstrikes.mace.MaceItem;
import dev.fishslop.orbitalstrikes.mace.ModEnchantments;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Shadow
	protected abstract void hurtArmor(DamageSource source, float amount);

	/** Breach: vanilla armor maths, but each level takes 15% off how much the armor protects. */
	@Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true)
	private void orbitalStrikes$breach(DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
		if (source.isBypassArmor() || !(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker) {
			return;
		}

		int breach = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.BREACH, attacker.getMainHandItem());
		if (breach <= 0) {
			return;
		}

		LivingEntity self = (LivingEntity) (Object) this;
		this.hurtArmor(source, damage);
		float armor = self.getArmorValue();
		float toughness = (float) self.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
		float protection = Mth.clamp(armor - damage / (2.0F + toughness / 4.0F), armor * 0.2F, 20.0F) / 25.0F;
		protection = Mth.clamp(protection - 0.15F * breach, 0.0F, 1.0F);
		cir.setReturnValue(damage * (1.0F - protection));
	}

	@ModifyVariable(method = "causeFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float orbitalStrikes$windBurstFall(float fallDistance) {
		LivingEntity self = (LivingEntity) (Object) this;
		return self.level.isClientSide ? fallDistance : MaceItem.adjustFallAfterWindBurst(self, fallDistance);
	}
}
