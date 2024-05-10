
package wily.legacy.client.screen;

import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import wily.legacy.util.ScreenUtil;

@Environment(EnvType.CLIENT)
public class LegacyDeathScreen extends DeathScreen {
    private static final ResourceLocation DRAFT_REPORT = new ResourceLocation("icon/draft_report");
    private int delayTicker;
    private final Component causeOfDeath;
    private final boolean hardcore;
    private final List<Button> exitButtons = Lists.newArrayList();
    @Nullable
    private Button exitToTitleButton;

    public LegacyDeathScreen(@Nullable Component component, boolean bl) {
        super(component,bl);
        this.causeOfDeath = component;
        this.hardcore = bl;
    }

    protected void init() {
        this.delayTicker = 0;
        this.exitButtons.clear();
        Component component = this.hardcore ? Component.translatable("deathScreen.spectate") : Component.translatable("deathScreen.respawn");
        this.exitButtons.add(this.addRenderableWidget(Button.builder(component, (button) -> {
            this.minecraft.player.respawn();
            button.active = false;
        }).bounds(this.width / 2 - 100, this.height / 4 + 72, 200, 20).build()));
        this.exitToTitleButton = this.addRenderableWidget(Button.builder(Component.translatable("menu.quit"), (button) -> {
            this.minecraft.getReportingContext().draftReportHandled(this.minecraft, this, this::handleExitToTitleScreen, true);
        }).bounds(this.width / 2 - 100, this.height / 4 + 96, 200, 20).build());
        this.exitButtons.add(this.exitToTitleButton);
        this.setButtonsActive(false);
    }

    private void handleExitToTitleScreen() {
        if (this.hardcore) {
            ExitConfirmationScreen.exit(minecraft,true);
        } else {
            this.minecraft.setScreen(new ExitConfirmationScreen(this));
        }
    }


    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBackground(guiGraphics,i,j,f);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(2.0F, 2.0F, 2.0F);
        ScreenUtil.drawOutlinedString(guiGraphics,this.font, this.title, (this.width - font.width(title) * 2) / 4, 40, 16777215,0,1f);
        guiGraphics.pose().popPose();
        if (this.causeOfDeath != null) {
            guiGraphics.drawCenteredString(this.font, this.causeOfDeath, this.width / 2, 110, 16777215);
        }
        if (this.causeOfDeath != null && j > 85) {
            Objects.requireNonNull(this.font);
            if (j < 85 + 9) {
                Style style = this.getClickedComponentStyleAt(i);
                guiGraphics.renderComponentHoverEffect(this.font, style, i, j);
            }
        }

        if (this.exitToTitleButton != null && this.minecraft.getReportingContext().hasDraftReport()) {
            guiGraphics.blitSprite(DRAFT_REPORT, this.exitToTitleButton.getX() + this.exitToTitleButton.getWidth() - 17, this.exitToTitleButton.getY() + 3, 15, 15);
        }
        for (Renderable renderable : this.renderables)
            renderable.render(guiGraphics, i, j, f);
    }


    @Nullable
    private Style getClickedComponentStyleAt(int i) {
        if (this.causeOfDeath == null) {
            return null;
        } else {
            int j = this.minecraft.font.width(this.causeOfDeath);
            int k = this.width / 2 - j / 2;
            int l = this.width / 2 + j / 2;
            return i >= k && i <= l ? this.minecraft.font.getSplitter().componentStyleAtWidth(this.causeOfDeath, i - k) : null;
        }
    }


    public void tick() {
        super.tick();
        ++this.delayTicker;
        if (this.delayTicker == 20) {
            this.setButtonsActive(true);
        }

    }

    private void setButtonsActive(boolean bl) {
        Button button;
        for(Iterator var2 = this.exitButtons.iterator(); var2.hasNext(); button.active = bl) {
            button = (Button)var2.next();
        }
    }

}
