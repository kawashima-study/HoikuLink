package io.github.kawashima_study.hoikulink.shared;

/**
 * アカウントのID（例：USR01KAB...）。
 */
public record UserId(String value) {

    private static final String PREFIX = "USR";

    public UserId {
        PrefixedUlid.requireValid(PREFIX, value);
    }

    public static UserId newId() {
        return new UserId(PrefixedUlid.generate(PREFIX));
    }

    @Override
    public String toString() {
        return value;
    }
}
