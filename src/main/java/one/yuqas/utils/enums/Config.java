package one.yuqas.utils.enums;

public enum Config {
    TAG("enabled", true, "Oyuncu Tagında Göster."),
    SIDE("side", true, "Tag Tarafı (Sağ/Sol)"),
    SHOW_PLACEHOLDER("show_placeholder", true, "Yüklenme Yazısı (...)");

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