package wily.legacy.util;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface RegisterListing<T> extends Iterable<RegisterListing.Holder<T>> {
    void register();
    <V extends T> Holder<V> add(String name, Supplier<V> supplier);
    Stream<Holder<T>> stream();
    Collection<Holder<T>> getEntries();
    Registry<T> getRegistry();
    String getNamespace();
    interface Holder<T> extends Supplier<T>{
        ResourceLocation getId();
    }
}
