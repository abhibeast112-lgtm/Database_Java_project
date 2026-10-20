package com.subscriptionsystem.model;

import java.math.BigDecimal;

public class Plan {
    private final int planId;
    private final String planName;
    private final BigDecimal price;
    private final int durationDays;

    public Plan(int planId, String planName,
                BigDecimal price, int durationDays) {
        this.planId = planId;
        this.planName = planName;
        this.price = price;
        this.durationDays = durationDays;
    }

    public int getPlanId() { return planId; }
    public String getPlanName() { return planName; }
    public BigDecimal getPrice() { return price; }
    public int getDurationDays() { return durationDays; }
}
