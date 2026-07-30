package com.example.askmod;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Man hinh cau hinh AskMod hien thi khi bam vao mod trong danh sach Mods
 * (ModMenu). Chi doc/ghi duoc file config/askmod.json cua tien trinh dang
 * chay man hinh nay - trong Singleplayer thi client va integrated server
 * dung chung 1 tien trinh nen hoat dong dung y; tren dedicated server that
 * su, nguoi choi van phai dung lenh "/ask admin" nhu cu.
 */
public class AskConfigScreen extends Screen {
	private final Screen parent;

	private TextFieldWidget anthropicKeyField;
	private TextFieldWidget anthropicModelField;
	private TextFieldWidget openaiKeyField;
	private TextFieldWidget openaiModelField;
	private TextFieldWidget geminiKeyField;
	private TextFieldWidget geminiModelField;
	private TextFieldWidget openrouterKeyField;
	private TextFieldWidget openrouterModelField;
	private TextFieldWidget systemPromptField;
	private TextFieldWidget maxTokensField;
	private TextFieldWidget cooldownField;
	private TextFieldWidget permissionField;

	private String selectedProvider;

	protected AskConfigScreen(Screen parent) {
		super(Text.literal("AskMod - Cau hinh"));
		this.parent = parent;
		this.selectedProvider = AskConfig.normalizeProvider(AskConfig.get().provider);
	}

	@Override
	protected void init() {
		AskConfig.Data cfg = AskConfig.get();

		int fieldWidth = 220;
		int leftCol = this.width / 2 - 230;
		int rightCol = this.width / 2 + 10;
		int y = 40;
		int rowHeight = 24;

		// --- Provider dang su dung ---
		this.addDrawableChild(CyclingButtonWidget.<String>builder(Text::literal)
				.values(AskConfig.PROVIDERS)
				.initially(selectedProvider)
				.build(leftCol, y, fieldWidth, 20, Text.literal("Provider dang dung"),
						(button, value) -> selectedProvider = value));
		y += rowHeight + 6;

		// --- Anthropic ---
		anthropicKeyField = new TextFieldWidget(this.textRenderer, leftCol, y, fieldWidth, 20,
				Text.literal("Anthropic API key"));
		anthropicKeyField.setMaxLength(512);
		anthropicKeyField.setText(cfg.anthropicApiKey);
		this.addDrawableChild(anthropicKeyField);

		anthropicModelField = new TextFieldWidget(this.textRenderer, rightCol, y, fieldWidth, 20,
				Text.literal("Anthropic model"));
		anthropicModelField.setMaxLength(128);
		anthropicModelField.setText(cfg.anthropicModel);
		this.addDrawableChild(anthropicModelField);
		y += rowHeight;

		// --- OpenAI ---
		openaiKeyField = new TextFieldWidget(this.textRenderer, leftCol, y, fieldWidth, 20,
				Text.literal("OpenAI API key"));
		openaiKeyField.setMaxLength(512);
		openaiKeyField.setText(cfg.openaiApiKey);
		this.addDrawableChild(openaiKeyField);

		openaiModelField = new TextFieldWidget(this.textRenderer, rightCol, y, fieldWidth, 20,
				Text.literal("OpenAI model"));
		openaiModelField.setMaxLength(128);
		openaiModelField.setText(cfg.openaiModel);
		this.addDrawableChild(openaiModelField);
		y += rowHeight;

		// --- Gemini ---
		geminiKeyField = new TextFieldWidget(this.textRenderer, leftCol, y, fieldWidth, 20,
				Text.literal("Gemini API key"));
		geminiKeyField.setMaxLength(512);
		geminiKeyField.setText(cfg.geminiApiKey);
		this.addDrawableChild(geminiKeyField);

		geminiModelField = new TextFieldWidget(this.textRenderer, rightCol, y, fieldWidth, 20,
				Text.literal("Gemini model"));
		geminiModelField.setMaxLength(128);
		geminiModelField.setText(cfg.geminiModel);
		this.addDrawableChild(geminiModelField);
		y += rowHeight;

		// --- OpenRouter ---
		openrouterKeyField = new TextFieldWidget(this.textRenderer, leftCol, y, fieldWidth, 20,
				Text.literal("OpenRouter API key"));
		openrouterKeyField.setMaxLength(512);
		openrouterKeyField.setText(cfg.openrouterApiKey);
		this.addDrawableChild(openrouterKeyField);

		openrouterModelField = new TextFieldWidget(this.textRenderer, rightCol, y, fieldWidth, 20,
				Text.literal("OpenRouter model"));
		openrouterModelField.setMaxLength(128);
		openrouterModelField.setText(cfg.openrouterModel);
		this.addDrawableChild(openrouterModelField);
		y += rowHeight + 10;

		// --- Cac tuy chon chung ---
		systemPromptField = new TextFieldWidget(this.textRenderer, leftCol, y, fieldWidth * 2 + 20, 20,
				Text.literal("System prompt"));
		systemPromptField.setMaxLength(1024);
		systemPromptField.setText(cfg.systemPrompt);
		this.addDrawableChild(systemPromptField);
		y += rowHeight;

		maxTokensField = new TextFieldWidget(this.textRenderer, leftCol, y, 100, 20,
				Text.literal("Max tokens"));
		maxTokensField.setMaxLength(6);
		maxTokensField.setText(String.valueOf(cfg.maxTokens));
		this.addDrawableChild(maxTokensField);

		cooldownField = new TextFieldWidget(this.textRenderer, leftCol + 110, y, 100, 20,
				Text.literal("Cooldown (giay)"));
		cooldownField.setMaxLength(6);
		cooldownField.setText(String.valueOf(cfg.cooldownSeconds));
		this.addDrawableChild(cooldownField);

		permissionField = new TextFieldWidget(this.textRenderer, leftCol + 220, y, 100, 20,
				Text.literal("Permission level"));
		permissionField.setMaxLength(1);
		permissionField.setText(String.valueOf(cfg.adminPermissionLevel));
		this.addDrawableChild(permissionField);
		y += rowHeight + 10;

		// --- Nut Luu / Huy ---
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Luu"), button -> saveAndClose())
				.dimensions(this.width / 2 - 105, this.height - 28, 100, 20)
				.build());
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Huy"), button -> this.close())
				.dimensions(this.width / 2 + 5, this.height - 28, 100, 20)
				.build());
	}

	private void saveAndClose() {
		AskConfig.Data cfg = AskConfig.get();

		cfg.provider = AskConfig.normalizeProvider(selectedProvider);

		cfg.anthropicApiKey = anthropicKeyField.getText();
		cfg.anthropicModel = anthropicModelField.getText();

		cfg.openaiApiKey = openaiKeyField.getText();
		cfg.openaiModel = openaiModelField.getText();

		cfg.geminiApiKey = geminiKeyField.getText();
		cfg.geminiModel = geminiModelField.getText();

		cfg.openrouterApiKey = openrouterKeyField.getText();
		cfg.openrouterModel = openrouterModelField.getText();

		cfg.systemPrompt = systemPromptField.getText();
		cfg.maxTokens = parseIntSafe(maxTokensField.getText(), cfg.maxTokens);
		cfg.cooldownSeconds = parseIntSafe(cooldownField.getText(), cfg.cooldownSeconds);

		int permission = parseIntSafe(permissionField.getText(), cfg.adminPermissionLevel);
		cfg.adminPermissionLevel = Math.max(0, Math.min(4, permission));

		AskConfig.save();
		this.close();
	}

	private static int parseIntSafe(String text, int fallback) {
		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	@Override
	public void close() {
		if (this.client != null) {
			this.client.setScreen(parent);
		}
	}

	@Override
	public boolean shouldPauseGame() {
		return false;
	}
}
