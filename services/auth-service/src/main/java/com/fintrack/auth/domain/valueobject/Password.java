package com.fintrack.auth.domain.valueobject;

import com.fintrack.auth.domain.exception.WeakPasswordException;

import java.util.Objects;

/**
 * Value Object représentant un mot de passe avec ses règles de validation.
 * Le mot de passe stocké ici peut être en clair (avant hachage)
 * ou haché (après persistance) — distingué par le flag isHashed.
 */
public record Password(String value, boolean isHashed) {

    private static final int MIN_LENGTH = 8;

    public Password {
        Objects.requireNonNull(value, "Password must not be null");
        if (!isHashed) {
            validate(value);
        }
    }

    /**
     * Crée un Password en clair et valide ses règles.
     */
    public static Password ofPlainText(String raw) {
        return new Password(raw, false);
    }

    /**
     * Crée un Password depuis un hash BCrypt (déjà encodé, pas de validation).
     */
    public static Password ofHash(String hash) {
        return new Password(hash, true);
    }

    private static void validate(String raw) {
        if (raw.length() < MIN_LENGTH) {
            throw new WeakPasswordException("Password must be at least " + MIN_LENGTH + " characters");
        }
        if (!raw.matches(".*[A-Z].*")) {
            throw new WeakPasswordException("Password must contain at least one uppercase letter");
        }
        if (!raw.matches(".*[0-9].*")) {
            throw new WeakPasswordException("Password must contain at least one digit");
        }
    }
}
