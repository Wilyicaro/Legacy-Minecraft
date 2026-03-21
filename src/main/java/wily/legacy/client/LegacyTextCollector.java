//? if >=1.21.11 {
package wily.legacy.client;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;

public class LegacyTextCollector implements ActiveTextCollector {
    private final ActiveTextCollector collector;
    private final float outline;
    private final int outlineColor;

    public LegacyTextCollector(ActiveTextCollector collector, float outline, int outlineColor) {
        this.collector = collector;
        this.outline = outline;
        this.outlineColor = outlineColor;
    }

    @Override
    public Parameters defaultParameters() {
        return this.collector.defaultParameters();
    }

    @Override
    public void defaultParameters(Parameters parameters) {
        this.collector.defaultParameters(parameters);
    }

    @Override
    public void accept(TextAlignment textAlignment, int i, int j, Parameters parameters, FormattedCharSequence formattedCharSequence) {
        float[] translations = new float[]{0, outline, -outline};
        for (float t : translations) {
            for (float t1 : translations) {
                if (t != 0 || t1 != 0) {
                    Matrix3x2f pose = new Matrix3x2f();
                    parameters.pose().translate(t, t1, pose);
                    this.collector.accept(textAlignment, i, j, parameters.withPose(pose), FormattedCharSequence.forward(formattedCharSequence.toString(), Style.EMPTY.withColor(this.outlineColor)));
                }
            }
        }
        this.collector.accept(textAlignment, i, j, parameters, formattedCharSequence);
    }

    @Override
    public void acceptScrolling(Component component, int i, int j, int k, int l, int m, Parameters parameters) {
        this.collector.acceptScrolling(component, i, j, k, l, m, parameters);
    }
}
//?}
