package com.ssafy.demo_app.domain.auth.repository;

import com.ssafy.demo_app.domain.auth.entity.RefreshToken;
import com.ssafy.demo_app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    List<RefreshToken> findByUserAndRevokedFalse(User user);

    void deleteByUser(User user);
}
