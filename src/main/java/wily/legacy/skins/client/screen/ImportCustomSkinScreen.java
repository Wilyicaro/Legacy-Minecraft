package wily.legacy.skins.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.ConfirmationScreen;
import wily.legacy.client.screen.TickBox;
import wily.legacy.skins.SkinsClientBootstrap;
import wily.legacy.skins.client.preview.PlayerSkinWidget;
import wily.legacy.skins.pose.SkinPoseRegistry;
import wily.legacy.skins.skin.CustomSkinPackStore;
import wily.legacy.skins.skin.SkinPackFiles;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ImportCustomSkinScreen extends ConfirmationScreen {
    private static final Component IMPORT_TITLE = Component.translatable("legacy.menu.import_custom_skin");
    private static final Component IMPORT_MESSAGE = Component.translatable("legacy.menu.import_custom_skin_message");
    private static final Component EDIT_TITLE = Component.translatable("legacy.menu.customize_skin").withStyle(ChatFormatting.BOLD);
    private static final Component NAME = Component.translatable("legacy.menu.custom_skin_name");
    private static final Component THEME = Component.translatable("legacy.menu.custom_skin_theme");
    private static final Component CHOOSE = Component.translatable("legacy.menu.choose_skin_png");
    private static final Component REPLACE = Component.translatable("legacy.menu.replace_skin_png");
    private static final Component CHARACTER_ANIMATIONS = Component.translatable("legacy.menu.character_animations");
    private static final Component SLIM_MODE = Component.translatable("legacy.menu.slim_mode");
    private static final Component CONFIRM = Component.translatable("legacy.menu.confirm");
    private static final String SLIM = "slim";
    private final Screen rootParent;
    private final String packId;
    private final String skinId;
    private final Consumer<String> importedAction;
    private final String initialName;
    private final String initialTheme;
    private final List<String> poseKeys = new ArrayList<>();
    private EditBox nameBox;
    private EditBox themeBox;
    private Button poseButton, skinButton;
    private Path skinPath;
    private boolean openedPicker;
    private boolean closeOnFirstCancel;

    public ImportCustomSkinScreen(Screen parent, Screen rootParent, String packId, Consumer<String> importedAction) {
        this(parent, rootParent, packId, null, importedAction, "", "", List.of());
    }

    private ImportCustomSkinScreen(Screen parent, Screen rootParent, String packId, String skinId, Consumer<String> importedAction, String initialName, String initialTheme, List<String> initialPoses) {
        super(parent, ConfirmationScreen::getPanelWidth, () -> getBaseHeight(skinId), skinId == null ? IMPORT_TITLE : Component.empty(), skinId == null ? IMPORT_MESSAGE : Component.empty(), screen -> {
        });
        this.rootParent = rootParent;
        this.packId = packId;
        this.skinId = skinId;
        this.importedAction = importedAction;
        this.initialName = initialName == null ? "" : initialName;
        this.initialTheme = initialTheme == null ? "" : initialTheme;
        if (initialPoses != null) poseKeys.addAll(initialPoses);
        okAction = screen -> importSkin();
    }

    public static ImportCustomSkinScreen edit(Screen parent, Screen rootParent, String packId, String skinId, String name, String theme, List<String> poses, Consumer<String> savedAction) {
        return new ImportCustomSkinScreen(parent, rootParent, packId, skinId, savedAction, name, theme, poses);
    }

    private static int getBaseHeight(String skinId) {
        boolean sd = LegacyOptions.getUIMode().isSD();
        if (skinId == null) return sd ? 214 : 264;
        return sd ? 232 : 280;
    }

    private static int posePanelHeight() {
        boolean sd = LegacyOptions.getUIMode().isSD();
        return (sd ? 30 : 44) + poseListHeight(sd, sd ? 18 : 20) + (sd ? 6 : 8);
    }

    private static int poseListHeight(boolean sd, int buttonHeight) {
        int spacing = sd ? 1 : 2;
        int toggleCount = SkinPoseRegistry.PoseTag.values().length + 1;
        int totalHeight = toggleCount * TickBox.getDefaultHeight() + Math.max(0, toggleCount - 1) * spacing;
        if (toggleCount > 0) totalHeight += spacing;
        return totalHeight + 2 * buttonHeight + spacing;
    }

    private static void togglePose(LinkedHashSet<String> selected, String poseKey) {
        if (!selected.remove(poseKey)) selected.add(poseKey);
    }

    private static String formatPose(String poseKey) {
        String[] words = poseKey.split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) builder.append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private static String errorText(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.toString() : message;
    }

    private boolean editing() {
        return skinId != null;
    }

    private boolean showPoseButton() {
        return editing();
    }

    private boolean sd() {
        return LegacyOptions.getUIMode().isSD();
    }

    private int fieldHeight() {
        return sd() ? 16 : 20;
    }

    private int fieldX() {
        return panel.x + (panel.width - renderableVList.listWidth) / 2;
    }

    private int titleY() {
        return panel.y + (sd() ? 4 : 10);
    }

    private int formTop() {
        if (editing()) return titleY() + font.lineHeight + (sd() ? 10 : 14);
        return panel.y + messageYOffset.get() + messageLabel.height + (sd() ? 10 : 14);
    }

    private int nameLabelY() {
        return formTop();
    }

    private int nameY() {
        return nameLabelY() + font.lineHeight + (sd() ? 4 : 6);
    }

    private int themeLabelY() {
        return nameY() + fieldHeight() + (sd() ? 8 : 10);
    }

    private int themeY() {
        return themeLabelY() + font.lineHeight + (sd() ? 4 : 6);
    }

    private int poseY() {
        return themeY() + fieldHeight() + (sd() ? 10 : 12);
    }

    private int skinY() {
        return (showPoseButton() ? poseY() + fieldHeight() : themeY() + fieldHeight()) + (sd() ? 6 : 8);
    }

    private int fileY() {
        return skinY() + fieldHeight() + (sd() ? 6 : 8);
    }

    private int formBottomY() {
        return fileY() + font.lineHeight;
    }

    private int buttonsY() {
        return formBottomY() + (sd() ? 8 : 10);
    }

    @Override
    protected void init() {
        super.init();
        if (editing()) addRenderableOnly((guiGraphics, mouseX, mouseY, partialTick) -> renderFormRecess(guiGraphics));
        nameBox = new EditBox(font, fieldX(), nameY(), renderableVList.listWidth, fieldHeight(), NAME);
        nameBox.setMaxLength(64);
        nameBox.setValue(initialName);
        nameBox.setResponder(value -> updateImportButtonStatus());
        addRenderableWidget(nameBox);
        themeBox = new EditBox(font, fieldX(), themeY(), renderableVList.listWidth, fieldHeight(), THEME);
        themeBox.setMaxLength(64);
        themeBox.setValue(initialTheme);
        addRenderableWidget(themeBox);
        if (showPoseButton()) {
            poseButton = addRenderableWidget(Button.builder(CHARACTER_ANIMATIONS, button -> openPoseScreen())
                    .bounds(fieldX(), poseY(), renderableVList.listWidth, fieldHeight())
                    .build());
        }
        skinButton = addRenderableWidget(Button.builder(skinId == null ? CHOOSE : REPLACE, button -> browseForSkin())
                .bounds(fieldX(), skinY(), renderableVList.listWidth, fieldHeight())
                .build());
        setInitialFocus(nameBox);
        updateImportButtonStatus();
        if (skinId == null && !openedPicker && minecraft != null) {
            openedPicker = true;
            closeOnFirstCancel = true;
            minecraft.execute(this::browseForSkin);
        }
    }

    @Override
    public void repositionElements() {
        String name = nameBox == null ? "" : nameBox.getValue();
        String theme = themeBox == null ? "" : themeBox.getValue();
        super.repositionElements();
        if (nameBox != null) nameBox.setValue(name);
        if (themeBox != null) themeBox.setValue(theme);
        if (poseButton != null) poseButton.setMessage(CHARACTER_ANIMATIONS);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        return super.keyPressed(keyEvent);
    }

    @Override
    public void renderableVListInit() {
        messageYOffset.set(title.getString().isEmpty() ? (sd() ? 6 : 15) : (sd() ? 18 : 35));
        int listWidth = LegacyOptions.getUIMode().isSD() ? panel.width - 12 : panel.width - 30;
        int y = buttonsY();
        renderableVList.init(panel.x + (panel.width - listWidth) / 2, y, listWidth, 0);
    }

    @Override
    protected void addButtons() {
        renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), button -> onClose()).build());
        renderableVList.addRenderable(okButton = Button.builder(skinId == null ? Component.translatable("legacy.menu.import_skin") : CONFIRM, button -> okAction.accept(this)).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (skinButton == null) return;
        int textX = fieldX();
        int fileValueY = fileY();
        String fileText = skinPath == null ? null : PlayerSkinWidget.clipText(font, skinPath.getFileName().toString(), renderableVList.listWidth);
        LegacyFontUtil.applySDFont(ignored -> {
            if (editing())
                guiGraphics.drawString(font, EDIT_TITLE, textX, titleY(), CommonColor.GRAY_TEXT.get(), false);
            guiGraphics.drawString(font, NAME, textX, nameLabelY(), CommonColor.GRAY_TEXT.get(), false);
            guiGraphics.drawString(font, THEME, textX, themeLabelY(), CommonColor.GRAY_TEXT.get(), false);
            if (fileText != null)
                guiGraphics.drawString(font, fileText, textX, fileValueY, CommonColor.GRAY_TEXT.get(), false);
        });
    }

    private void renderFormRecess(GuiGraphics guiGraphics) {
        if (nameBox == null || themeBox == null || skinButton == null) return;
        int insetX = sd() ? 4 : 6;
        int insetTop = sd() ? 8 : 10;
        int insetBottom = sd() ? 8 : 10;
        int x = fieldX() - insetX;
        int y = nameLabelY() - insetTop;
        int width = renderableVList.listWidth + insetX * 2;
        int height = formBottomY() + insetBottom - y;
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL_RECESS, x, y, width, height);
    }

    private void browseForSkin() {
        try {
            Path selected = SkinPackFiles.choosePng(minecraft, CHOOSE.getString());
            if (selected == null) {
                if (closeOnFirstCancel && skinPath == null) onClose();
                closeOnFirstCancel = false;
                return;
            }
            closeOnFirstCancel = false;
            skinPath = selected;
            if (nameBox != null && nameBox.getValue().trim().isEmpty()) {
                String fileName = selected.getFileName() == null ? "" : selected.getFileName().toString();
                int dot = fileName.lastIndexOf('.');
                nameBox.setValue(dot > 0 ? fileName.substring(0, dot) : fileName);
            }
            updateImportButtonStatus();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void importSkin() {
        if (minecraft == null || nameBox == null) return;
        try {
            String savedSkinId = skinId == null
                    ? CustomSkinPackStore.importSkin(minecraft, packId, nameBox.getValue(), themeBox == null ? "" : themeBox.getValue(), List.copyOf(poseKeys), skinPath)
                    : skinId;
            if (skinId != null) {
                CustomSkinPackStore.updateSkin(minecraft, packId, skinId, nameBox.getValue(), themeBox == null ? "" : themeBox.getValue(), List.copyOf(poseKeys), skinPath);
            }
            if (importedAction != null) {
                importedAction.accept(savedSkinId);
                minecraft.setScreen(parent);
                return;
            }
            CustomSkinPackStore.enableResourcePack(minecraft);
            SkinsClientBootstrap.reloadChangeSkinScreen(minecraft, rootParent == null ? parent : rootParent, packId, savedSkinId);
        } catch (IOException ex) {
            showError(ex);
        }
    }

    private void openPoseScreen() {
        if (minecraft == null) return;
        LinkedHashSet<String> selected = new LinkedHashSet<>(poseKeys);
        minecraft.setScreen(new ConfirmationScreen(this, ConfirmationScreen::getPanelWidth, ImportCustomSkinScreen::posePanelHeight, CHARACTER_ANIMATIONS, Component.empty(), screen -> {
        }) {
            @Override
            protected void addButtons() {
                int listWidth = LegacyOptions.getUIMode().isSD() ? ConfirmationScreen.getPanelWidth() - 12 : ConfirmationScreen.getPanelWidth() - 30;
                renderableVList.addRenderable(new TickBox(0, 0, listWidth, selected.contains(SLIM), value -> SLIM_MODE, value -> null, value -> togglePose(selected, SLIM)));
                for (SkinPoseRegistry.PoseTag pose : SkinPoseRegistry.PoseTag.values()) {
                    String poseKey = pose.name().toLowerCase(Locale.ROOT);
                    renderableVList.addRenderable(new TickBox(0, 0, listWidth, selected.contains(poseKey), value -> Component.literal(formatPose(poseKey)), value -> null, value -> togglePose(selected, poseKey)));
                }
                renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), value -> onClose()).build());
                renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.done"), value -> {
                    poseKeys.clear();
                    if (selected.contains(SLIM)) poseKeys.add(SLIM);
                    for (String poseKey : selected) {
                        if (!SLIM.equals(poseKey)) poseKeys.add(poseKey);
                    }
                    if (poseButton != null) poseButton.setMessage(CHARACTER_ANIMATIONS);
                    if (minecraft != null) minecraft.setScreen(parent);
                }).build());
            }

            @Override
            public void renderableVListInit() {
                boolean sd = LegacyOptions.getUIMode().isSD();
                int listWidth = sd ? panel.width - 12 : panel.width - 30;
                int totalHeight = poseListHeight(sd, accessor.getInteger("buttonsHeight", sd ? 18 : 20));
                messageYOffset.set(sd ? 18 : 35);
                renderableVList.init(panel.x + (panel.width - listWidth) / 2, panel.y + panel.height - totalHeight - (sd ? 6 : 8), listWidth, 0);
            }
        });
    }

    private void updateImportButtonStatus() {
        if (okButton == null || nameBox == null) return;
        if (skinId == null) {
            okButton.active = skinPath != null && !nameBox.getValue().trim().isEmpty();
            return;
        }
        okButton.active = !nameBox.getValue().trim().isEmpty();
    }

    private void showError(Exception ex) {
        if (minecraft == null) return;
        minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, editing() ? EDIT_TITLE : title, Component.literal(errorText(ex))));
    }
}
