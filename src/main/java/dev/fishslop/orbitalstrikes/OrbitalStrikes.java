package dev.fishslop.orbitalstrikes;

import dev.fishslop.orbitalstrikes.item.ModItems;
import dev.fishslop.orbitalstrikes.strike.StrikeScheduler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceLocation;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrbitalStrikes implements ModInitializer {
	public static final String MOD_ID = "orbital_strikes";
	public static final Logger LOGGER = LoggerFactory.getLogger("Orbital Strikes");

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}

	@Override
	public void onInitialize(ModContainer mod) {
		ModItems.register();

		ServerTickEvents.END_WORLD_TICK.register(StrikeScheduler::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> StrikeScheduler.clear());
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> OrbitalStrikeCommand.register(dispatcher));

		LOGGER.info("Orbital strike cannons are online.");
	}
}
