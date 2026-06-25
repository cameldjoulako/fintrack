package com.fintrack.auth.application.usecase;

/**
 * Commande d'entrée pour le use case RegisterUser.
 *
 * SRP : séparé du use case — c'est un objet de transfert,
 * pas de la logique métier.
 * Pas d'annotation de validation ici — la validation HTTP
 * est dans la couche api/, la validation métier est dans le domaine.
 */
public record RegisterUserCommand(
        String email,
        String password,
        String firstName,
        String lastName
) {}
