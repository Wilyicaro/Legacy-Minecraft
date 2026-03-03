package wily.legacy.api;

import net.minecraft.network.chat.Component;

public record ContentCategory(
    String id,
    Component title,
    String indexUrl,
    String targetDirectoryName,
    Runnable onReloadNeeded
) {}