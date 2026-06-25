package com.fintrack.auth.domain;

import com.fintrack.auth.domain.exception.InvalidEmailException;
import com.fintrack.auth.domain.valueobject.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Email Value Object")
class EmailTest {

    @Nested
    @DisplayName("Valid emails")
    class ValidEmails {

        @ParameterizedTest(name = "should accept [{0}]")
        @ValueSource(strings = {
                "user@example.com",
                "User@Example.COM",
                "user.name+tag@domain.co",
                "user123@sub.domain.org"
        })
        void shouldAcceptValidEmail(String raw) {
            Email email = new Email(raw);
            assertThat(email.value()).isEqualTo(raw.trim().toLowerCase());
        }

        @Test
        @DisplayName("should normalize to lowercase")
        void shouldNormalizeToLowercase() {
            Email email = new Email("User@Example.COM");
            assertThat(email.value()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("should trim whitespace")
        void shouldTrimWhitespace() {
            Email email = new Email("  user@example.com  ");
            assertThat(email.value()).isEqualTo("user@example.com");
        }
    }

    @Nested
    @DisplayName("Invalid emails")
    class InvalidEmails {

        @ParameterizedTest(name = "should reject [{0}]")
        @ValueSource(strings = {
                "notanemail",
                "@nodomain.com",
                "user@",
                "user @example.com",
                "",
                "user@domain"
        })
        void shouldRejectInvalidEmail(String raw) {
            assertThatThrownBy(() -> new Email(raw))
                    .isInstanceOf(InvalidEmailException.class);
        }

        @Test
        @DisplayName("should reject null")
        void shouldRejectNull() {
            assertThatThrownBy(() -> new Email(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    @DisplayName("two emails with same value should be equal")
    void shouldBeEqualByValue() {
        Email e1 = new Email("user@example.com");
        Email e2 = new Email("USER@EXAMPLE.COM");
        assertThat(e1).isEqualTo(e2);
    }
}
