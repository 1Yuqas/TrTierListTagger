package one.yuqas.utils.enums;

public enum TierType {
    BEST(""),
    CRYSTAL("\uE992"),
    SWORD("\uE993"),
    UHC("\uE994"),
    POT("\uE995"),
    NETHPOT("\uE996"),
    SMP("\uE997"),
    AXE("\uE998"),
    GAPPLE("\uE999");

    private final String unicode;

    TierType(String unicode) {
        this.unicode = unicode;
    }

    public String getIcon() {
        return this.unicode;
    }
}