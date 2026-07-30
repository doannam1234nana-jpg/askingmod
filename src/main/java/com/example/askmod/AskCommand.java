package com.example.askmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AskCommand {
	// Chong spam: moi nguoi choi phai doi vai giay giua 2 lan hoi
	private static final Map<UUID, Long> lastUsed = new ConcurrentHashMap<>();

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("ask")
				.then(CommandManager.argument("question", StringArgumentType.greedyString())
						.executes(AskCommand::run))
				// "/ask admin ..." - chi nguoi choi/console co du permission level moi dung duoc.
				// Permission level nay lay tu config (mac dinh 2, tuong duong OP thuong).
				.then(CommandManager.literal("admin")
						.requires(source -> source.hasPermissionLevel(AskConfig.get().adminPermissionLevel))
						.then(CommandManager.literal("provider")
								.then(CommandManager.argument("name", StringArgumentType.word())
										.suggests((ctx, builder) -> CommandSource.suggestMatching(AskConfig.PROVIDERS, builder))
										.executes(AskCommand::setProvider)))
						.then(CommandManager.literal("setkey")
								.then(CommandManager.argument("provider", StringArgumentType.word())
										.suggests((ctx, builder) -> CommandSource.suggestMatching(AskConfig.PROVIDERS, builder))
										.then(CommandManager.argument("key", StringArgumentType.greedyString())
												.executes(AskCommand::setKey))))
						.then(CommandManager.literal("setmodel")
								.then(CommandManager.argument("provider", StringArgumentType.word())
										.suggests((ctx, builder) -> CommandSource.suggestMatching(AskConfig.PROVIDERS, builder))
										.then(CommandManager.argument("model", StringArgumentType.greedyString())
												.executes(AskCommand::setModel))))
						.then(CommandManager.literal("cooldown")
								.then(CommandManager.argument("seconds", IntegerArgumentType.integer(0))
										.executes(AskCommand::setCooldown)))
						.then(CommandManager.literal("permission")
								.then(CommandManager.argument("level", IntegerArgumentType.integer(0, 4))
										.executes(AskCommand::setAdminPermission)))
						.then(CommandManager.literal("reload").executes(AskCommand::reload))
						.then(CommandManager.literal("show").executes(AskCommand::show))));
	}

	// -----------------------------------------------------------------
	// /ask <question>
	// -----------------------------------------------------------------

	private static int run(CommandContext<ServerCommandSource> context) {
		String question = StringArgumentType.getString(context, "question");
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		AskConfig.Data cfg = AskConfig.get();

		if (!cfg.isConfigured()) {
			source.sendError(Text.literal("§c[AskMod] Chua cau hinh API key cho provider '" + cfg.provider
					+ "'! Nho admin dung lenh /ask admin setkey " + cfg.provider + " <key>, "
					+ "hoac sua file config/askmod.json roi /ask admin reload."));
			return 0;
		}

		if (player != null) {
			long now = System.currentTimeMillis();
			long cooldownMs = cfg.cooldownSeconds * 1000L;
			Long last = lastUsed.get(player.getUuid());
			if (last != null && now - last < cooldownMs) {
				long remainSec = (cooldownMs - (now - last)) / 1000 + 1;
				source.sendError(Text.literal("§cVui long doi " + remainSec + " giay truoc khi hoi tiep!"));
				return 0;
			}
			lastUsed.put(player.getUuid(), now);
		}

		source.sendFeedback(() -> Text.literal("§7[AskMod] Dang suy nghi..."), false);

		AIClient.askAsync(question).thenAccept(answer -> {
			source.getServer().execute(() -> {
				String reply = answer == null || answer.isBlank() ? "(AI khong tra ve noi dung)" : answer;
				if (player != null && player.isAlive()) {
					player.sendMessage(Text.literal("§b[AI] §f" + reply), false);
				} else {
					source.sendFeedback(() -> Text.literal("§b[AI] §f" + reply), false);
				}
			});
		}).exceptionally(ex -> {
			source.getServer().execute(() ->
					source.sendError(Text.literal("§c[AskMod] Loi khi goi AI: " + ex.getMessage())));
			AskMod.LOGGER.error("[AskMod] Loi khi goi AI API", ex);
			return null;
		});

		return 1;
	}

	// -----------------------------------------------------------------
	// /ask admin ... (yeu cau permission level, xem AskConfig.adminPermissionLevel)
	// -----------------------------------------------------------------

	private static int setProvider(CommandContext<ServerCommandSource> context) {
		String name = StringArgumentType.getString(context, "name");
		ServerCommandSource source = context.getSource();
		if (!AskConfig.isValidProvider(name)) {
			source.sendError(Text.literal("§c[AskMod] Provider khong hop le. Cac gia tri hop le: "
					+ String.join(", ", AskConfig.PROVIDERS)));
			return 0;
		}
		AskConfig.Data cfg = AskConfig.get();
		cfg.provider = AskConfig.normalizeProvider(name);
		AskConfig.save();
		source.sendFeedback(() -> Text.literal("§a[AskMod] Provider dang dung: " + cfg.provider), true);
		return 1;
	}

	private static int setKey(CommandContext<ServerCommandSource> context) {
		String provider = StringArgumentType.getString(context, "provider");
		String key = StringArgumentType.getString(context, "key");
		ServerCommandSource source = context.getSource();
		if (!AskConfig.isValidProvider(provider)) {
			source.sendError(Text.literal("§c[AskMod] Provider khong hop le. Cac gia tri hop le: "
					+ String.join(", ", AskConfig.PROVIDERS)));
			return 0;
		}
		AskConfig.Data cfg = AskConfig.get();
		cfg.setApiKeyFor(provider, key);
		AskConfig.save();
		// Khong in lai key ra chat/log vi day la thong tin nhay cam.
		source.sendFeedback(() -> Text.literal("§a[AskMod] Da luu API key cho provider '"
				+ AskConfig.normalizeProvider(provider) + "' (an trong chat/log)."), true);
		AskMod.LOGGER.info("[AskMod] API key cho provider '" + AskConfig.normalizeProvider(provider)
				+ "' vua duoc cap nhat qua lenh admin.");
		return 1;
	}

	private static int setModel(CommandContext<ServerCommandSource> context) {
		String provider = StringArgumentType.getString(context, "provider");
		String model = StringArgumentType.getString(context, "model");
		ServerCommandSource source = context.getSource();
		if (!AskConfig.isValidProvider(provider)) {
			source.sendError(Text.literal("§c[AskMod] Provider khong hop le. Cac gia tri hop le: "
					+ String.join(", ", AskConfig.PROVIDERS)));
			return 0;
		}
		AskConfig.Data cfg = AskConfig.get();
		cfg.setModelFor(provider, model);
		AskConfig.save();
		source.sendFeedback(() -> Text.literal("§a[AskMod] Model cua provider '"
				+ AskConfig.normalizeProvider(provider) + "' -> " + model), true);
		return 1;
	}

	private static int setCooldown(CommandContext<ServerCommandSource> context) {
		int seconds = IntegerArgumentType.getInteger(context, "seconds");
		ServerCommandSource source = context.getSource();
		AskConfig.Data cfg = AskConfig.get();
		cfg.cooldownSeconds = seconds;
		AskConfig.save();
		source.sendFeedback(() -> Text.literal("§a[AskMod] Cooldown -> " + seconds + " giay"), true);
		return 1;
	}

	private static int setAdminPermission(CommandContext<ServerCommandSource> context) {
		int level = IntegerArgumentType.getInteger(context, "level");
		ServerCommandSource source = context.getSource();
		AskConfig.Data cfg = AskConfig.get();
		cfg.adminPermissionLevel = level;
		AskConfig.save();
		source.sendFeedback(() -> Text.literal("§a[AskMod] Permission level de dung /ask admin -> " + level), true);
		return 1;
	}

	private static int reload(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		AskConfig.load();
		source.sendFeedback(() -> Text.literal("§a[AskMod] Da doc lai config/askmod.json"), true);
		return 1;
	}

	private static int show(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		AskConfig.Data cfg = AskConfig.get();
		StringBuilder sb = new StringBuilder();
		sb.append("§e--- AskMod config ---\n");
		sb.append("§7Provider dang dung: §f").append(cfg.provider).append("\n");
		sb.append("§7Cooldown: §f").append(cfg.cooldownSeconds).append("s   ");
		sb.append("§7Max tokens: §f").append(cfg.maxTokens).append("\n");
		sb.append("§7Admin permission level: §f").append(cfg.adminPermissionLevel).append("\n");
		for (String p : AskConfig.PROVIDERS) {
			sb.append("§7- ").append(p).append(": key=")
					.append(mask(cfg.getApiKeyFor(p)))
					.append(", model=").append(cfg.getModelFor(p)).append("\n");
		}
		source.sendFeedback(() -> Text.literal(sb.toString()), false);
		return 1;
	}

	private static String mask(String key) {
		if (key == null || key.isBlank()) {
			return "§c(chua dat)";
		}
		if (key.length() <= 8) {
			return "§a****";
		}
		return "§a****" + key.substring(key.length() - 4);
	}
}
