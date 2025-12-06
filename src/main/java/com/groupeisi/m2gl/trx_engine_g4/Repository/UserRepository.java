package com.groupeisi.m2gl.trx_engine_g4.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groupeisi.m2gl.trx_engine_g4.entities.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByKeycloakId(String keycloakId);
    Optional<User> findByTelephone(String telephone);
}