package org.example.stockitbe.user;

import org.example.stockitbe.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmployeeCode(String employeeCode);
    boolean existsByEmployeeCode(String employeeCode);
    boolean existsByEmail(String email);
    Optional<User> findTopByEmployeeCodeStartingWithOrderByEmployeeCodeDesc(String prefix);
}
