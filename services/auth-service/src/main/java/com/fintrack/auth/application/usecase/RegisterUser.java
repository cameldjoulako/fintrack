package com.fintrack.auth.application.usecase;

import com.fintrack.auth.domain.exception.EmailAlreadyExistsException;
import com.fintrack.auth.domain.model.User;
import com.fintrack.auth.domain.port.PasswordEncoder;
import com.fintrack.auth.domain.port.UserReadRepository;
import com.fintrack.auth.domain.port.UserWriteRepository;
import com.fintrack.auth.domain.valueobject.Email;
import com.fintrack.auth.domain.valueobject.Password;

/**
 * Use Case : enregistrement d'un nouvel utilisateur.
 *
 * SRP  : une seule responsabilité — orchestrer l'enregistrement.
 *        Command et Result sont dans des fichiers séparés.
 * DIP  : dépend uniquement d'interfaces (ports), jamais d'implémentations.
 * ISP  : injecte UserReadRepository ET UserWriteRepository séparément —
 *        chaque port expose uniquement ce dont ce use case a besoin.
 *
 * Flow corrigé (DDD) :
 *   1. Valider l'email (domaine — Email value object)
 *   2. Vérifier l'unicité (port read)
 *   3. Créer l'agrégat sans password (factory User.register())
 *   4. Hacher le password via port (infrastructure, pas domaine)
 *   5. Appliquer le hash sur l'agrégat (User.applyHashedPassword())
 *   6. Persister (port write)
 *   7. Retourner le résultat applicatif
 */
public class RegisterUser {

    private final UserReadRepository  userReadRepository;
    private final UserWriteRepository userWriteRepository;
    private final PasswordEncoder     passwordEncoder;

    public RegisterUser(UserReadRepository userReadRepository,
                        UserWriteRepository userWriteRepository,
                        PasswordEncoder passwordEncoder) {
        this.userReadRepository  = userReadRepository;
        this.userWriteRepository = userWriteRepository;
        this.passwordEncoder     = passwordEncoder;
    }

    public RegisterUserResult execute(RegisterUserCommand command) {

        // 1. Validation métier de l'email (lève InvalidEmailException si invalide)
        Email email = new Email(command.email());

        // 2. Unicité de l'email
        if (userReadRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(command.email());
        }

        // 3. Création de l'agrégat — sans password pour l'instant
        User user = User.register(email, command.firstName(), command.lastName());

        // 4. Hachage via port (BCrypt en infrastructure, le domaine ne sait pas)
        Password hashedPassword = passwordEncoder.encode(
                Password.ofPlainText(command.password())
        );

        // 5. Application du hash sur l'agrégat via méthode métier explicite
        user.applyHashedPassword(hashedPassword);

        // 6. Persistance
        User saved = userWriteRepository.save(user);

        // 7. Résultat applicatif (pas l'agrégat — évite la fuite de domaine)
        return new RegisterUserResult(
                saved.id().toString(),
                saved.email().value(),
                saved.firstName(),
                saved.lastName(),
                saved.status().name(),
                saved.createdAt().toString()
        );
    }
}
