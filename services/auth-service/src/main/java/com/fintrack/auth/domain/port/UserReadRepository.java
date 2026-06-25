package com.fintrack.auth.domain.port;

import com.fintrack.auth.domain.model.User;
import com.fintrack.auth.domain.valueobject.Email;
import com.fintrack.auth.domain.valueobject.UserId;

import java.util.Optional;

/**
 * Port de sortie — opérations de lecture sur les utilisateurs.
 *
 * ISP : séparé de UserWriteRepository.
 * LoginUser et GetUserProfile n'ont accès qu'à ce port.
 */
public interface UserReadRepository {

    Optional<User> findByEmail(Email email);

    Optional<User> findById(UserId id);

    boolean existsByEmail(Email email);
}
