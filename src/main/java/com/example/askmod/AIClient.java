package com.example.askmod;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Goi API cua Anthropic (Claude), OpenAI (ChatGPT), Google (Gemini) hoac
 * OpenRouter de lay cau tra loi. Provider duoc chon trong config/askmod.json
 * ("provider": "anthropic" | "openai" | "gemini" | "openrouter"), hoac qua
 * lenh admin "/ask admin provider <ten>".
 */
public class AIClient {
	private static final HttpClient CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(15))
			.build();

	public static CompletableFuture<String> askAsync(String question) {
		AskConfig.Data cfg = AskConfig.get();
		String provider = AskConfig.normalizeProvider(cfg.provider);

		HttpRequest request = switch (provider) {
			case "openai" -> buildOpenAiStyleRequest(
					"https://api.openai.com/v1/chat/completions",
					cfg.openaiApiKey, cfg.openaiModel, cfg.systemPrompt, cfg.maxTokens, question, null);
			case "gemini" -> buildGeminiRequest(cfg, question);
			case "openrouter" -> buildOpenAiStyleRequest(
					"https://openrouter.ai/api/v1/chat/completions",
					cfg.openrouterApiKey, cfg.openrouterModel, cfg.systemPrompt, cfg.maxTokens, question,
					r -> r.header("HTTP-Referer", "https://askmod.local")
							.header("X-Title", "AskMod Minecraft"));
			default -> buildAnthropicRequest(cfg, question);
		};

		return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(response -> {
					if (response.statusCode() < 200 || response.statusCode() >= 300) {
						throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
					}
					return switch (provider) {
						case "openai", "openrouter" -> parseOpenAiStyle(response.body());
						case "gemini" -> parseGemini(response.body());
						default -> parseAnthropic(response.body());
					};
				});
	}

	// ---------------------------------------------------------------------
	// Anthropic (Claude)
	// ---------------------------------------------------------------------

	private static HttpRequest buildAnthropicRequest(AskConfig.Data cfg, String question) {
		JsonObject body = new JsonObject();
		body.addProperty("model", cfg.anthropicModel);
		body.addProperty("max_tokens", cfg.maxTokens);
		body.addProperty("system", cfg.systemPrompt);

		JsonArray messages = new JsonArray();
		JsonObject userMsg = new JsonObject();
		userMsg.addProperty("role", "user");
		userMsg.addProperty("content", question);
		messages.add(userMsg);
		body.add("messages", messages);

		return HttpRequest.newBuilder()
				.uri(URI.create("https://api.anthropic.com/v1/messages"))
				.timeout(Duration.ofSeconds(30))
				.header("Content-Type", "application/json")
				.header("x-api-key", cfg.anthropicApiKey)
				.header("anthropic-version", "2023-06-01")
				.POST(HttpRequest.BodyPublishers.ofString(body.toString()))
				.build();
	}

	private static String parseAnthropic(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		JsonArray content = root.getAsJsonArray("content");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < content.size(); i++) {
			JsonObject block = content.get(i).getAsJsonObject();
			if (block.has("text")) {
				sb.append(block.get("text").getAsString());
			}
		}
		return sb.toString().trim();
	}

	// ---------------------------------------------------------------------
	// OpenAI (ChatGPT) va OpenRouter dung chung dinh dang "chat/completions"
	// ---------------------------------------------------------------------

	private static HttpRequest buildOpenAiStyleRequest(String url, String apiKey, String model,
			String systemPrompt, int maxTokens, String question,
			java.util.function.UnaryOperator<HttpRequest.Builder> extraHeaders) {
		JsonObject body = new JsonObject();
		body.addProperty("model", model);
		body.addProperty("max_tokens", maxTokens);

		JsonArray messages = new JsonArray();
		JsonObject sysMsg = new JsonObject();
		sysMsg.addProperty("role", "system");
		sysMsg.addProperty("content", systemPrompt);
		messages.add(sysMsg);

		JsonObject userMsg = new JsonObject();
		userMsg.addProperty("role", "user");
		userMsg.addProperty("content", question);
		messages.add(userMsg);
		body.add("messages", messages);

		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(Duration.ofSeconds(30))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + apiKey)
				.POST(HttpRequest.BodyPublishers.ofString(body.toString()));

		if (extraHeaders != null) {
			builder = extraHeaders.apply(builder);
		}
		return builder.build();
	}

	private static String parseOpenAiStyle(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		JsonArray choices = root.getAsJsonArray("choices");
		JsonObject firstChoice = choices.get(0).getAsJsonObject();
		JsonObject message = firstChoice.getAsJsonObject("message");
		return message.get("content").getAsString().trim();
	}

	// ---------------------------------------------------------------------
	// Google Gemini
	// ---------------------------------------------------------------------

	private static HttpRequest buildGeminiRequest(AskConfig.Data cfg, String question) {
		JsonObject body = new JsonObject();

		JsonObject systemInstruction = new JsonObject();
		JsonArray sysParts = new JsonArray();
		JsonObject sysPart = new JsonObject();
		sysPart.addProperty("text", cfg.systemPrompt);
		sysParts.add(sysPart);
		systemInstruction.add("parts", sysParts);
		body.add("systemInstruction", systemInstruction);

		JsonArray contents = new JsonArray();
		JsonObject userContent = new JsonObject();
		userContent.addProperty("role", "user");
		JsonArray userParts = new JsonArray();
		JsonObject userPart = new JsonObject();
		userPart.addProperty("text", question);
		userParts.add(userPart);
		userContent.add("parts", userParts);
		contents.add(userContent);
		body.add("contents", contents);

		JsonObject generationConfig = new JsonObject();
		generationConfig.addProperty("maxOutputTokens", cfg.maxTokens);
		body.add("generationConfig", generationConfig);

		String url = "https://generativelanguage.googleapis.com/v1beta/models/"
				+ cfg.geminiModel + ":generateContent?key=" + cfg.geminiApiKey;

		return HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(Duration.ofSeconds(30))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body.toString()))
				.build();
	}

	private static String parseGemini(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		JsonArray candidates = root.getAsJsonArray("candidates");
		if (candidates == null || candidates.isEmpty()) {
			return "";
		}
		JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
		if (content == null || !content.has("parts")) {
			return "";
		}
		JsonArray parts = content.getAsJsonArray("parts");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parts.size(); i++) {
			JsonObject part = parts.get(i).getAsJsonObject();
			if (part.has("text")) {
				sb.append(part.get("text").getAsString());
			}
		}
		return sb.toString().trim();
	}
}
