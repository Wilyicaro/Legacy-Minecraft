package wily.legacy.client.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.validation.ContentValidationException;
import wily.legacy.LegacyMinecraft;

import java.io.IOException;

public class SaveOptionsScreen extends ConfirmationScreen{
    protected final PlayGameScreen parent = (PlayGameScreen) super.parent;
    private LevelSummary summary;

    public SaveOptionsScreen(PlayGameScreen parent, LevelSummary summary) {
        super(parent, 230, 143, Component.translatable("legacy.menu.save_options"), Component.translatable("legacy.menu.save_options_message"), (b)->{});
        this.summary = summary;
    }

    @Override
    protected void initButtons() {
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b-> this.minecraft.setScreen(parent)).bounds(panel.x + 15, panel.y + panel.height - 74,200,20).build());
        EditBox renameBox = new EditBox(font, width / 2 - 100,0,200, 20, Component.translatable("selectWorld.enterName"));
        addRenderableWidget(Button.builder(Component.translatable("legacy.menu.rename_save"),b-> minecraft.setScreen(new ConfirmationScreen(parent,Component.translatable("legacy.menu.rename_save_title"),Component.translatable("legacy.menu.rename_save_message"),p->{
            String id = summary.getLevelId();
            try {
                LevelStorageSource.LevelStorageAccess levelStorageAccess = this.minecraft.getLevelSource().validateAndCreateAccess(id);
                levelStorageAccess.renameLevel(renameBox.getValue());
                levelStorageAccess.close();
                parent.saveSelectionList.reloadSaveList();
                minecraft.setScreen(parent);
            } catch (IOException iOException) {
                SystemToast.onWorldAccessFailure(this.minecraft, id);
                parent.saveSelectionList.reloadSaveList();
            } catch (ContentValidationException contentValidationException) {
                LegacyMinecraft.LOGGER.warn("{}", contentValidationException.getMessage());
                this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(parent));
            }
        }){
            @Override
            protected void init() {
                super.init();
                renameBox.setResponder(s-> okButton.active = !Util.isBlank(s));
                renameBox.setY(panel.y + 45);
                renameBox.setValue(summary.getLevelName());
                addRenderableWidget(renameBox);
            }
        })).bounds(panel.x + 15, panel.getRectangle().bottom() - 52,200,20).build());
        addRenderableWidget(Button.builder(Component.translatable("selectWorld.delete"),b-> minecraft.setScreen(new ConfirmationScreen(parent,230,120, Component.translatable("selectWorld.delete"), Component.translatable("selectWorld.deleteQuestion"), b1->parent.saveSelectionList.deleteSave(summary)))).bounds(panel.x + 15, panel.getRectangle().bottom() - 30,200,20).build());
    }
}
