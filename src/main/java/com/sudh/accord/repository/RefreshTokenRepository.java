package com.sudh.accord.repository;

import com.sudh.accord.entity.RefreshToken;
import com.sudh.accord.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Used for cascade-revocation when reuse of an already-revoked token is
    // detected (see AuthService.refresh) — every outstanding session for the
    // user gets invalidated, not just the reused one.
    List<RefreshToken> findAllByUserAndRevokedFalse(User user);
}