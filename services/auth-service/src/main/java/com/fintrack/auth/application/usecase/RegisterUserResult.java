package com.fintrack.auth.application.usecase;

/**
 * Résultat du use case RegisterUser.
 *
 * SRP : séparé du use case.
 * Contient uniquement les données nécessaires à la réponse —
 * jamais l'agrégat User complet (évite les fuites de domaine vers l'API).
 */
public record RegisterUserResult(
        String userId,
        String email,
        String firstName,
        String lastName,
        String status,
        String createdAt
) {}
