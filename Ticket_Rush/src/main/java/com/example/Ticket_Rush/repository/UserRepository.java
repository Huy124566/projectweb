package com.example.Ticket_Rush.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Ticket_Rush.entity.User;
import com.example.Ticket_Rush.entity.UserRole;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.gender = :gender")
    Long countByGender(@Param("gender") String gender);
    
    @Query("SELECT COUNT(u) FROM User u WHERE YEAR(CURRENT_DATE) - YEAR(u.dateOfBirth) BETWEEN :minAge AND :maxAge")
    Long countByAgeBetween(@Param("minAge") int minAge, @Param("maxAge") int maxAge);
    
    @Query("SELECT COUNT(u) FROM User u WHERE YEAR(CURRENT_DATE) - YEAR(u.dateOfBirth) > :age")
    Long countByAgeGreaterThan(@Param("age") int age);
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") UserRole role);
}