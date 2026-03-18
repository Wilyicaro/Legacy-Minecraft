package wily.legacy.Skins.client.screen.changeskin;

public record ChangeSkinLayoutMetrics(
        int tooltipTopOffset,
        int tooltipWidthTrim,
        int tooltipHeightTrim,
        int tooltipFooterHeight,
        int tooltipHeightRecover,
        int carouselClipInset,
        int carouselClipBottomTrim,
        float centerScale,
        float rightCardScale,
        int originPadX,
        int originPadY,
        int panelMarginX,
        int carouselOffset,
        int minLeftClearance,
        int rightCardPadding
) {
    public static final ChangeSkinLayoutMetrics DEFAULT = new ChangeSkinLayoutMetrics(
            45, 23, 80, 50, 40,
            2, 24,
            0.935f, 0.44f,
            8, 20, 6, 80, 88, 6
    );

    public static final ChangeSkinLayoutMetrics SD_480 = new ChangeSkinLayoutMetrics(
            34, 18, 64, 40, 30,
            2, 18,
            0.76f, 0.36f,
            6, 12, 5, 58, 62, 4
    );
}
