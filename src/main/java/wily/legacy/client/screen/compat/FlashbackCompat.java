package wily.legacy.client.screen.compat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.FactoryAPI;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.RenderableVList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class FlashbackCompat {
    private static boolean initialized;
    private static boolean supported;
    private static Field recorderField;
    private static Field exportJobField;
    private static Field recordingControlsField;
    private static Field controlsLocationField;
    private static Method isInReplayMethod;
    private static Method getConfigMethod;
    private static Method getReplayIncompatibleModsMethod;
    private static Method startRecordingMethod;
    private static Method finishRecordingMethod;
    private static Method pauseRecordingMethod;
    private static Method cancelRecordingMethod;
    private static Method recorderIsPausedMethod;
    private static Method withTooltipMethod;
    private static Constructor<?> replayButtonConstructor;
    private static Constructor<?> recordingButtonConstructor;
    private static Constructor<?> selectReplayScreenConstructor;

    private static synchronized boolean initialize() {
        if (initialized) return supported;
        initialized = true;
        if (!FactoryAPI.isLoadingMod("flashback")) return false;
        try {
            Class<?> flashbackClass = Class.forName("com.moulberry.flashback.Flashback");
            Class<?> recorderClass = Class.forName("com.moulberry.flashback.record.Recorder");
            Class<?> flashbackButtonClass = Class.forName("com.moulberry.flashback.screen.FlashbackButton");
            Class<?> selectReplayScreenClass = Class.forName("com.moulberry.flashback.screen.select_replay.SelectReplayScreen");
            recorderField = flashbackClass.getField("RECORDER");
            exportJobField = flashbackClass.getField("EXPORT_JOB");
            isInReplayMethod = flashbackClass.getMethod("isInReplay");
            getConfigMethod = flashbackClass.getMethod("getConfig");
            getReplayIncompatibleModsMethod = flashbackClass.getMethod("getReplayIncompatibleMods");
            startRecordingMethod = flashbackClass.getMethod("startRecordingReplay");
            finishRecordingMethod = flashbackClass.getMethod("finishRecordingReplay");
            pauseRecordingMethod = flashbackClass.getMethod("pauseRecordingReplay", boolean.class);
            cancelRecordingMethod = flashbackClass.getMethod("cancelRecordingReplay");
            recorderIsPausedMethod = recorderClass.getMethod("isPaused");
            recordingControlsField = getConfigMethod.getReturnType().getField("recordingControls");
            controlsLocationField = recordingControlsField.getType().getField("controlsLocation");
            replayButtonConstructor = flashbackButtonClass.getConstructor(int.class, int.class, int.class, int.class, Component.class, Button.OnPress.class);
            recordingButtonConstructor = flashbackButtonClass.getConstructor(int.class, int.class, int.class, int.class, Component.class, Button.OnPress.class, ResourceLocation.class);
            withTooltipMethod = flashbackButtonClass.getMethod("flashbackWithTooltip");
            selectReplayScreenConstructor = selectReplayScreenClass.getConstructor(Screen.class);
            supported = true;
        } catch (ReflectiveOperationException exception) {
            Legacy4J.LOGGER.error("Unable to initialize Flashback compatibility", exception);
        }
        return supported;
    }

    public static boolean isExporting() {
        if (!initialize()) return false;
        try {
            return exportJobField.get(null) != null;
        } catch (IllegalAccessException exception) {
            return false;
        }
    }

    public static AbstractWidget createReplayButton(Screen parent, AbstractWidget anchor) {
        if (!initialize()) return null;
        try {
            int size = anchor.getHeight();
            return (AbstractWidget) replayButtonConstructor.newInstance(anchor.getX() + anchor.getWidth() + 4, anchor.getY(), size, size, Component.translatable("flashback.open_replays"), (Button.OnPress) button -> openReplays(parent));
        } catch (ReflectiveOperationException exception) {
            Legacy4J.LOGGER.error("Unable to create Flashback replay button", exception);
            return null;
        }
    }

    public static void addBelowRecordingButtons(RenderableVList list, Screen parent) {
        if (!initialize() || isInReplay() || !"BELOW".equals(controlsLocation())) return;
        Object recorder = recorder();
        if (recorder == null) {
            list.addRenderable(Button.builder(Component.translatable("flashback.recording_controls.start"), button -> invokeAndClose(startRecordingMethod)).build());
            return;
        }
        list.addRenderable(Button.builder(Component.translatable("flashback.recording_controls.finish"), button -> invokeAndClose(finishRecordingMethod)).build());
        boolean paused = isPaused(recorder);
        list.addRenderable(Button.builder(Component.translatable(paused ? "flashback.recording_controls.unpause" : "flashback.recording_controls.pause"), button -> invokeAndClose(pauseRecordingMethod, !paused)).build());
        list.addRenderable(Button.builder(Component.translatable("flashback.recording_controls.cancel"), button -> confirmCancel(parent)).build());
    }

    public static List<AbstractWidget> createSideRecordingButtons(Screen parent, List<Renderable> renderables) {
        if (!initialize() || isInReplay()) return List.of();
        String location = controlsLocation();
        if (!"RIGHT".equals(location) && !"LEFT".equals(location)) return List.of();
        boolean right = "RIGHT".equals(location);
        int x = parent.width / 2;
        TreeSet<Integer> heights = new TreeSet<>();
        for (Renderable renderable : renderables) {
            if (renderable instanceof AbstractWidget widget) {
                int nextX = right ? widget.getX() + widget.getWidth() + 4 : widget.getX() - 24;
                if (right ? nextX > x : nextX < x) {
                    x = nextX;
                    heights.clear();
                }
                heights.add(widget.getY());
            }
        }
        List<Integer> yPositions = new ArrayList<>(heights);
        int index = yPositions.size() >= 4 ? 1 : 0;
        List<AbstractWidget> buttons = new ArrayList<>();
        Object recorder = recorder();
        if (recorder == null) {
            AbstractWidget button = createRecordingButton(x, nextY(yPositions, index), "flashback.recording_controls.start", "icon_pixelated_start.png", value -> invokeAndClose(startRecordingMethod));
            return button == null ? List.of() : List.of(button);
        }
        buttons.add(createRecordingButton(x, nextY(yPositions, index++), "flashback.recording_controls.finish", "icon_pixelated_finish.png", button -> invokeAndClose(finishRecordingMethod)));
        boolean paused = isPaused(recorder);
        buttons.add(createRecordingButton(x, nextY(yPositions, index++), paused ? "flashback.recording_controls.unpause" : "flashback.recording_controls.pause", paused ? "icon_pixelated_start.png" : "icon_pixelated_pause.png", button -> invokeAndClose(pauseRecordingMethod, !paused)));
        buttons.add(createRecordingButton(x, nextY(yPositions, index), "flashback.recording_controls.cancel", "icon_pixelated_cancel.png", button -> confirmCancel(parent)));
        buttons.removeIf(java.util.Objects::isNull);
        return buttons;
    }

    private static int nextY(List<Integer> positions, int index) {
        if (index < positions.size()) return positions.get(index);
        return positions.isEmpty() ? Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2 : positions.get(positions.size() - 1) + 24 * (index - positions.size() + 1);
    }

    private static AbstractWidget createRecordingButton(int x, int y, String translationKey, String icon, Button.OnPress action) {
        try {
            Object button = recordingButtonConstructor.newInstance(x, y, 20, 20, Component.translatable(translationKey), action, FactoryAPI.createLocation("flashback", icon));
            withTooltipMethod.invoke(button);
            return (AbstractWidget) button;
        } catch (ReflectiveOperationException exception) {
            Legacy4J.LOGGER.error("Unable to create Flashback recording button", exception);
            return null;
        }
    }

    private static void openReplays(Screen parent) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            List<?> incompatibleMods = Screen.hasShiftDown() ? List.of() : (List<?>) getReplayIncompatibleModsMethod.invoke(null);
            if (incompatibleMods.isEmpty()) {
                minecraft.setScreen((Screen) selectReplayScreenConstructor.newInstance(parent));
                return;
            }
            String names = incompatibleMods.stream().map(Object::toString).collect(Collectors.joining(", "));
            Component description = Component.translatable("flashback.incompatible_with_viewing_description").append(Component.literal(names).withStyle(ChatFormatting.RED));
            minecraft.setScreen(new AlertScreen(() -> minecraft.setScreen(parent), Component.translatable("flashback.incompatible_with_viewing"), description));
        } catch (ReflectiveOperationException exception) {
            Legacy4J.LOGGER.error("Unable to open Flashback replays", exception);
        }
    }

    private static void confirmCancel(Screen parent) {
        Minecraft.getInstance().setScreen(new ConfirmScreen(value -> {
            if (value) invokeAndClose(cancelRecordingMethod);
            else Minecraft.getInstance().setScreen(parent);
        }, Component.translatable("flashback.confirm_cancel_recording"), Component.translatable("flashback.confirm_cancel_recording_description")));
    }

    private static boolean isInReplay() {
        try {
            return (boolean) isInReplayMethod.invoke(null);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private static String controlsLocation() {
        try {
            Object controls = recordingControlsField.get(getConfigMethod.invoke(null));
            return ((Enum<?>) controlsLocationField.get(controls)).name();
        } catch (ReflectiveOperationException exception) {
            return "HIDDEN";
        }
    }

    private static Object recorder() {
        try {
            return recorderField.get(null);
        } catch (IllegalAccessException exception) {
            return null;
        }
    }

    private static boolean isPaused(Object recorder) {
        try {
            return (boolean) recorderIsPausedMethod.invoke(recorder);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private static void invokeAndClose(Method method, Object... arguments) {
        try {
            method.invoke(null, arguments);
            Minecraft.getInstance().setScreen(null);
        } catch (ReflectiveOperationException exception) {
            Legacy4J.LOGGER.error("Unable to run Flashback action", exception);
        }
    }
}
