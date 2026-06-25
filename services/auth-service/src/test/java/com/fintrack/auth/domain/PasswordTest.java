package com.fintrack.auth.domain;

import com.fintrack.auth.domain.exception.WeakPasswordException;
import com.fintrack.auth.domain.valueobject.Password;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Password Value Object")
class PasswordTest {

    @Nested
    @DisplayName("Valid passwords")
    class ValidPasswords {

        @ParameterizedTest(name = "should accept [{0}]")
        @ValueSource(strings = {
                "Password1",
                "SecurePass99",
                "MyP@ssw0rd!",
                "Abcdefgh1"
        })
        void shouldAcceptValidPassword(String raw) {
            assertThatCode(() -> Password.ofPlainText(raw))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Invalid passwords")
    class InvalidPasswords {

        @Test
        @DisplayName("should reject password shorter than 8 characters")
        void shouldRejectShortPassword() {
            assertThatThrownBy(() -> Password.ofPlainText("Short1"))
                    .isInstanceOf(WeakPasswordException.class)
                    .hasMessageContaining("at least");
        }

        @Test
        @DisplayName("should reject password without uppercase")
        void shouldRejectNoUppercase() {
            assertThatThrownBy(() -> Password.ofPlainText("password1"))
                    .isInstanceOf(WeakPasswordException.class)
                    .hasMessageContaining("uppercase");
        }

        @Test
        @DisplayName("should reject password without digit")
        void shouldRejectNoDigit() {
            assertThatThrownBy(() -> Password.ofPlainText("PasswordOnly"))
                    .isInstanceOf(WeakPasswordException.class)
                    .hasMessageContaining("digit");
        }
    }

    @Test
    @DisplayName("hashed password should skip validation")
    void hashedPasswordShouldSkipValidation() {
        // Un hash BCrypt ne respecte pas les règles textuelles — c'est normal
        assertThatCode(() -> Password.ofHash("$2a$10$someHashValue"))
                .doesNotThrowAnyException();
    }
}
