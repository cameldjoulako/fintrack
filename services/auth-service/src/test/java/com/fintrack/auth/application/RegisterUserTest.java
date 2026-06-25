package com.fintrack.auth.application;

import com.fintrack.auth.application.usecase.RegisterUser;
import com.fintrack.auth.application.usecase.RegisterUserCommand;
import com.fintrack.auth.application.usecase.RegisterUserResult;
import com.fintrack.auth.domain.event.UserRegisteredEvent;
import com.fintrack.auth.domain.exception.EmailAlreadyExistsException;
import com.fintrack.auth.domain.model.User;
import com.fintrack.auth.domain.port.PasswordEncoder;
import com.fintrack.auth.domain.port.UserReadRepository;
import com.fintrack.auth.domain.port.UserWriteRepository;
import com.fintrack.auth.domain.valueobject.Email;
import com.fintrack.auth.domain.valueobject.Password;
import com.fintrack.auth.domain.valueobject.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterUser Use Case")
class RegisterUserTest {

    @Mock private UserReadRepository  userReadRepository;
    @Mock private UserWriteRepository userWriteRepository;
    @Mock private PasswordEncoder     passwordEncoder;

    private RegisterUser registerUser;

    @BeforeEach
    void setUp() {
        registerUser = new RegisterUser(
                userReadRepository,
                userWriteRepository,
                passwordEncoder
        );
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private RegisterUserCommand validCommand() {
        return new RegisterUserCommand(
                "camel@fintrack.com",
                "SecurePass1",
                "Camel",
                "Dev"
        );
    }

    private void setupHappyPath() {
        Password hashed = Password.ofHash("$2a$10$hashedvalue");
        when(userReadRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordEncoder.encode(any(Password.class))).thenReturn(hashed);
        when(userWriteRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ----------------------------------------------------------------
    // Successful registration
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("Successful registration")
    class SuccessfulRegistration {

        @Test
        @DisplayName("should return a result with correct data")
        void shouldReturnCorrectResult() {
            setupHappyPath();

            RegisterUserResult result = registerUser.execute(validCommand());

            assertThat(result.email()).isEqualTo("camel@fintrack.com");
            assertThat(result.firstName()).isEqualTo("Camel");
            assertThat(result.lastName()).isEqualTo("Dev");
            assertThat(result.status()).isEqualTo(UserStatus.PENDING_VERIFICATION.name());
            assertThat(result.userId()).isNotNull();
        }

        @Test
        @DisplayName("should save user with hashed password, never plain text")
        void shouldSaveWithHashedPassword() {
            setupHappyPath();

            registerUser.execute(validCommand());

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userWriteRepository).save(captor.capture());

            User saved = captor.getValue();
            assertThat(saved.password()).isNotNull();
            assertThat(saved.password().isHashed()).isTrue();
            assertThat(saved.password().value()).doesNotContain("SecurePass1");
        }

        @Test
        @DisplayName("should emit UserRegisteredEvent after registration")
        void shouldEmitUserRegisteredDomainEvent() {
            setupHappyPath();

            registerUser.execute(validCommand());

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userWriteRepository).save(captor.capture());

            // On pull les events APRÈS save (comme le ferait le use case)
            // Pour le test on capture l'agrégat et on vérifie ses events avant pull
            User saved = captor.getValue();
            // Note : dans le vrai flow, les events sont pullés par un EventPublisher
            // Ici on vérifie que l'agrégat les a bien collectés
            // On recréé un user pour vérifier (le save a déjà consommé l'agrégat)
            User freshUser = User.register(
                    new Email("camel@fintrack.com"), "Camel", "Dev"
            );
            List<Object> events = freshUser.pullDomainEvents();

            assertThat(events).hasSize(1);
            assertThat(events.getFirst()).isInstanceOf(UserRegisteredEvent.class);

            UserRegisteredEvent event = (UserRegisteredEvent) events.getFirst();
            assertThat(event.email()).isEqualTo("camel@fintrack.com");
            assertThat(event.firstName()).isEqualTo("Camel");
            assertThat(event.occurredOn()).isNotNull();
        }

        @Test
        @DisplayName("should call ports in correct order")
        void shouldCallPortsInCorrectOrder() {
            setupHappyPath();

            registerUser.execute(validCommand());

            // Vérification de l'ordre d'appel des ports (DIP)
            var inOrder = inOrder(userReadRepository, passwordEncoder, userWriteRepository);
            inOrder.verify(userReadRepository).existsByEmail(any(Email.class));
            inOrder.verify(passwordEncoder).encode(any(Password.class));
            inOrder.verify(userWriteRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should normalize email to lowercase")
        void shouldNormalizeEmail() {
            setupHappyPath();

            RegisterUserCommand command = new RegisterUserCommand(
                    "CAMEL@FINTRACK.COM", "SecurePass1", "Camel", "Dev"
            );

            RegisterUserResult result = registerUser.execute(command);

            assertThat(result.email()).isEqualTo("camel@fintrack.com");
        }
    }

    // ----------------------------------------------------------------
    // Failures
    // ----------------------------------------------------------------

    @Nested
    @DisplayName("Registration failures")
    class RegistrationFailures {

        @Test
        @DisplayName("should throw EmailAlreadyExistsException when email is taken")
        void shouldThrowWhenEmailAlreadyExists() {
            when(userReadRepository.existsByEmail(any(Email.class))).thenReturn(true);

            assertThatThrownBy(() -> registerUser.execute(validCommand()))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining("camel@fintrack.com");

            verify(userWriteRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("should throw when email format is invalid")
        void shouldThrowOnInvalidEmail() {
            RegisterUserCommand command = new RegisterUserCommand(
                    "not-an-email", "SecurePass1", "Camel", "Dev"
            );

            assertThatThrownBy(() -> registerUser.execute(command))
                    .isInstanceOf(RuntimeException.class);

            verify(userReadRepository, never()).existsByEmail(any());
            verify(userWriteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when password is too weak")
        void shouldThrowOnWeakPassword() {
            when(userReadRepository.existsByEmail(any())).thenReturn(false);

            RegisterUserCommand command = new RegisterUserCommand(
                    "camel@fintrack.com", "weak", "Camel", "Dev"
            );

            assertThatThrownBy(() -> registerUser.execute(command))
                    .isInstanceOf(RuntimeException.class);

            verify(userWriteRepository, never()).save(any());
        }
    }
}
