package wily.legacy.util;

import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

public class CompoundTagUtil {
    public static CompoundTag parseCompoundTag(String tagString){
        try {
            return TagParser.parseTag(tagString);
        } catch (CommandSyntaxException ex) {
            throw new JsonSyntaxException("Invalid nbt tag: " + ex.getMessage());
        }
    }

}
