package org.example.backend.entity;

public enum AdPackage {
    ONE_MONTH(1, 10000L),
    THREE_MONTH(3, 10000L),
    SIX_MONTH(6, 10000L);

    private final int months;
    private final long price;

    AdPackage(int months, long price) {
        this.months = months;
        this.price = price;
    }

    public int getMonths() { return months; }
    public long getPrice() { return price; }

    public static AdPackage fromMonths(int months) {
        for (AdPackage pkg : values()) {
            if (pkg.months == months) return pkg;
        }
        return null;
    }
} 