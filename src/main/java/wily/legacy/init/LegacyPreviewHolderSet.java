package wily.legacy.init;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;

import java.util.List;

final class LegacyPreviewHolderSet<T> extends HolderSet.Named<T> {
    LegacyPreviewHolderSet(HolderOwner<T> owner, TagKey<T> key) {
        super(owner, key);
    }

    @Override
    protected List<Holder<T>> contents() {
        return List.of();
    }

    @Override
    public boolean isBound() {
        return true;
    }

    @Override
    public boolean contains(Holder<T> holder) {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof HolderSet.Named<?> named && key().equals(named.key());
    }

    @Override
    public int hashCode() {
        return key().hashCode();
    }
}
