package talkingpotatoes.pickplaceapi.user.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import talkingpotatoes.pickplaceapi.user.domain.entity.User;

/**
 * 회원 Repository
 * @author : 이나영
 * @since : 2026/05/19
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserId(String userId);

    Optional<User> findByEmail(String email);
}
