package sn.esp.nenecare.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sn.esp.nenecare.user.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
