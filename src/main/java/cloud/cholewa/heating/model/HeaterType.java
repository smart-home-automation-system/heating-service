package cloud.cholewa.heating.model;

public enum HeaterType {
    RADIATOR("radiator"),
    FLOOR("floor");

    private final String value;

    HeaterType(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
