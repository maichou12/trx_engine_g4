package com.groupeisi.m2gl.trx_engine_g4.Repository;
import com.groupeisi.m2gl.trx_engine_g4.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

import com.groupeisi.m2gl.trx_engine_g4.entities.Compte;
public interface CompteRepository extends JpaRepository<Compte, Integer> {
    Optional<Compte> findByUser(User user);
    Optional<Compte> findByNumCompte(UUID numCompte);
    boolean existsByNumCompte(UUID numCompte);
}
