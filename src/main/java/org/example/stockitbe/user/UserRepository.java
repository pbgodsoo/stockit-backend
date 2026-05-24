package org.example.stockitbe.user;

import org.example.stockitbe.user.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmployeeCode(String employeeCode);
    boolean existsByEmployeeCode(String employeeCode);
    boolean existsByEmail(String email);
    Optional<User> findTopByEmployeeCodeStartingWithOrderByEmployeeCodeDesc(String prefix);



}
