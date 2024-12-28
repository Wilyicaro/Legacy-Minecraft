package wily.legacy.client.screen.compat;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostComponents;
import io.github.gaming32.worldhost.gui.screen.AddFriendScreen;
import io.github.gaming32.worldhost.gui.screen.FriendsScreen;
import io.github.gaming32.worldhost.gui.widget.UserListWidget;
import io.github.gaming32.worldhost.plugin.FriendAdder;
import io.github.gaming32.worldhost.plugin.FriendListFriend;
import io.github.gaming32.worldhost.plugin.ProfileInfo;
import io.github.gaming32.worldhost.plugin.vanilla.WorldHostFriendListFriend;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.Legacy4J;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.screen.*;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.*;

public class WorldHostFriendsScreen extends PanelVListScreen {
    public static final Tooltip ADD_SILENTLY_TEXT_TOOLTIP = Tooltip.create(Component.translatable("world-host.friends.add_silently.tooltip"));
    public static final Component INVITE = Component.translatable("legacy.menu.invite");
    public static final Component INVITE_FRIENDS = Component.translatable("legacy.menu.invite_friends");
    public static final Component FRIENDS = Component.translatable("world-host.friends");
    public static final Component WORLD_HOST_FRIENDS = WorldHostComponents.FRIENDS;
    public static final Component WORLD_HOST_REMOVE_FRIEND = Component.translatable("world-host.friends.remove");
    public static final Component WORLD_HOST_FRIEND_USERNAME_TEXT = Component.translatable("world-host.add_friend.enter_username");

    public WorldHostFriendsScreen(Screen parent) {
        this(parent, 250,190);
    }
    public WorldHostFriendsScreen(Screen parent, int imageWidth, int imageHeight) {
        super(parent, s-> Panel.centered(s, LegacySprites.PANEL,imageWidth,imageHeight), WORLD_HOST_FRIENDS);
        renderableVList.layoutSpacing(i->0);
        addFriendButtons(()->{});
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(), ()-> ControlTooltip.getKeyMessage(InputConstants.KEY_X,this));
    }

    public void reloadFriendButtons(){
        int i = renderableVList.renderables.indexOf(getFocused());
        renderableVList.renderables.clear();
        addFriendButtons(()-> {
            if (i >= 0 &&  i < renderableVList.renderables.size()) setFocused((GuiEventListener) renderableVList.renderables.get(i));
            repositionElements();
        });
    }

    @Override
    public void renderableVListInit() {
        renderableVList.init(panel.x + 10,panel.y + 22,panel.width - 20,panel.height - 28);
    }

    @Override
    protected void panelInit() {
        super.panelInit();
        addRenderableOnly((guiGraphics, i, j, f) ->  guiGraphics.drawString(font,getTitle(),panel.x + 11, panel.y + panel.height - 182, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
    }

    protected void addFriendButtons(Runnable afterButtonsAdd){
        CreationList.addIconButton(renderableVList, Legacy4J.createModLocation("icon/add_user_portal"),FriendsScreen.ADD_FRIEND_TEXT,b-> minecraft.setScreen(new WorldHostFriendsScreen(this,250,234){
            long lastTyping = -1;
            private Runnable delayedLookup;
            final List<FriendListFriend> addableFriends = new ArrayList<>();
            final List<FriendListFriend> friendsToAdd = new ArrayList<>();
            final TickBox silentAddBox = new TickBox(0,0,200,16,false, b1-> AddFriendScreen.ADD_FRIEND_SILENT_TEXT, b1-> ADD_SILENTLY_TEXT_TOOLTIP, t-> {});
            final EditBox friendBox = new EditBox(Minecraft.getInstance().font, width / 2 - 100,0,200, 20, WORLD_HOST_FRIEND_USERNAME_TEXT);
            final List<FriendAdder> friendAdders = WorldHost.getFriendAdders();
            @Override
            protected void addFriendButtons(Runnable afterButtonsAdd) {
                if (addableFriends == null) return;
                addableFriends.forEach(this::addFriendButton);
                friendsToAdd.forEach(this::addFriendButton);
                afterButtonsAdd.run();
            }

            private void addFriendButton(FriendListFriend friend){
                renderableVList.addRenderable(new FriendButton(0,0,230,30,friend){
                    @Override
                    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                        super.renderWidget(guiGraphics, i, j, f);
                        RenderSystem.enableBlend();
                        FactoryGuiGraphics.of(guiGraphics).blitSprite(TickBox.SPRITES[isHoveredOrFocused() ? 1 : 0], this.getX() + 30, this.getY() + (height - 12) / 2, 12, 12);
                        if (friendsToAdd.contains(friend)) FactoryGuiGraphics.of(guiGraphics).blitSprite(TickBox.TICK, this.getX() + 30, this.getY()  + (height - 12) / 2, 14, 12);
                        RenderSystem.disableBlend();
                    }
                    @Override
                    protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                        ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), getX() + 45, this.getY(), getX() + getWidth() - 2, this.getY() + this.getHeight(), j, true);
                    }
                    @Override
                    public void onPress() {
                        if (friendsToAdd.contains(friend)) {
                            friendsToAdd.remove(friend);
                            addableFriends.add(friend);
                        } else {
                            friendsToAdd.add(friend);
                            addableFriends.remove(friend);
                        }
                        reloadFriendButtons();
                    }

                    @Override
                    public boolean supportsRemoving() {
                        return false;
                    }
                });
            }

            @Override
            public void onClose() {
                friendsToAdd.forEach(f->f.addFriend(!silentAddBox.selected, WorldHostFriendsScreen.this::reloadFriendButtons));
                super.onClose();
            }

            @Override
            protected void init() {
                super.init();
                friendBox.setPosition(panel.x + (panel.getWidth() - friendBox.getWidth()) / 2,panel.y + 5);
                friendBox.setMaxLength(36);
                friendBox.setResponder(s->{
                    lastTyping = Util.getMillis();
                    addableFriends.clear();
                    final List<FriendAdder> delayedAdders = new ArrayList<>();
                    for (final FriendAdder adder : friendAdders) {
                        if (s.length() > adder.maxValidNameLength()) continue;
                        if (adder.delayLookup(s)) {
                            delayedAdders.add(adder);
                        } else adder.searchFriends(s,16- addableFriends.size(), f->{
                            if (friendsToAdd.stream().anyMatch(f1->f1.fallbackProfileInfo().name().equals(f.fallbackProfileInfo().name()))) return;
                            addableFriends.add(f);
                            this.reloadFriendButtons();
                        });
                    }
                    delayedLookup = delayedAdders.isEmpty() ? null : ()-> delayedAdders.forEach(adder->
                            adder.searchFriends(s,16- addableFriends.size(), f->{
                                if (friendsToAdd.stream().anyMatch(f1->f1.fallbackProfileInfo().name().equals(f.fallbackProfileInfo().name()))) return;
                                addableFriends.add(f);
                                this.reloadFriendButtons();
                            }));
                });
                addRenderableWidget(friendBox);
                silentAddBox.setPosition(panel.getX() + (panel.getWidth() - 200) / 2, panel.y + 30);
                addRenderableWidget(silentAddBox);

            }
            @Override
            public void tick() {
                if (lastTyping != -1 && Util.getMillis() - 300 > lastTyping) {
                    lastTyping = -1;
                    delayedLookup.run();
                    delayedLookup = null;
                }
            }
            @Override
            public void renderableVListInit() {
                renderableVList.init(panel.x + 10,panel.y + 66,panel.width - 20,panel.height - 72);
            }

        }));
        WorldHost.getPlugins().forEach(p-> p.plugin().listFriends(f-> renderableVList.addRenderable(new FriendButton(0, 0, 230, 30, f))));
        afterButtonsAdd.run();
    }

    public class FriendButton extends AbstractButton implements ControlTooltip.ActionHolder {
        public final FriendListFriend friend;
        public ProfileInfo profileInfo;

        public FriendButton(int x, int y, int width, int height, FriendListFriend friend) {
            super(x, y, width, height, Component.empty());
            this.friend = friend;
            this.profileInfo = friend.fallbackProfileInfo();
            friend.profileInfo()
                    .thenAcceptAsync(ready -> profileInfo = ready, Minecraft.getInstance())
                    .exceptionally(t -> {
                        WorldHost.LOGGER.error("Failed to request profile info for {}", friend, t);
                        return null;
                    });
        }

        @Override
        public Component getMessage() {
            return UserListWidget.getNameWithTag(friend,profileInfo);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
            super.renderWidget(guiGraphics, i, j, f);
            profileInfo.iconRenderer().draw(guiGraphics,getX() + 5, getY() + 5, 20,20);
        }

        @Override
        protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
            ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), getX() + 30, this.getY(), getX() + getWidth() - 2, this.getY() + this.getHeight(), j, true);
        }

        @Override
        public boolean keyPressed(int i, int j, int k) {
            if (i == InputConstants.KEY_X) {
                minecraft.setScreen(new ConfirmationScreen(WorldHostFriendsScreen.this, WORLD_HOST_REMOVE_FRIEND, Component.translatable("world-host.friends.remove.title"), b -> {
                    friend.removeFriend(()->{
                        minecraft.setScreen(WorldHostFriendsScreen.this);
                        reloadFriendButtons();
                    });
                }));
                return true;
            }
            return super.keyPressed(i, j, k);
        }

        @Override
        public void onPress() {
            friend.showFriendInfo(WorldHostFriendsScreen.this);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }

        public boolean supportsRemoving(){
            return true;
        }

        @Override
        public @Nullable Component getAction(Context context) {
            return context.actionOfContext(KeyContext.class,k->isFocused() && k.key() == InputConstants.KEY_X && supportsRemoving() ? WORLD_HOST_REMOVE_FRIEND : ControlTooltip.getSelectAction(this,context));
        }
    }

}
