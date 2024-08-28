package wily.legacy.client.screen.compat;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostComponents;
import io.github.gaming32.worldhost.gui.screen.AddFriendScreen;
import io.github.gaming32.worldhost.gui.screen.FriendsScreen;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.client.screen.*;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

public class WorldHostFriendsScreen extends PanelVListScreen {
    public static final Component ADD_SILENTLY_TEXT = Components.translatable("world-host.friends.add_silently");
    public static final Component INVITE = Component.translatable("legacy.menu.invite");
    public static final Component INVITE_FRIENDS = Component.translatable("legacy.menu.invite_friends");
    public static final Component WORLD_HOST_FRIENDS = WorldHostComponents.FRIENDS;
    public static final Component WORLD_HOST_REMOVE_FRIEND = Components.translatable("world-host.friends.remove");
    public static final Component WORLD_HOST_FRIEND_USERNAME_TEXT = Components.translatable("world-host.add_friend.enter_username");
    public WorldHostFriendsScreen(Screen parent) {
        super(parent, 250,190, WORLD_HOST_FRIENDS);
        renderableVList.layoutSpacing(i->0);
        addFriendButtons(()->{});
        panel.panelSprite = LegacySprites.PANEL;
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.set(0,()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(), ()-> INVITE);
        renderer.add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(), ()-> FriendsScreen.ADD_FRIEND_TEXT);
        renderer.add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(), ()-> getFocused() == null ? null : WORLD_HOST_REMOVE_FRIEND);
    }

    public void reloadFriendButtons(){
        int i = renderableVList.renderables.indexOf(getFocused());
        renderableVList.renderables.clear();
        addFriendButtons(()-> {
            if (i >= 0 &&  i < renderableVList.renderables.size()) setFocused((GuiEventListener) renderableVList.renderables.get(i));
        });
    }

    @Override
    protected void init() {
        panel.init();
        renderableVList.init(this,panel.x + 10,panel.y + 22,panel.width - 20,panel.height - 8);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == InputConstants.KEY_O){
            TickBox silentAddBox = new TickBox(0,0,200,16,false, b-> ADD_SILENTLY_TEXT, b-> null, t-> {});
            EditBox friendBox = new EditBox(font, width / 2 - 100,0,200, 20, WORLD_HOST_FRIEND_USERNAME_TEXT);
            minecraft.setScreen(new ConfirmationScreen(this,230,140,FriendsScreen.ADD_FRIEND_TEXT,WORLD_HOST_FRIEND_USERNAME_TEXT, b->{}){
                long lastTyping = -1;
                GameProfile friend;
                @Override
                protected void init() {
                    okAction = b-> {
                        if (friend != null) {
                            FriendsScreen.addFriend(friend);
                            if (!silentAddBox.selected && WorldHost.protoClient != null) WorldHost.protoClient.friendRequest(friend.getId());

                            reloadFriendButtons();
                        }
                        return true;
                    };
                    super.init();
                    friendBox.setPosition(panel.x + 15,panel.y + 45);
                    okButton.active = friend != null;
                    friendBox.setMaxLength(36);
                    friendBox.setResponder(s->{
                        if (AddFriendScreen.VALID_USERNAME.matcher(s).matches()) {
                            lastTyping = Util.getMillis();
                            friend = null;
                            okButton.active = false;
                        } else if (AddFriendScreen.VALID_UUID.matcher(s).matches()) {
                            friend = new GameProfile(UUID.fromString(s), "");
                            okButton.active = true;
                        } else if (s.startsWith("o:")) {
                            final String actualName = s.substring(2);
                            friend = new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + actualName).getBytes(StandardCharsets.UTF_8)), actualName);
                            okButton.active = true;
                        }
                    });
                    addRenderableWidget(friendBox);
                    silentAddBox.setPosition(panel.getX() + (panel.getWidth() - 200) / 2, panel.y + panel.height - 68);
                    addRenderableWidget(silentAddBox);

                }

                @Override
                public void render(PoseStack poseStack, int i, int j, float f) {
                    super.render(poseStack, i, j, f);
                    LegacyGuiGraphics.of(poseStack).blitSprite(LegacySprites.SQUARE_ENTITY_PANEL,panel.getX() + panel.getWidth() - 47, panel.getY() + 9, 32,32);
                    if (friend != null){
                        poseStack.pose().pushPose();
                        poseStack.pose().translate(1.5f,1.5f,0);
                        PlayerFaceRenderer.draw(poseStack, minecraft.getSkinManager().getInsecureSkinLocation(friend), panel.getX() + panel.getWidth() - 47, panel.getY() + 9, 29);
                        poseStack.pose().popPose();
                    }
                }

                @Override
                public void tick() {
                    if (lastTyping != -1 && Util.getMillis() - 300 > lastTyping) {
                        lastTyping = -1;
                        final String username = friendBox.getValue();
                        WorldHost.getMaybeAsync(WorldHost.getProfileCache(), username, p -> {
                            if (p.isPresent()) {
                                assert minecraft != null;
                                friend = WorldHost.fetchProfile(minecraft.getMinecraftSessionService(), p.get());
                                okButton.active = true;
                            } else
                                friend = null;
                        });
                    }
                }
            });
            return true;
        }
        return super.keyPressed(i, j, k);
    }

    protected void addFriendButtons(Runnable afterButtonsAdd){
        if (WorldHost.CONFIG.getFriends().isEmpty()) return;
        Minecraft minecraft =  Minecraft.getInstance();
        Util.backgroundExecutor().execute(() -> {
            for (UUID uuid : WorldHost.CONFIG.getFriends()) {
                GameProfile profile = WorldHost.fetchProfile(minecraft.getMinecraftSessionService(),uuid);
                Supplier<ResourceLocation> skin = ()-> minecraft.getSkinManager().getInsecureSkinLocation(profile);
                skin.get();
                renderableVList.addRenderable(new AbstractButton(0, 0, 230, 30, Component.literal(WorldHost.getName(profile))) {
                    @Override
                    protected void renderWidget(PoseStack poseStack, int i, int j, float f) {
                        super.renderWidget(poseStack, i, j, f);
                        PlayerFaceRenderer.draw(poseStack, skin.get(), getX() + 5, getY() + 5, 20);
                    }

                    @Override
                    protected void renderScrollingString(PoseStack poseStack, Font font, int i, int j) {
                        ScreenUtil.renderScrollingString(poseStack, font, this.getMessage(), getX() + 30, this.getY(), getX() + getWidth() - 2, this.getY() + this.getHeight(), j, true);
                    }

                    @Override
                    public boolean keyPressed(int i, int j, int k) {
                        if (i == InputConstants.KEY_X) {
                            minecraft.setScreen(new ConfirmationScreen(WorldHostFriendsScreen.this, WORLD_HOST_REMOVE_FRIEND, Components.translatable("world-host.friends.remove.title"), b -> {
                                WorldHost.CONFIG.getFriends().remove(profile.getId());
                                WorldHost.saveConfig();
                                reloadFriendButtons();
                                if (minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer().isPublished() && WorldHost.protoClient != null) {
                                    WorldHost.protoClient.closedWorld(Collections.singleton(profile.getId()));
                                }
                                minecraft.setScreen(WorldHostFriendsScreen.this);
                            }));
                            return true;
                        }
                        return super.keyPressed(i, j, k);
                    }

                    @Override
                    public void onPress() {
                        if (WorldHost.protoClient != null) WorldHost.protoClient.friendRequest(profile.getId());
                    }

                    @Override
                    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                        defaultButtonNarrationText(narrationElementOutput);
                    }
                });
            }
            repositionElements();
            afterButtonsAdd.run();
        });

    }
    @Override
    public void renderDefaultBackground(PoseStack poseStack, int i, int j, float f) {
        panel.render(poseStack,i,j,f);
        poseStack.drawString(font,getTitle(),panel.x + 11, panel.y + 8, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
    }
}
