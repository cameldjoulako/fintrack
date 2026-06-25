package com.fintrack.auth.domain;

import com.fintrack.auth.domain.event.UserRegisteredEvent;
import com.fintrack.auth.domain.model.User;
import com.fintrack.auth.domain.valueobject.Email;
import com.fintrack.auth.domain.valueobject.Password;
import com.fintrack.auth.domain.valueobject.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User Aggregate Root")
class UserTest {

    private static final Email    VALID_EMAIL     = new Email("camel@fintrack.com");
    private static final Password HASHED_PASSWORD = Password.ofHash("$2a$10$hash");

    // ----------------------------------------------------------------
    // Factory — register
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("User.register()")
    class Register {

        @Test
        @DisplayName("should create user with PENDING_VERIFICATION status")
        void shouldCreateWithPendingStatus() {
            User user = User.register(VALID_EMAIL, "Camel", "Dev");

            assertThat(user.status()).isEqualTo(UserStatus.PENDING_VERIFICATION);
            assertThat(user.email()).isEqualTo(VALID_EMAIL);
            assertThat(user.firstName()).isEqualTo("Camel");
            assertThat(user.role().name()).isEqualTo("USER");
            assertThat(user.id()).isNotNull();
            assertThat(user.createdAt()).isNotNull();
        }

        @Test
        @DisplayName("should create user without password until hash is applied")
        void shouldCreateWithoutPassword() {
            User user = User.register(VALID_EMAIL, "Camel", "Dev");

            assertThat(user.hasPassword()).isFalse();
        }

        @Test
        @DisplayName("should emit exactly one UserRegisteredEvent")
        void shouldEmitUserRegisteredEvent() {
            User user = User.register(VALID_EMAIL, "Camel", "Dev");

            List<Object> events = user.pullDomainEvents();

            assertThat(events).hasSize(1);
            assertThat(events.getFirst()).isInstanceOf(UserRegisteredEvent.class);
        }

        @Test
        @DisplayName("should emit event with correct data")
        void shouldEmitEventWithCorrectData() {
            User user = User.register(VALID_EMAIL, "Camel", "Dev");

            UserRegisteredEvent event = (UserRegisteredEvent) user.pullDomainEvents().getFirst();

            assertThat(event.email()).isEqualTo("camel@fintrack.com");
            assertThat(event.firstName()).isEqualTo("Camel");
            assertThat(event.userId()).isEqualTo(user.id().value());
            assertThat(event.occurredOn()).isNotNull();
        }

        @Test
        @DisplayName("pullDomainEvents() should clear the events list")
        void shouldClearEventsAfterPull() {
            User user = User.register(VALID_EMAIL, "Camel", "Dev");
            user.pullDomainEvents(); // premier pull

            List<Object> secondPull = user.pullDomainEvents();

            assertThat(secondPull).isEmpty();
        }
    }

    // ----------------------------------------------------------------
    // applyHashedPassword
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("applyHashedPassword()")
    class ApplyHashedPassword {

        @Test
        @DisplayName("should set hashed password on user")
        void shouldSetHashedPassword() {
            User user = User.register(VALID_EMAIL, "Camel", "Dev");

            user.applyHashedPassword(HASHED_PASSWORD);

            assertThat(user.hasPassword()).isTrue();
            assertThat(user.password().isHashed()).isTrue();
        }

        @Test
        @DisplayName("should throw if password is not hashed")
        void shouldThrowIfPasswordIsPlainText() {
            User user = User.register(VALID_EMAIL, "Camel", "Dev");
            Password plainPassword = Password.ofPlainText("SecurePass1");

            assertThatThrownBy(() -> user.applyHashedPassword(plainPassword))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("hashed");
        }
    }

    // ----------------------------------------------------------------
    // Status transitions
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("Status transitions")
    class StatusTransitions {

        @Test
        @DisplayName("should activate a PENDING_VERIFICATION user")
        void shouldActivateUser() {
            User user = User.register(VALID_EMAIL, "Camel", "Dev");
            user.applyHashedPassword(HASHED_PASSWORD);

            user.activate();

            assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.isActive()).isTrue();
        }

        @Test
        @DisplayName("should throw when activating an already ACTIVE user")
        void shouldThrowWhenActivatingActiveUser() {
            User user = User.register(VALID_EMAIL, "Camel", "Dev");
            user.applyHashedPassword(HASHED_PASSWORD);
            user.activate();

            assertThatThrownBy(user::activate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ACTIVE");
        }

        @Test
        @DisplayName("should suspend an ACTIVE user")
        void shouldSuspendActiveUser() {
            User user = User.register(VALID_EMAIL, "Camel", "Dev");
            user.applyHashedPassword(HASHED_PASSWORD);
            user.activate();

            user.suspend();

            assertThat(user.status()).isEqualTo(UserStatus.SUSPENDED);
            assertThat(user.isActive()).isFalse();
        }

        @Test
        @DisplayName("should throw when suspending a PENDING user")
        void shouldThrowWhenSuspendingPendingUser() {
            User user = User.register(VALID_EMAIL, "Camel", "Dev");

            assertThatThrownBy(user::suspend)
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
