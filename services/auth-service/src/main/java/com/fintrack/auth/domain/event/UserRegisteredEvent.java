package com.fintrack.auth.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain Event émis quand un utilisateur s'enregistre.
 * Sera publié sur Kafka par la couche infrastructure.
 */
public record UserRegisteredEvent(
        UUID userId,
        String email,
        String firstName,
        Instant occurredOn
) {}
