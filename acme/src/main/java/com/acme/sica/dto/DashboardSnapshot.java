package com.acme.sica.dto;

import java.util.List;

public record DashboardSnapshot(
        Long peopleInside,
        Long pendingRequests,
        Long openIncidents,
        Long visitsToday,
        List<RecentVisit> recentVisits,
        List<ActivityItem> recentActivity) {

    public DashboardSnapshot {
        recentVisits = List.copyOf(recentVisits);
        recentActivity = List.copyOf(recentActivity);
    }

    public record RecentVisit(String person, String company, String state, String entryTime) {
    }

    public record ActivityItem(String timestamp, String title, String detail, String type) {
    }
}
