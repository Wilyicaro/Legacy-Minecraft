//? if fabric && (1.21.1 || >=1.21.4) {
package wily.legacy.client.screen.compat;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.combo_options.RecordingControlsLocation;
import com.moulberry.flashback.record.Recorder;
import com.moulberry.flashback.screen.FlashbackButton;
import com.moulberry.flashback.screen.select_replay.SelectReplayScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.factoryapi.FactoryAPI;
import wily.legacy.client.screen.RenderableVList;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public final class FlashbackCompat {
    private static boolean isLoaded() {
        return FactoryAPI.isLoadingMod("flashback");
    }

    public static boolean isExporting() {
        return isLoaded() && Direct.isExporting();
    }

    public static AbstractWidget createReplayButton(Screen parent, AbstractWidget anchor) {
        if (!isLoaded()) return null;
        return Direct.createReplayButton(parent, anchor);
    }

    public static void addBelowRecordingButtons(RenderableVList list, Screen parent) {
        if (isLoaded()) Direct.addBelowRecordingButtons(list, parent);
    }

    public static List<AbstractWidget> createSideRecordingButtons(Screen parent, List<Renderable> renderables) {
        return isLoaded() ? Direct.createSideRecordingButtons(parent, renderables) : List.of();
    }

    private static final class Direct {
        private static boolean isExporting() {
            return Flashback.isExporting();
        }

        private static AbstractWidget createReplayButton(Screen parent, AbstractWidget anchor) {
            int size = anchor.getHeight();
            return new FlashbackButton(anchor.getRight() + 4, anchor.getY(), size, size, Component.translatable("flashback.open_replays"), button -> openReplays(parent));
        }

        private static void addBelowRecordingButtons(RenderableVList list, Screen parent) {
            if (Flashback.isInReplay() || controlsLocation() != RecordingControlsLocation.BELOW) return;
            Recorder recorder = Flashback.RECORDER;
            if (recorder == null) {
                list.addRenderable(Button.builder(Component.translatable("flashback.recording_controls.start"), button -> runAndClose(Flashback::startRecordingReplay)).build());
                return;
            }
            list.addRenderable(Button.builder(Component.translatable("flashback.recording_controls.finish"), button -> runAndClose(Flashback::finishRecordingReplay)).build());
            boolean paused = recorder.isPaused();
            list.addRenderable(Button.builder(Component.translatable(paused ? "flashback.recording_controls.unpause" : "flashback.recording_controls.pause"), button -> runAndClose(() -> Flashback.pauseRecordingReplay(!paused))).build());
            list.addRenderable(Button.builder(Component.translatable("flashback.recording_controls.cancel"), button -> confirmCancel(parent)).build());
        }

        private static List<AbstractWidget> createSideRecordingButtons(Screen parent, List<Renderable> renderables) {
            if (Flashback.isInReplay()) return List.of();
            RecordingControlsLocation location = controlsLocation();
            if (location != RecordingControlsLocation.RIGHT && location != RecordingControlsLocation.LEFT) return List.of();
            boolean right = location == RecordingControlsLocation.RIGHT;
            int x = parent.width / 2;
            TreeSet<Integer> heights = new TreeSet<>();
            for (Renderable renderable : renderables) {
                if (renderable instanceof AbstractWidget widget) {
                    int nextX = right ? widget.getRight() + 4 : widget.getX() - 24;
                    if (right ? nextX > x : nextX < x) {
                        x = nextX;
                        heights.clear();
                    }
                    heights.add(widget.getY());
                }
            }
            List<Integer> yPositions = new ArrayList<>(heights);
            int index = yPositions.size() >= 4 ? 1 : 0;
            Recorder recorder = Flashback.RECORDER;
            if (recorder == null) {
                return List.of(createRecordingButton(x, nextY(yPositions, index), "flashback.recording_controls.start", "icon_pixelated_start.png", button -> runAndClose(Flashback::startRecordingReplay)));
            }
            List<AbstractWidget> buttons = new ArrayList<>();
            buttons.add(createRecordingButton(x, nextY(yPositions, index++), "flashback.recording_controls.finish", "icon_pixelated_finish.png", button -> runAndClose(Flashback::finishRecordingReplay)));
            boolean paused = recorder.isPaused();
            buttons.add(createRecordingButton(x, nextY(yPositions, index++), paused ? "flashback.recording_controls.unpause" : "flashback.recording_controls.pause", paused ? "icon_pixelated_start.png" : "icon_pixelated_pause.png", button -> runAndClose(() -> Flashback.pauseRecordingReplay(!paused))));
            buttons.add(createRecordingButton(x, nextY(yPositions, index), "flashback.recording_controls.cancel", "icon_pixelated_cancel.png", button -> confirmCancel(parent)));
            return buttons;
        }

        private static int nextY(List<Integer> positions, int index) {
            if (index < positions.size()) return positions.get(index);
            return positions.isEmpty() ? Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2 : positions.get(positions.size() - 1) + 24 * (index - positions.size() + 1);
        }

        private static AbstractWidget createRecordingButton(int x, int y, String translationKey, String icon, Button.OnPress action) {
            return new FlashbackButton(x, y, 20, 20, Component.translatable(translationKey), action, FactoryAPI.createLocation("flashback", icon)).flashbackWithTooltip();
        }

        private static void openReplays(Screen parent) {
            Minecraft minecraft = Minecraft.getInstance();
            List<String> incompatibleMods = Screen.hasShiftDown() ? List.of() : Flashback.getReplayIncompatibleMods();
            if (incompatibleMods.isEmpty()) {
                minecraft.setScreen(new SelectReplayScreen(parent, Flashback.getReplayFolder()));
                return;
            }
            Component description = Component.translatable("flashback.incompatible_with_viewing_description").append(Component.literal(String.join(", ", incompatibleMods)).withStyle(ChatFormatting.RED));
            minecraft.setScreen(new AlertScreen(() -> minecraft.setScreen(parent), Component.translatable("flashback.incompatible_with_viewing"), description));
        }

        private static void confirmCancel(Screen parent) {
            Minecraft.getInstance().setScreen(new ConfirmScreen(value -> {
                if (value) runAndClose(Flashback::cancelRecordingReplay);
                else Minecraft.getInstance().setScreen(parent);
            }, Component.translatable("flashback.confirm_cancel_recording"), Component.translatable("flashback.confirm_cancel_recording_description")));
        }

        private static RecordingControlsLocation controlsLocation() {
            return Flashback.getConfig().recordingControls.controlsLocation;
        }

        private static void runAndClose(Runnable action) {
            action.run();
            Minecraft.getInstance().setScreen(null);
        }
    }
}
//?}
