package wily.legacy.inventory;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import wily.legacy.client.Offset;

public class LegacySlotWrapper extends Slot {
    protected final Slot target;
    public LegacySlotWrapper(Container container, int i, int j, int k) {
        this(new Slot(container,i,j,k));
    }
    public LegacySlotWrapper(Slot slot) {
        this(slot,slot.container, slot.getContainerSlot(), slot.x, slot.y);
    }
    public LegacySlotWrapper(Slot slot,Container container, int i, int j, int k) {
        super(container, i, j, k);
        this.target = slot;
    }
    public int getWidth(){
        return 21;
    }
    public int getHeight(){
        return 21;
    }
    public ResourceLocation getIconSprite() {
        return null;
    }
    @Override
    public void onTake(Player player, ItemStack itemStack) {
        this.target.onTake(player, itemStack);
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return this.target.mayPlace(itemStack);
    }

    @Override
    public ItemStack getItem() {
        return this.target.getItem();
    }

    @Override
    public boolean hasItem() {
        return this.target.hasItem();
    }

    @Override
    public void setByPlayer(ItemStack itemStack, ItemStack itemStack2) {
        this.target.setByPlayer(itemStack, itemStack2);
    }

    @Override
    public void set(ItemStack itemStack) {
        this.target.set(itemStack);
    }

    @Override
    public void setChanged() {
        this.target.setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return this.target.getMaxStackSize();
    }

    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        return this.target.getMaxStackSize(itemStack);
    }

    @Override
    @Nullable
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return this.target.getNoItemIcon();
    }

    @Override
    public ItemStack remove(int i) {
        return this.target.remove(i);
    }

    @Override
    public boolean isActive() {
        return this.target.isActive();
    }

    @Override
    public boolean mayPickup(Player player) {
        return this.target.mayPickup(player);
    }

    public boolean hasIconHolder() {
        return true;
    }

    public Offset getOffset() {
        return null;
    }
}
