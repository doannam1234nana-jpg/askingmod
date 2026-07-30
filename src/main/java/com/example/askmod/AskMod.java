package com.example.askmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AskMod implements ModInitializer {
	public static final String MOD_ID = "askmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		AskConfig.load();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				AskCommand.register(dispatcher));

		LOGGER.info("[AskMod] Da khoi tao thanh cong. Dung lenh /ask <cau hoi> de hoi AI.");
	}
}
