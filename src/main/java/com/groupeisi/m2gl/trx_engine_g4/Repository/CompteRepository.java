
package com.groupeisi.m2gl.trx_engine_g4.Repository;

import com.groupeisi.m2gl.trx_engine_g4.entities.Compte;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CompteRepository extends JpaRepository<Compte, Integer> {
    Optional<Compte> findByNumCompte(UUID numCompte);
    boolean existsByNumCompte(UUID numCompte);
    
    //  MÉTHODE POUR LE PAYMENTSERVICE
    Optional<Compte> findByCodeMarchant(Integer codeMarchant);
}