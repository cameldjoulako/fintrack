package com.fintrack.auth.domain.port;

import com.fintrack.auth.domain.model.User;

/**
 * Port de sortie — opérations d'écriture sur les utilisateurs.
 *
 * ISP : séparé de UserReadRepository.
 * RegisterUser n'a besoin que de ce port.
 * Les use cases de lecture n'ont pas accès à save().
 */
public interface UserWriteRepository {

    /**
     * Persiste un utilisateur (création ou mise à jour).
     * Retourne l'entité sauvegardée avec les champs générés.
     */
    User save(User user);
}
