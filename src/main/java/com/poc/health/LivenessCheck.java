package com.poc.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

// @Liveness = this check answers: "is the app alive / should it be restarted?"
// @ApplicationScoped = one instance for the whole app (CDI scope)
// HealthCheck = MicroProfile interface we must implement
@Liveness
@ApplicationScoped
public class LivenessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        // In a real app you'd check: can we allocate memory? are threads deadlocked?
        // For POC: always UP
        return HealthCheckResponse
                .named("poc-liveness")
                .status(true)   // true = UP, false = DOWN
                .build();
    }
}
