package com.fintrack.auth.domain.valueobject;

/**
 * Value Object — statut du cycle de vie d'un compte utilisateur.
 * Séparé de User pour respecter OCP et SRP.
 *
 * Transitions autorisées :
 *   PENDING_VERIFICATION → ACTIVE      (vérification email)
 *   ACTIVE → SUSPENDED                 (action admin)
 *   SUSPENDED → ACTIVE                 (réactivation admin)
 *   ACTIVE | SUSPENDED → DELETED       (suppression compte)
 */
public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    SUSPENDED,
    DELETED
}
