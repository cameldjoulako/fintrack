package com.fintrack.auth.domain.model;

import com.fintrack.auth.domain.event.UserRegisteredEvent;
import com.fintrack.auth.domain.valueobject.Email;
import com.fintrack.auth.domain.valueobject.Password;
import com.fintrack.auth.domain.valueobject.Role;
import com.fintrack.auth.domain.valueobject.UserId;
import com.fintrack.auth.domain.valueobject.UserStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root — User.
 *
 * Règles DDD respectées :
 * - Pas de constructeur public — uniquement des factory methods
 * - Mutation uniquement via méthodes métier explicites
 * - Domain events collectés ici, dispatchés par la couche application
 * - Aucune dépendance Spring, JPA, ou framework
 *
 * Correction SOLID appliquée :
 * - UserId, Role, UserStatus extraits en Value Objects indépendants (OCP)
 * - register() reçoit le Password en clair — le hachage est délégué
 *   au port PasswordEncoder dans la couche application (SRP)
 */
public class User {

    private final UserId id;
    private final Email email;
    private Password password;
    private final String firstName;
    private final String lastName;
    private final Role role;
    private UserStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private final List<Object> domainEvents = new ArrayList<>();

    private User(UserId id, Email email, Password password,
                 String firstName, String lastName,
                 Role role, UserStatus status, Instant createdAt) {
        this.id        = id;
        this.email     = email;
        this.password  = password;
        this.firstName = firstName;
        this.lastName  = lastName;
        this.role      = role;
        this.status    = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    /**
     * Factory — enregistrement d'un nouvel utilisateur.
     *
     * Reçoit le Password EN CLAIR. Le use case est responsable
     * d'appeler passwordEncoder.encode() et de passer le hash
     * via User.applyHashedPassword() avant la persistance.
     * Émet UserRegisteredEvent.
     */
    public static User register(Email email, String firstName, String lastName) {
        User user = new User(
                UserId.generate(),
                email,
                null,                          // password défini après hachage
                firstName,
                lastName,
                Role.USER,
                UserStatus.PENDING_VERIFICATION,
                Instant.now()
        );
        user.domainEvents.add(new UserRegisteredEvent(
                user.id.value(),
                user.email.value(),
                user.firstName,
                Instant.now()
        ));
        return user;
    }

    /**
     * Factory — reconstitution depuis la persistance.
     * Pas d'event — l'utilisateur existe déjà.
     */
    public static User reconstitute(UserId id, Email email, Password hashedPassword,
                                    String firstName, String lastName,
                                    Role role, UserStatus status,
                                    Instant createdAt, Instant updatedAt) {
        User user = new User(id, email, hashedPassword,
                firstName, lastName, role, status, createdAt);
        user.updatedAt = updatedAt;
        return user;
    }

    // --- Méthodes métier ---

    /**
     * Applique le password haché après que le use case a appelé passwordEncoder.
     * Séparation claire : le domaine ne sait pas comment on hache,
     * mais il contrôle quand le hash est appliqué.
     */
    public void applyHashedPassword(Password hashedPassword) {
        if (!hashedPassword.isHashed()) {
            throw new IllegalArgumentException("Password must be hashed before being applied to User");
        }
        this.password  = hashedPassword;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (this.status != UserStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException(
                    "Cannot activate user with status: " + this.status);
        }
        this.status    = UserStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void suspend() {
        if (this.status != UserStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot suspend user with status: " + this.status);
        }
        this.status    = UserStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean hasPassword() {
        return this.password != null;
    }

    // --- Domain Events ---

    public List<Object> pullDomainEvents() {
        List<Object> events = Collections.unmodifiableList(new ArrayList<>(domainEvents));
        domainEvents.clear();
        return events;
    }

    // --- Getters (pas de setters — mutation via méthodes métier) ---

    public UserId id()          { return id; }
    public Email email()        { return email; }
    public Password password()  { return password; }
    public String firstName()   { return firstName; }
    public String lastName()    { return lastName; }
    public Role role()          { return role; }
    public UserStatus status()  { return status; }
    public Instant createdAt()  { return createdAt; }
    public Instant updatedAt()  { return updatedAt; }
}
