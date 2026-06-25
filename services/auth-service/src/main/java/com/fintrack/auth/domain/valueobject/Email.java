package com.fintrack.auth.domain.valueobject;

import com.fintrack.auth.domain.exception.InvalidEmailException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object représentant une adresse email valide.
 * Immuable, auto-validé à la construction.
 * Aucune dépendance Spring ou infrastructure.
 */
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    public Email {
        Objects.requireNonNull(value, "Email must not be null");
        String trimmed = value.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidEmailException("Invalid email format: " + value);
        }
        value = trimmed;
    }

    @Override
    public String toString() {
        return value;
    }
}
