/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.util.math.MatrixStack;

public interface TextRenderer {
    static TextRenderer get() {
        return Config.get().customFont.get() ? Fonts.RENDERER : VanillaTextRenderer.INSTANCE;
    }

    void setAlpha(double a);

    void begin(double scale, boolean scaleOnly, boolean big);
    default void begin(double scale) { begin(scale, false, false); }
    default void begin() { begin(1, false, false); }

    default void beginBig() { begin(1, false, true); }

    double getWidth(String text, int length, boolean shadow, boolean title);
    default double getWidth(String text, int length, boolean shadow) { return getWidth(text, length, shadow, false); }
    default double getWidth(String text, boolean shadow) { return getWidth(text, text.length(), shadow); }
    default double getWidth(String text) { return getWidth(text, text.length(), false); }

    double getHeight(boolean shadow);
    default double getHeight() { return getHeight(false); }

    default double getTitleScale() {
        return Config.get().titleTextSize.get();
    }

    double render(String text, double x, double y, Color color, boolean shadow, boolean title);
    default double render(String text, double x, double y, Color color, boolean shadow) { return render(text, x, y, color, shadow, false); }
    default double render(String text, double x, double y, Color color) { return render(text, x, y, color, false, false); }

    boolean isBuilding();

    default void end() { end(null); }
    void end(MatrixStack matrices);
}
