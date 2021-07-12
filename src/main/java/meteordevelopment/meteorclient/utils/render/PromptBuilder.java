/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.gui.*;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.client.gui.screen.Screen;

import static meteordevelopment.meteorclient.utils.Utils.mc;

public class PromptBuilder {
    private final GuiTheme theme;
    private final Screen parent;
    private String title = "";
    private String message = "";
    private Runnable onYes = () -> {};
    private Runnable onNo = () -> {};
    private int promptHash = -1;

    public PromptBuilder() {
        this(GuiThemes.get(), mc.currentScreen);
    }

    public PromptBuilder(GuiTheme theme, Screen parent) {
        this.theme = theme;
        this.parent = parent;
    }

    public PromptBuilder title(String title) {
        this.title = title;
        return this;
    }

    public PromptBuilder message(String message) {
        this.message = message;
        return this;
    }

    public PromptBuilder onYes(Runnable runnable) {
        this.onYes = runnable;
        return this;
    }

    public PromptBuilder onNo(Runnable runnable) {
        this.onNo = runnable;
        return this;
    }

    public PromptBuilder promptHash(int hash) {
        this.promptHash = hash;
        return this;
    }

    public PromptBuilder promptHash(Object from) {
        this.promptHash = from.hashCode();
        return this;
    }

    public void show() {
        if (promptHash == -1) this.promptHash(this);
        if (Config.get().dontShowAgainPrompts.contains(promptHash)) {
            onNo.run();
            return;
        }
        Screen prompt = new PromptScreen(theme, title, message, onYes, onNo, parent, promptHash);
        mc.openScreen(prompt);
    }

    private class PromptScreen extends WindowScreen {

        public PromptScreen(GuiTheme theme, String title, String message, Runnable onYes, Runnable onNo, Screen parent, int promptHash) {
            super(theme, title);
            this.parent = parent;

            for (String line : message.split("\n")) {
                add(theme.label(line)).expandCellX();
            }

            add(theme.horizontalSeparator()).expandCellX();


            WHorizontalList checkboxContainer = add(theme.horizontalList()).expandCellX().widget();
            WCheckbox dontShowAgainCheckbox = checkboxContainer.add(theme.checkbox(false)).widget();
            checkboxContainer.add(theme.label("Don't show this prompt again.")).expandCellX();

            WHorizontalList list = add(theme.horizontalList()).expandCellX().widget();

            WButton yesButton = list.add(theme.button("Yes")).widget();
            yesButton.action = () -> {
                onYes.run();
                this.onClose();
            };

            WButton noButton = list.add(theme.button("No")).widget();
            noButton.action = () -> {
                onNo.run();
                if (dontShowAgainCheckbox.checked) {
                    Config.get().dontShowAgainPrompts.add(promptHash);
                }
                this.onClose();
            };

            dontShowAgainCheckbox.action = () -> {
                yesButton.visible = !dontShowAgainCheckbox.checked;
            };
        }

    }
}
