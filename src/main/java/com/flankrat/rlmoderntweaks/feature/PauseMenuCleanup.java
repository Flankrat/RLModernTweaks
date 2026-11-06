package com.flankrat.rlmoderntweaks.feature;

import com.flankrat.rlmoderntweaks.config.RLMTConfig;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pause menu: hides Advancements and Give Feedback and closes the gaps. Statistics and Report
 * Bugs move onto the row Advancements was on and everything below moves up one row. Buttons are
 * found by their translation key, so it works in any language. Port of hide_advancement_button.js.
 */
public final class PauseMenuCleanup {
    private PauseMenuCleanup() {
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen) || !RLMTConfig.PAUSE_MENU_CLEANUP.get()) return;

        Map<String, AbstractWidget> byKey = new HashMap<>();
        List<AbstractWidget> widgets = new ArrayList<>();
        for (var listener : event.getListenersList()) {
            if (!(listener instanceof AbstractWidget widget)) continue;
            widgets.add(widget);
            String key = widget.getMessage().getContents() instanceof TranslatableContents t ? t.getKey() : widget.getMessage().getString();
            byKey.put(key, widget);
        }

        AbstractWidget adv = byKey.get("gui.advancements");
        AbstractWidget stats = byKey.get("gui.stats");
        AbstractWidget feedback = byKey.get("menu.sendFeedback");
        AbstractWidget bugs = byKey.get("menu.reportBugs");
        for (AbstractWidget hidden : new AbstractWidget[]{adv, feedback}) {
            if (hidden != null) {
                hidden.visible = false;
                hidden.active = false;
            }
        }

        // Only rearrange the vanilla layout, so other mods' menus aren't broken.
        if (adv == null || stats == null || feedback == null || bugs == null) return;
        int rowStep = feedback.getY() - adv.getY();
        if (rowStep <= 0) return;
        int leftX = adv.getX();
        int rightX = stats.getX();
        int topRowY = adv.getY();
        int removedRowY = feedback.getY();
        stats.setPosition(leftX, topRowY);
        bugs.setPosition(rightX, topRowY);
        for (AbstractWidget widget : widgets) {
            if (widget == adv || widget == feedback || widget == stats || widget == bugs) continue;
            if (widget.getY() > removedRowY) widget.setY(widget.getY() - rowStep);
        }
    }
}
