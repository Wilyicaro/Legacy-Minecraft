package wily.legacy.client.screen.compat;


import wily.factoryapi.FactoryAPIClient;

public class ModMenuCompat {
    public static void init() {
        FactoryAPIClient.SECURE_EXECUTOR.executeWhen(() -> {
            setModifyTitleScreen(false);
            return false;
        });
    }

    private static void setModifyTitleScreen(boolean value) {
        try {
            Class<?> config = Class.forName("com.terraformersmc.modmenu.config.ModMenuConfig");
            Object option = config.getField("MODIFY_TITLE_SCREEN").get(null);
            option.getClass().getMethod("setValue", boolean.class).invoke(option, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
