package wily.legacy.client.screen.globalleaderboards.board;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import wily.legacy.client.screen.LegacyIconHolder;

public final class LazyItemIconHolder extends LegacyIconHolder {
    private final ItemLike itemLike;

    public LazyItemIconHolder(int width, int height, ItemLike itemLike) {
        super(width, height);
        this.itemLike = itemLike;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        itemIcon = itemStack();
        super.render(graphics, mouseX, mouseY, delta);
    }

    private ItemStack itemStack() {
        try {
            return itemLike.asItem().getDefaultInstance();
        } catch (RuntimeException ex) {
            return ItemStack.EMPTY;
        }
    }
}
