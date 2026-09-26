package wily.legacy.client.control.access;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public interface InternalControlAccess {
	Component MORE = Component.literal("...").withStyle(ChatFormatting.GRAY);

	static InternalControlAccess getInstance() {
		throw new AssertionError();
	}

	Optional<Identifier> currentMinecraftLogo();

	double getPointerX();
	double getPointerY();
	boolean isCursorDisabled();

	float getVisualPointerX();

	float getVisualPointerY();

	boolean isKbm();
}
