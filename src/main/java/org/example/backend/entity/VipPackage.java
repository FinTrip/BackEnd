package org.example.backend.entity;

public enum VipPackage {
    ONE_MONTH(1, 10000L),
    SIX_MONTH(6, 10000L),
    TWELVE_MONTH(12, 10000L);

    private final int months;
    private final long price;

    VipPackage(int months, long price) {
        this.months = months;
        this.price = price;
    }

    public int getMonths() { return months; }
    public long getPrice() { return price; }

    public static VipPackage fromMonths(int months) {
        for (VipPackage pkg : values()) {
            if (pkg.months == months) return pkg;
        }
        return null;
    }
} 