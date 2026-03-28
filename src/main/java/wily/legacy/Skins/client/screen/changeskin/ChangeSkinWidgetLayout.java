package wily.legacy.Skins.client.screen.changeskin;

import wily.legacy.client.screen.Panel;

final class ChangeSkinWidgetLayout {
    private final int originX;
    private final int originY;

    private ChangeSkinWidgetLayout(int originX, int originY) {
        this.originX = originX;
        this.originY = originY;
    }

    static ChangeSkinWidgetLayout resolve(Panel tooltipBox,
                                          Panel panel,
                                          float uiScale,
                                          int widgetBaseWidth,
                                          int widgetBaseHeight,
                                          ChangeSkinLayoutMetrics metrics) {
        float scale = uiScale <= 0f ? 1f : uiScale;
        ChangeSkinLayoutMetrics actual = metrics == null ? ChangeSkinLayoutMetrics.DEFAULT : metrics;

        int topOffset = scaled(actual.tooltipTopOffset(), scale);
        int widthTrim = scaled(actual.tooltipWidthTrim(), scale);
        int heightTrim = scaled(actual.tooltipHeightTrim(), scale);
        int heightFooter = scaled(actual.tooltipFooterHeight(), scale);
        int heightRecover = scaled(actual.tooltipHeightRecover(), scale);

        int contentX = tooltipBox.x;
        int contentY = panel.y + topOffset;
        int contentWidth = tooltipBox.getWidth() - widthTrim;
        int contentHeight = tooltipBox.getHeight() - heightTrim - heightFooter + heightRecover;

        float centerScale = actual.centerScale() * scale;
        int centerWidth = Math.round(widgetBaseWidth * centerScale);
        int centerHeight = Math.round(widgetBaseHeight * centerScale);
        int originPadX = scaled(actual.originPadX(), scale);
        int originPadY = scaled(actual.originPadY(), scale);

        int startX = contentX + contentWidth / 2 - centerWidth / 2 - originPadX;
        int startY = contentY + contentHeight / 2 - centerHeight / 2 - originPadY;

        int carouselOffset = Math.max(1, scaled(actual.carouselOffset(), scale));
        int marginX = Math.max(2, scaled(actual.panelMarginX(), scale));

        int panelLeft = contentX + marginX;
        int panelRight = contentX + contentWidth - marginX;
        int minStartX = panelLeft + (carouselOffset * 4) - scaled(actual.minLeftClearance(), scale);

        int rightCardWidth = Math.round((widgetBaseWidth * actual.rightCardScale()) * scale) + scaled(actual.rightCardPadding(), scale);
        int maxStartX = panelRight - rightCardWidth - (carouselOffset * 4);

        if (minStartX <= maxStartX) {
            startX = Math.max(minStartX, Math.min(startX, maxStartX));
        } else {
            int availableWidth = Math.max(0, panelRight - panelLeft);
            startX = panelLeft + (availableWidth / 2) - (centerWidth / 2) - originPadX;
        }

        return new ChangeSkinWidgetLayout(startX, startY);
    }

    int originX() {
        return originX;
    }

    int originY() {
        return originY;
    }

    private static int scaled(int value, float uiScale) {
        return Math.round(value * uiScale);
    }
}
