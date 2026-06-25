package com.fintrack.auth.domain.valueobject;

/**
 * Value Object — rôle d'un utilisateur dans le système.
 * Séparé de User pour respecter OCP :
 * un nouveau rôle n'implique pas de modifier User.java.
 */
public enum Role {
    USER,
    ADMIN
}
