package one.yuqas.utils.enums;

import java.awt.*;

public enum TierType {
    BEST("", new Color(0xFFFFFF).getRGB(), new Color(0xFFFFFF).getRGB()),
    MACE("\uE991", new Color(0x599AFF).getRGB(), new Color(0x4B77C2).getRGB()),
    CRYSTAL("\uE992", new Color(0xCC00FF).getRGB(), new Color(0x660099).getRGB()),
    SWORD("\uE993", new Color(0x00FFFF).getRGB(), new Color(0x008B8B).getRGB()),
    UHC("\uE994", new Color(0xFFFF00).getRGB(), new Color(0xFFA500).getRGB()),
    POT("\uE995", new Color(0xFF5555).getRGB(), new Color(0xAA0000).getRGB()),
    NETHPOT("\uE996", new Color(0x625E5E).getRGB(), new Color(0x312E2E).getRGB()),
    SMP("\uE997", new Color(0x3FB38E).getRGB(), new Color(0x1B6152).getRGB()),
    AXE("\uE998", new Color(0x55FFFF).getRGB(), new Color(0x00AAAA).getRGB()),
    GAPPLE("\uE999", new Color(0xFFD700).getRGB(), new Color(0xDAA520).getRGB());

    private final String unicode;
    private final int htColor;
    private final int ltColor;

    TierType(String unicode, int htColor, int ltColor) {
        this.unicode = unicode;
        this.htColor = htColor;
        this.ltColor = ltColor;
    }

    public String getIcon() {
        return this.unicode;
    }

    public int getHtColor() {
        return this.htColor;
    }

    public int getLtColor() {
        return this.ltColor;
    }
}