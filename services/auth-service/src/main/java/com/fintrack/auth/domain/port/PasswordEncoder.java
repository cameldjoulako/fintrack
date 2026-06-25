package com.fintrack.auth.domain.port;

import com.fintrack.auth.domain.valueobject.Password;

/**
 * Port de sortie — abstraction du hachage de mot de passe.
 * Le domaine ne sait pas que BCrypt existe.
 * L'implémentation Spring Security est dans infrastructure/security/.
 */
public interface PasswordEncoder {

    Password encode(Password plainPassword);

    boolean matches(Password plainPassword, Password hashedPassword);
}
