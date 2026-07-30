package com.example.askmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Quan ly file cau hinh tai config/askmod.json.
 *
 * Ho tro nhieu provider AI cung luc: moi provider co apiKey + model rieng,
 * "provider" quyet dinh provider nao dang duoc dung khi goi lenh /ask.
 * Admin co the doi cac gia tri nay bang lenh /ask admin (xem AskCommand),
 * hoac sua truc tiep file JSON roi dung "/ask admin reload".
 */
public class AskConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Path configPath;
	private static Data data;

	/** Cac provider duoc ho tro. */
	public static final String[] PROVIDERS = {"anthropic", "openai", "gemini", "openrouter"};

	public static class Data {
		/** Provider dang active: "anthropic", "openai", "gemini" hoac "openrouter" */
		public String provider = "anthropic";

		// --- Anthropic (Claude) ---
		/** Lay tai https://console.anthropic.com */
		public String anthropicApiKey = "";
		public String anthropicModel = "claude-3-5-haiku-20241022";

		// --- OpenAI (ChatGPT) ---
		/** Lay tai https://platform.openai.com */
		public String openaiApiKey = "";
		public String openaiModel = "gpt-4o-mini";

		// --- Google Gemini ---
		/** Lay tai https://aistudio.google.com/apikey */
		public String geminiApiKey = "";
		public String geminiModel = "gemini-2.0-flash";

		// --- OpenRouter (mot key, goi duoc nhieu model: Claude, GPT, Gemini, Llama,...) ---
		/** Lay tai https://openrouter.ai/keys */
		public String openrouterApiKey = "";
		/** Vi du: "anthropic/claude-3.5-haiku", "openai/gpt-4o-mini", "google/gemini-2.0-flash-exp" */
		public String openrouterModel = "anthropic/claude-3.5-haiku";

		/** Prompt he thong dinh huong cach AI tra loi */
		public String systemPrompt = "Ban la mot tro ly AI than thien trong game Minecraft. "
				+ "Tra loi ngan gon (toi da 3-4 cau), de hieu, va bang cung ngon ngu voi cau hoi cua nguoi choi.";
		/** So token toi da cho cau tra loi */
		public int maxTokens = 300;
		/** Thoi gian cho toi thieu giua 2 lan hoi cua 1 nguoi choi, tinh bang giay */
		public int cooldownSeconds = 8;

		/**
		 * Permission level (0-4, kieu vanilla Minecraft OP) can co de dung cac lenh
		 * "/ask admin ...". Mac dinh 2 = tuong duong quyen OP co ban.
		 * Chi anh huong tren server / integrated server (chu nhan single player
		 * mac dinh da co permission level 4 nen luon dung duoc).
		 */
		public int adminPermissionLevel = 2;

		public boolean isConfigured() {
			String key = getActiveApiKey();
			return key != null && !key.isBlank();
		}

		public String getActiveModel() {
			return switch (normalizeProvider(provider)) {
				case "openai" -> openaiModel;
				case "gemini" -> geminiModel;
				case "openrouter" -> openrouterModel;
				default -> anthropicModel;
			};
		}

		public String getActiveApiKey() {
			return switch (normalizeProvider(provider)) {
				case "openai" -> openaiApiKey;
				case "gemini" -> geminiApiKey;
				case "openrouter" -> openrouterApiKey;
				default -> anthropicApiKey;
			};
		}

		public String getApiKeyFor(String p) {
			return switch (normalizeProvider(p)) {
				case "openai" -> openaiApiKey;
				case "gemini" -> geminiApiKey;
				case "openrouter" -> openrouterApiKey;
				default -> anthropicApiKey;
			};
		}

		public String getModelFor(String p) {
			return switch (normalizeProvider(p)) {
				case "openai" -> openaiModel;
				case "gemini" -> geminiModel;
				case "openrouter" -> openrouterModel;
				default -> anthropicModel;
			};
		}

		public void setApiKeyFor(String p, String key) {
			switch (normalizeProvider(p)) {
				case "openai" -> openaiApiKey = key;
				case "gemini" -> geminiApiKey = key;
				case "openrouter" -> openrouterApiKey = key;
				default -> anthropicApiKey = key;
			}
		}

		public void setModelFor(String p, String model) {
			switch (normalizeProvider(p)) {
				case "openai" -> openaiModel = model;
				case "gemini" -> geminiModel = model;
				case "openrouter" -> openrouterModel = model;
				default -> anthropicModel = model;
			}
		}
	}

	/** Chuan hoa ten provider nguoi dung nhap ("chatgpt" -> "openai", v.v.) */
	public static String normalizeProvider(String p) {
		if (p == null) return "anthropic";
		String v = p.trim().toLowerCase();
		return switch (v) {
			case "chatgpt", "gpt", "openai" -> "openai";
			case "claude", "anthropic" -> "anthropic";
			case "gemini", "google" -> "gemini";
			case "openrouter", "router" -> "openrouter";
			default -> v;
		};
	}

	public static boolean isValidProvider(String p) {
		String v = normalizeProvider(p);
		for (String known : PROVIDERS) {
			if (known.equals(v)) return true;
		}
		return false;
	}

	public static void load() {
		configPath = FabricLoader.getInstance().getConfigDir().resolve("askmod.json");
		try {
			if (Files.exists(configPath)) {
				String json = Files.readString(configPath);
				data = GSON.fromJson(json, Data.class);
				if (data == null) {
					data = new Data();
				}
				migrateLegacyFields(json, data);
			} else {
				data = new Data();
				save();
				AskMod.LOGGER.info("[AskMod] Da tao file cau hinh moi tai: " + configPath
						+ " - hay dien apiKey vao roi khoi dong lai server (hoac dung /ask admin setkey).");
			}
		} catch (IOException e) {
			AskMod.LOGGER.error("[AskMod] Khong the doc file cau hinh, dung gia tri mac dinh", e);
			data = new Data();
		}
	}

	/**
	 * Cac ban cu cua mod chi co 1 cap "apiKey" + "model" o cap goc.
	 * Neu phat hien file cau hinh cu, tu dong chuyen no vao dung
	 * provider tuong ung de nguoi dung khong bi mat cau hinh khi cap nhat mod.
	 */
	private static void migrateLegacyFields(String rawJson, Data target) {
		try {
			JsonObject root = JsonParser.parseString(rawJson).getAsJsonObject();
			if (!root.has("apiKey")) {
				return;
			}
			String legacyKey = root.get("apiKey").getAsString();
			String legacyModel = root.has("model") ? root.get("model").getAsString() : null;
			if (legacyKey == null || legacyKey.isBlank()) {
				return;
			}
			String provider = normalizeProvider(target.provider);
			if (target.getApiKeyFor(provider) == null || target.getApiKeyFor(provider).isBlank()) {
				target.setApiKeyFor(provider, legacyKey);
				if (legacyModel != null && !legacyModel.isBlank()) {
					target.setModelFor(provider, legacyModel);
				}
				AskMod.LOGGER.info("[AskMod] Da tu dong chuyen cau hinh cu (apiKey/model) sang provider '"
						+ provider + "'.");
				save();
			}
		} catch (Exception e) {
			// File cau hinh khong theo dinh dang cu, bo qua.
		}
	}

	public static void save() {
		try {
			Files.createDirectories(configPath.getParent());
			Files.writeString(configPath, GSON.toJson(data));
		} catch (IOException e) {
			AskMod.LOGGER.error("[AskMod] Khong the ghi file cau hinh", e);
		}
	}

	public static Data get() {
		if (data == null) {
			load();
		}
		return data;
	}
}
