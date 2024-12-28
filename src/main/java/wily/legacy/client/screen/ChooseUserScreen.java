package wily.legacy.client.screen;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.Legacy4J;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.MCAccount;
import wily.legacy.util.ScreenUtil;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class ChooseUserScreen extends PanelVListScreen {
    public static final Component CHOOSE_USER = Component.translatable("legacy.menu.choose_user");
    public static final Component CHOOSE_USER_MESSAGE = Component.translatable("legacy.menu.choose_user_message");
    public static final Component ADD_ACCOUNT = Component.translatable("legacy.menu.choose_user.add");
    public static final Component ACCOUNT_OPTIONS = Component.translatable("legacy.menu.choose_user.account_options");
    public static final Component DIRECT_LOGIN = Component.translatable("legacy.menu.choose_user.direct_login");
    public static final Component ACCOUNT_OPTIONS_MESSAGE = Component.translatable("legacy.menu.choose_user.account_options_message");
    public static final Component EDIT_ACCOUNT = Component.translatable("legacy.menu.choose_user.account_options.edit");
    public static final Component DELETE_ACCOUNT = Component.translatable("legacy.menu.choose_user.account_options.delete");
    public static final Component ACCOUNT_ENCRYPTION = Component.translatable("legacy.menu.choose_user.add.encryption");
    public static final Component ACCOUNT_ENCRYPTION_MESSAGE = Component.translatable("legacy.menu.choose_user.add.encryption_message");
    public static final Component ADD_WITH_ENCRYPTION = Component.translatable("legacy.menu.choose_user.add.encryption.present");
    public static final Component ADD_WITHOUT_ENCRYPTION = Component.translatable("legacy.menu.choose_user.add.encryption.absent");
    public static final Component VISIBLE_PASSWORD = Component.translatable("legacy.menu.choose_user.add.encryption.visible_password");
    public static final Pattern usernamePattern = Pattern.compile("[A-Za-z0-9_]{2,16}");

    public ChooseUserScreen(Screen parent) {
        super(parent, 260,190, CHOOSE_USER);
        renderableVList.layoutSpacing(i->0);
        addAccountButtons();
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.add(()->getFocused() == null || renderableVList.renderables.indexOf(getFocused()) <= 0 ? null : ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(), ()-> ACCOUNT_OPTIONS);
        renderer.add(()->ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(), ()-> DIRECT_LOGIN);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == InputConstants.KEY_X){
            minecraft.setScreen(accountScreen(DIRECT_LOGIN,this,false, this::manageLogin));
            return true;
        }
        return super.keyPressed(i, j, k);
    }

    public void reloadAccountButtons(){
        int i = renderableVList.renderables.indexOf(getFocused());
        renderableVList.renderables.clear();
        addAccountButtons();
        repositionElements();
        if (i >= 0 &&  i < renderableVList.renderables.size()) setFocused((GuiEventListener) renderableVList.renderables.get(i));
    }

    @Override
    public void renderableVListInit() {
        addRenderableOnly(((guiGraphics, i, j, f) -> {
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL_RECESS,panel.getX() + 10, panel.getY() + 13, panel.getWidth() - 20, panel.getHeight() - 26);
            guiGraphics.drawString(font,getTitle(),panel.getX() + (panel.getWidth() - font.width(getTitle())) / 2, panel.y + 20, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        }));
        renderableVList.init(panel.x + 15,panel.y + 32,panel.width - 30,panel.height - 34);
    }

    public static ConfirmationScreen passwordScreen(Screen parent, Consumer<String> pass){
        EditBox passWordBox = new EditBox(Minecraft.getInstance().font, 0,0,200,20,Component.translatable("legacy.menu.choose_user.add.encryption.password"));
        TickBox tickBox = new TickBox(0,0,true, bol->VISIBLE_PASSWORD,bol-> null, t->passWordBox.setFormatter((s,i)-> FormattedCharSequence.forward(t.selected ? s : "*".repeat(s.length()), Style.EMPTY)));
        tickBox.onPress();
        return new ConfirmationScreen(parent, passWordBox.getMessage(),Component.translatable("legacy.menu.choose_user.add.encryption.password_message"), b1-> pass.accept(passWordBox.getValue())){
            @Override
            protected void addButtons() {
                super.addButtons();
                okButton.active = false;
            }

            @Override
            protected void init() {
                super.init();
                tickBox.setPosition(panel.getX() + 15, panel.getY() + 68);
                addRenderableWidget(tickBox);
                passWordBox.setPosition(panel.getX() + 15, panel.getY() + 45);
                passWordBox.setResponder(s-> okButton.active = !s.isEmpty());
                addRenderableWidget(passWordBox);
            }
        };
    }

    public static ConfirmationScreen accountScreen(Component title, Screen parent, boolean allowEncryption, Consumer<MCAccount> press){
        return new ConfirmationScreen(parent,230, 120,title, CHOOSE_USER_MESSAGE){
            @Override
            protected void addButtons() {
                renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), b-> this.onClose()).build());
                renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.choose_user.microsoft"), b-> {
                    if (allowEncryption) minecraft.setScreen(new ConfirmationScreen(this,ACCOUNT_ENCRYPTION,ACCOUNT_ENCRYPTION_MESSAGE){
                        @Override
                        protected void addButtons() {
                            renderableVList.addRenderable(Button.builder(ADD_WITH_ENCRYPTION,b-> minecraft.setScreen(passwordScreen(this, s-> MCAccount.create(()-> minecraft.setScreen(this),s).thenAcceptAsync(press,minecraft)))).build());
                            renderableVList.addRenderable(Button.builder(ADD_WITHOUT_ENCRYPTION,b-> MCAccount.create(()-> minecraft.setScreen(this),null).thenAcceptAsync(press,minecraft)).build());
                        }
                    });
                    else MCAccount.create(()-> minecraft.setScreen(this),null).thenAcceptAsync(press,minecraft);

                }).build());
                renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.choose_user.offline"), b-> {
                    EditBox usernameBox = new EditBox(Minecraft.getInstance().font, 0,0,200,20,Component.translatable("legacy.menu.choose_user.offline.username"));
                    minecraft.setScreen(new ConfirmationScreen(this, usernameBox.getMessage(),Component.translatable("legacy.menu.choose_user.offline.username_message"), b1-> press.accept(MCAccount.create(new GameProfile(UUID.nameUUIDFromBytes(("offline:"+usernameBox.getValue()).getBytes()),usernameBox.getValue()),false,null,null))){
                        @Override
                        protected void addButtons() {
                            super.addButtons();
                            okButton.active = false;
                        }

                        @Override
                        protected void init() {
                            super.init();
                            usernameBox.setPosition(panel.getX() + 15, panel.getY() + 45);
                            usernameBox.setResponder(s-> {
                                boolean matches = usernamePattern.matcher(s).matches();
                                usernameBox.setTextColor(matches ? 0xFFFFFF : 0xFF5555);
                                okButton.active = matches;
                            });
                            addRenderableWidget(usernameBox);
                        }
                    });
                }).build());
            }
        };
    }

    public void manageLogin(MCAccount account){
        if (account.isEncrypted())
            minecraft.setScreen(passwordScreen(ChooseUserScreen.this, s -> account.login(ChooseUserScreen.this, s)));
        else account.login(ChooseUserScreen.this, null);
    }

    protected void addAccountButtons(){
        Minecraft minecraft =  Minecraft.getInstance();
        CreationList.addIconButton(renderableVList, Legacy4J.createModLocation("icon/add_user"),ADD_ACCOUNT, b->minecraft.setScreen(accountScreen(ADD_ACCOUNT, this,true, a-> {
            MCAccount.list.add(a);
            MCAccount.saveAll();
            reloadAccountButtons();
            minecraft.setScreen(ChooseUserScreen.this);
        })));
        for (MCAccount account : MCAccount.list) {
            renderableVList.addRenderable(new AbstractButton(0, 0, 230, 30, account.getMSARefreshToken(null).isEmpty() ? Component.translatable("legacy.menu.offline_user",account.getProfile().getName()) : Component.literal(account.getProfile().getName())) {
                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                    super.renderWidget(guiGraphics, i, j, f);
                    PlayerFaceRenderer.draw(guiGraphics, minecraft.getSkinManager()./*? if >1.20.1 {*/getInsecureSkin/*?} else {*//*getInsecureSkinLocation*//*?}*/(account.getProfile()), getX() + 5, getY() + 5, 20);
                    if (minecraft.options.touchscreen().get().booleanValue() || isHovered()) {
                        guiGraphics.fill(getX() + 5, getY() + 5, getX() + 25, getY() + 25, -1601138544);
                        FactoryGuiGraphics.of(guiGraphics).blitSprite(ScreenUtil.isMouseOver(i,j,getX()+5,getY()+5,20,20) ? SaveRenderableList.JOIN_HIGHLIGHTED : SaveRenderableList.JOIN, getX() + 5, getY() + 5, 20, 20);
                    }
                }

                @Override
                protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                    ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), getX() + 30, this.getY(), getX() + getWidth() - 2, this.getY() + this.getHeight(), j, true);
                }

                @Override
                public boolean mouseClicked(double d, double e, int i) {
                    if (ScreenUtil.isMouseOver(d,e,getX()+5,getY()+5,20,20)) manageLogin(account);
                    return super.mouseClicked(d, e, i);
                }

                @Override
                public boolean keyPressed(int i, int j, int k) {
                    if (i == InputConstants.KEY_O) {
                        minecraft.setScreen(new ConfirmationScreen(ChooseUserScreen.this, ACCOUNT_OPTIONS, ACCOUNT_OPTIONS_MESSAGE, b -> {}){
                            @Override
                            protected void addButtons() {
                                renderableVList.addRenderable(Button.builder(CommonComponents.GUI_CANCEL, b-> onClose()).build());
                                renderableVList.addRenderable(Button.builder(EDIT_ACCOUNT,b-> minecraft.setScreen(accountScreen(EDIT_ACCOUNT, this,true,a-> {
                                    MCAccount.list.set(MCAccount.list.indexOf(account), a);
                                    MCAccount.saveAll();
                                    reloadAccountButtons();
                                    minecraft.setScreen(ChooseUserScreen.this);
                                }))).build());
                                renderableVList.addRenderable(Button.builder(DELETE_ACCOUNT, b->{
                                    MCAccount.list.remove(account);
                                    MCAccount.saveAll();
                                    reloadAccountButtons();
                                    minecraft.setScreen(ChooseUserScreen.this);
                                }).build());
                            }
                        });
                        return true;
                    }
                    return super.keyPressed(i, j, k);
                }

                @Override
                public void onPress() {
                    if (isFocused()) manageLogin(account);
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                    defaultButtonNarrationText(narrationElementOutput);
                }
            });
        }
    }
}

