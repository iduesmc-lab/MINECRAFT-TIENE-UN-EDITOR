package com.iduesmc.director.client;

import com.iduesmc.director.net.CinemaCardPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the director's cards: cinematic letterbox bars, a blinking «● REC», the take number and a
 * clapperboard with the scene title. Time is measured in real milliseconds so it still works in slow motion.
 */
public final class CinemaHud {
	private static final long SLIDE_MS = 350;

	@Nullable
	private static CinemaCardPayload card;
	private static long shownAt;

	private CinemaHud() {
	}

	public static void show(CinemaCardPayload payload) {
		card = payload;
		shownAt = System.currentTimeMillis();
	}

	public static void render(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (card == null || client.options.hudHidden) {
			return;
		}
		long elapsed = System.currentTimeMillis() - shownAt;
		if (elapsed > card.durationMs()) {
			card = null;
			return;
		}
		float in = MathHelper.clamp(elapsed / (float) SLIDE_MS, 0f, 1f);
		float out = MathHelper.clamp((card.durationMs() - elapsed) / (float) SLIDE_MS, 0f, 1f);
		float progress = Math.min(in, out);
		float eased = 1f - (1f - progress) * (1f - progress);

		TextRenderer font = client.textRenderer;
		int width = context.getScaledWindowWidth();
		int height = context.getScaledWindowHeight();

		// Letterbox.
		int bar = Math.round(height * 0.12f * eased);
		context.fill(0, 0, width, bar, 0xFF000000);
		context.fill(0, height - bar, width, height, 0xFF000000);

		int alpha = Math.max(4, Math.round(255 * eased)) << 24;
		if (bar > 10) {
			if ((System.currentTimeMillis() / 500) % 2 == 0) {
				context.drawTextWithShadow(font, Text.literal("● REC"), 8, (bar - 8) / 2, 0xFF3333 | alpha);
			}
			String take = "TOMA " + card.take();
			context.drawTextWithShadow(font, Text.literal(take), width - 8 - font.getWidth(take), (bar - 8) / 2, 0xFFFFFF | alpha);
		}

		// Clapperboard.
		Text title = Text.literal(card.title());
		Text subtitle = Text.literal(card.subtitle());
		float scale = width < 480 ? 2.0f : 3.0f;
		int titleWidth = Math.round(font.getWidth(title) * scale);
		int boxWidth = Math.min(width - 16, Math.max(titleWidth, font.getWidth(subtitle)) + 28);
		int boxHeight = Math.round(9 * scale) + 30;
		int boxX = (width - boxWidth) / 2;
		int boxY = Math.round(height * 0.30f) - Math.round((1f - eased) * 20);

		int stripe = 8;
		context.fill(boxX, boxY - stripe, boxX + boxWidth, boxY, 0x111111 | alpha);
		for (int x = boxX, i = 0; x < boxX + boxWidth; x += 12, i++) {
			if (i % 2 == 0) {
				context.fill(x, boxY - stripe, Math.min(x + 6, boxX + boxWidth), boxY, 0xEEEEEE | alpha);
			}
		}
		context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x1A1A1A | (Math.round(220 * eased) << 24));
		context.fill(boxX, boxY + boxHeight - 2, boxX + boxWidth, boxY + boxHeight, card.color() | alpha);

		MatrixStack matrices = context.getMatrices();
		matrices.push();
		matrices.translate(width / 2f, boxY + 8, 0);
		matrices.scale(scale, scale, 1f);
		context.drawCenteredTextWithShadow(font, title, 0, 0, card.color() | alpha);
		matrices.pop();
		context.drawCenteredTextWithShadow(font, subtitle, width / 2, boxY + 8 + Math.round(9 * scale) + 6, 0xFFFFFF | alpha);
	}
}
