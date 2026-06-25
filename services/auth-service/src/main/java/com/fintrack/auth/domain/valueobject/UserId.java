package com.fintrack.auth.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object — identifiant unique d'un utilisateur.
 * Immuable, auto-validé.
 */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "UserId must not be null");
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId of(String value) {
        try {
            return new UserId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UserId format: " + value, e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
