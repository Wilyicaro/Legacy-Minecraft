package wily.legacy.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.toasts.SystemToast;
//? if >1.20.2 {
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
//?} else {
/*import net.minecraft.client.gui.screens.SymlinkWarningScreen;
*///?}
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.validation.ContentValidationException;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.util.LegacyComponents;

import java.io.IOException;

public class SaveOptionsScreen extends ConfirmationScreen{
    protected final PlayGameScreen parent;
    private LevelSummary summary;

    public SaveOptionsScreen(PlayGameScreen parent, LevelSummary summary) {
        super(parent, 230, 165, LegacyComponents.SAVE_OPTIONS, LegacyComponents.SAVE_OPTIONS_MESSAGE, (b)->{});
        this.parent = parent;
        this.summary = summary;
    }

    @Override
    protected void addButtons() {
        renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), b-> minecraft.setScreen(parent)).bounds(panel.x + 15, panel.y + panel.height - 96,200,20).build());
        EditBox renameBox = new EditBox(font, width / 2 - 100,0,200, 20, Component.translatable("selectWorld.enterName"));
        renameBox.setValue(summary.getLevelName());
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.rename_save"),b-> minecraft.setScreen(new ConfirmationScreen(parent,Component.translatable("legacy.menu.rename_save_title"),Component.translatable("legacy.menu.rename_save_message"), p->{
            String id = summary.getLevelId();
            try {
                LevelStorageSource.LevelStorageAccess levelStorageAccess = minecraft.getLevelSource().validateAndCreateAccess(id);
                levelStorageAccess.renameLevel(renameBox.getValue());
                levelStorageAccess.close();
                parent.saveRenderableList.reloadSaveList();
                minecraft.setScreen(parent);
            } catch (IOException iOException) {
                SystemToast.onWorldAccessFailure(minecraft, id);
                parent.saveRenderableList.reloadSaveList();
            } catch (ContentValidationException contentValidationException) {
                Legacy4J.LOGGER.warn("{}", contentValidationException.getMessage());
                minecraft.setScreen(/*? if >1.20.2 {*/NoticeWithLinkScreen.createWorldSymlinkWarningScreen(()-> minecraft.setScreen(parent))/*?} else {*//*new SymlinkWarningScreen(parent)*//*?}*/);
            }
        }){

            @Override
            protected void init() {
                super.init();
                renameBox.setResponder(s-> okButton.active = !s.isBlank());
                renameBox.setPosition(panel.x + 15,panel.y + 45);
                addRenderableWidget(renameBox);
            }
        })).bounds(panel.x + 15, panel.getRectangle().bottom() - 74,200,20).build());
        renderableVList.addRenderable(Button.builder(Component.translatable("selectWorld.delete"),b-> minecraft.setScreen(new ConfirmationScreen(parent,230,120, Component.translatable("selectWorld.delete"), Component.translatable("selectWorld.deleteQuestion"), b1->parent.saveRenderableList.deleteSave(summary)))).bounds(panel.x + 15, panel.getRectangle().bottom() - 52,200,20).build());

        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.copySave"),b-> minecraft.setScreen(new ConfirmationScreen(parent,230,120, Component.translatable("legacy.menu.copySave"), Component.translatable("legacy.menu.copySaveMessage"), b1->{
            String id = summary.getLevelId();
            Legacy4JClient.copySaveFile(minecraft.getLevelSource().getBaseDir().resolve(id),minecraft.getLevelSource(),id);
            parent.saveRenderableList.reloadSaveList();
            minecraft.setScreen(parent);

        }))).bounds(panel.x + 15, panel.getRectangle().bottom() - 30,200,20).build());
    }
}
