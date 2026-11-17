package wily.legacy.client.control;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import wily.legacy.Legacy4J;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

public interface ControllerHandler {
    Component DOWNLOAD_MESSAGE = Component.translatable("legacy.menu.download_natives_message");
    Component DOWNLOADING_NATIVES = Component.translatable("legacy.menu.downloading_natives");
    Component LOADING_NATIVES = Component.translatable("legacy.menu.loading_natives");
    ControllerHandler EMPTY = new ControllerHandler() {
        @Override
        public Component getName() {
            return CommonComponents.OPTION_OFF;
        }

        @Override
        public void init() {
        }

        @Override
        public boolean update() {
            return false;
        }

        @Override
        public void setup(ControllerManager manager) {
        }

        @Override
        public Controller getController(int jid) {
            return null;
        }

        @Override
        public boolean isValidController(int jid) {
            return false;
        }

        @Override
        public int getButtonIndex(ControllerBinding.Button button) {
            return -1;
        }

        @Override
        public int getAxisIndex(ControllerBinding.Axis axis) {
            return -1;
        }

        @Override
        public void applyGamePadMappingsFromBuffer(BufferedReader reader) {
        }
    };

    /**
     * @return Controller Handler display name
     */
    Component getName();

    /**
     * Starts the downloading and loading of the game pad mappings and the library if needed
     */
    void init();

    boolean update();

    /**
     * Manages the connected controller bindings
     *
     * @param manager Controller Manager instance
     */
    default void setup(ControllerManager manager) {
        manager.connectedController.manageBindings(manager::updateBindings);
    }

    /**
     * @param jid Controller ID, generally based on the connection order
     * @return The controller corresponding to this ID, or null if it's invalid
     */
    Controller getController(int jid);

    /**
     * @param jid Controller ID, generally based on the connection order
     * @return If this ID corresponds to a valid controller
     */
    boolean isValidController(int jid);

    /**
     * @param button {@link ControllerBinding.Button} to convert to a button index used by this Controller Handler
     * @return Button index used by this Controller Handler
     */
    int getButtonIndex(ControllerBinding.Button button);

    /**
     * @param axis {@link ControllerBinding.Axis} to convert to an axis index used by this Controller Handler
     * @return Axis index used by this Controller Handler
     */
    int getAxisIndex(ControllerBinding.Axis axis);

    void applyGamePadMappingsFromBuffer(BufferedReader reader) throws IOException;

    default void tryDownloadAndApplyNewMappings() {
        try {
            applyGamePadMappingsFromBuffer(new BufferedReader(new InputStreamReader(URI.create("https://raw.githubusercontent.com/mdqinc/SDL_GameControllerDB/master/gamecontrollerdb.txt").toURL().openStream())));
        } catch (IOException e) {
            Legacy4J.LOGGER.warn(e.getMessage());
        }
    }
}
