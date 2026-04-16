package com.poc.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

// @Readiness = answers: "is the app ready to accept traffic?"
// Kubernetes uses this to decide if a pod should receive requests
@Readiness
@ApplicationScoped
public class ReadinessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse
                .named("poc-readiness")
                .status(true)
                .withData("instance", "B-canary")   // visible in the JSON response
                .withData("version", "1.0")
                .build();
    }
}
