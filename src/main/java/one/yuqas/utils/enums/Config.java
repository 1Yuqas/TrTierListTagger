package one.yuqas.utils.enums;

public enum Config {
    TAG("enabled", true, "Oyuncu başlığı üzerinde seviye etiketini gösterir."),
    SIDE("side", true, "Etiketin oyuncu ismine göre konumunu belirler (Sağ/Sol)."),
    TIER_TYPE("tier_type", "BEST", "Hangi tier türünün gösterileceğini belirler."),
    TAB_TAG("tab_enabled", true, "Tab menüsünde seviye etiketini gösterir."),
    TAB_SIDE("tab_side", true, "Tab menüsündeki etiketin konumunu belirler (Sağ/Sol).");

    private final String key;
    private final Object defaultValue;
    private final String description;

    Config(String key, Object defaultValue, String description) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String getKey() { return key; }
    public Object getDefaultValue() { return defaultValue; }
    public String getDescription() { return description; }
}