package wily.legacy.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.GsonHelper;

import java.util.function.Consumer;
import java.util.function.Function;

public class CompoundTagUtil {
    public static CompoundTag parseCompoundTag(String tagString){
        try {
            return TagParser.parseTag(tagString);
        } catch (CommandSyntaxException ex) {
            throw new JsonSyntaxException("Invalid nbt tag: " + ex.getMessage());
        }
    }
    public static <T> T getJsonStringOrNull(JsonObject object, String element, Function<String,T> constructor){
        String s = GsonHelper.getAsString(object,element, null);
        return s == null ? null : constructor.apply(s);
    }
    public static <T> void ifJsonStringNotNull(JsonObject object, String element, Function<String,T> constructor, Consumer<T> consumer){
        T obj = getJsonStringOrNull(object,element,constructor);
        if  (obj != null) consumer.accept(obj);
    }
}
