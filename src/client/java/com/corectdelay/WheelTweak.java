package com.corectdelay;

import com.corectdelay.mixin.client.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;

public class WheelTweak {

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof HandledScreen<?> handled && !(screen instanceof CreativeInventoryScreen)) {
                ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontal, vertical) -> {
                    if (!CorectDelayConfig.wheelTweak || vertical == 0) {
                        return true; // przepuść do vanilli
                    }
                    boolean handledByUs = handleScroll(client, handled, vertical);
                    return !handledByUs; // false = anuluj domyślną akcję scrolla
                });
            }
        });
    }

    private static boolean handleScroll(MinecraftClient client, HandledScreen<?> screen, double vertical) {
        if (client.player == null || client.interactionManager == null) return false;

        ScreenHandler handler = screen.getScreenHandler();
        if (!handler.getCursorStack().isEmpty()) return false;

        Slot hovered = ((HandledScreenAccessor) screen).corectdelay$getFocusedSlot();
        if (hovered == null || !hovered.hasStack()) return false;

        ItemStack hoveredStack = hovered.getStack();
        if (!hovered.canInsert(hoveredStack)) return false; // pomijamy sloty wyniku (np. crafting)

        boolean scrollUp = vertical > 0;
        boolean push; // true = wysyłamy item z kliknietego slotu, false = ściągamy do niego
        switch (CorectDelayConfig.wheelScrollDirection) {
            case 1 -> push = scrollUp;
            case 2 -> push = (scrollUp == isOtherInventoryAbove(handler, hovered));
            default -> push = !scrollUp;
        }

        if (push) {
            Slot target = findPushTarget(handler, hovered, hoveredStack);
            if (target == null) return true;
            moveOne(client, handler, hovered, target);
        } else {
            int max = Math.min(hovered.getMaxItemCount(hoveredStack), hoveredStack.getMaxCount());
            if (hoveredStack.getCount() >= max) return true;
            Slot source = findPullSource(client, handler, hovered, hoveredStack);
            if (source == null) return true;
            moveOne(client, handler, source, hovered);
        }
        return true;
    }

    /** Przenosi dokładnie 1 item: podnieś stack, prawy klik w cel, odłóż resztę. */
    private static void moveOne(MinecraftClient client, ScreenHandler handler, Slot from, Slot to) {
        int sync = handler.syncId;
        var im = client.interactionManager;
        im.clickSlot(sync, from.id, 0, SlotActionType.PICKUP, client.player);
        im.clickSlot(sync, to.id, 1, SlotActionType.PICKUP, client.player);
        im.clickSlot(sync, from.id, 0, SlotActionType.PICKUP, client.player);
    }

    private static Slot findPushTarget(ScreenHandler handler, Slot hovered, ItemStack stack) {
        Slot firstEmpty = null;
        for (Slot s : handler.slots) {
            if (s.inventory == hovered.inventory) continue;
            if (!s.canInsert(stack)) continue;
            ItemStack in = s.getStack();
            if (in.isEmpty()) {
                if (firstEmpty == null) firstEmpty = s;
            } else if (ItemStack.areItemsAndComponentsEqual(in, stack)
                    && in.getCount() < Math.min(s.getMaxItemCount(in), in.getMaxCount())) {
                return s; // najpierw dopełniamy istniejące stacki
            }
        }
        return firstEmpty;
    }

    private static Slot findPullSource(MinecraftClient client, ScreenHandler handler, Slot hovered, ItemStack stack) {
        List<Slot> slots = handler.slots;
        boolean lastToFirst = CorectDelayConfig.wheelSearchOrder;
        int start = lastToFirst ? slots.size() - 1 : 0;
        int end = lastToFirst ? -1 : slots.size();
        int step = lastToFirst ? -1 : 1;
        for (int i = start; i != end; i += step) {
            Slot s = slots.get(i);
            if (s.inventory == hovered.inventory) continue;
            if (!s.hasStack()) continue;
            ItemStack in = s.getStack();
            if (!s.canInsert(in)) continue; // pomijamy sloty wyniku
            if (!s.canTakeItems(client.player)) continue;
            if (ItemStack.areItemsAndComponentsEqual(in, stack)) return s;
        }
        return null;
    }

    private static boolean isOtherInventoryAbove(ScreenHandler handler, Slot hovered) {
        double sum = 0;
        int n = 0;
        for (Slot s : handler.slots) {
            if (s.inventory != hovered.inventory) {
                sum += s.y;
                n++;
            }
        }
        return n > 0 && (sum / n) < hovered.y;
    }
}
