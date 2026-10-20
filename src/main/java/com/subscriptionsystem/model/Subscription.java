package com.subscriptionsystem.model;

import java.time.LocalDate;

public class Subscription {
    private final int subscriptionId;
    private final int customerId;
    private final int planId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String status;

    public Subscription(int subscriptionId, int customerId, int planId,
                        LocalDate startDate, LocalDate endDate, String status) {
        this.subscriptionId = subscriptionId;
        this.customerId = customerId;
        this.planId = planId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public int getSubscriptionId() { return subscriptionId; }
    public int getCustomerId() { return customerId; }
    public int getPlanId() { return planId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getStatus() { return status; }
}
