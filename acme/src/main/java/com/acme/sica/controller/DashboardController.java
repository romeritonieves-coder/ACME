package com.acme.sica.controller;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.dto.DashboardSnapshot;

public final class DashboardController {
    private final SicaApplicationContext context;

    public DashboardController(SicaApplicationContext context) {
        this.context = context;
    }

    public DashboardSnapshot load() {
        return context.dashboardService().load();
    }
}
