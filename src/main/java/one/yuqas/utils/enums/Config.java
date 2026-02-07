package one.yuqas.utils.enums;

public enum Config {
    TAG("enabled", true, "Oyuncu başlığı üzerinde seviye etiketini gösterir."),
    SIDE("side", true, "Etiketin oyuncu ismine göre konumunu belirler (Sağ/Sol)."),
    SHOW_PLACEHOLDER("show_placeholder", true, "Veri yüklenirken geçici bir bekleme göstergesi (...) görüntüler.");

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