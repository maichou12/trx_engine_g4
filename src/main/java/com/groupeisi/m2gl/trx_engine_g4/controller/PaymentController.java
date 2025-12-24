package com.groupeisi.m2gl.trx_engine_g4.controller;

import com.groupeisi.m2gl.trx_engine_g4.DTOs.*;
import com.groupeisi.m2gl.trx_engine_g4.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    
    /**
     * 🔥 ENDPOINT 1: PAIEMENT CLIENT → MARCHAND
     */
    @PostMapping("/paiement")
    public ResponseEntity<PaymentResponseDto> effectuerPaiement(@Valid @RequestBody PaiementRequest request) {
        PaymentResponseDto response = paymentService.processPaiement(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 🔥 ENDPOINT 2: TRANSFERT INTERNE (Wave Style - Client ↔ Marchand)
     */
    @PostMapping("/transfert-interne")
    public ResponseEntity<PaymentResponseDto> effectuerTransfertInterne(@Valid @RequestBody TransfertInterneRequest request) {
        PaymentResponseDto response = paymentService.processTransfertInterne(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 🔥 ENDPOINT 3: CRÉER UN COMPTE MARCHAND 
     * Pour un client qui veut un compte marchand séparé
     */
    @PostMapping("/creer-compte-marchand")
    public ResponseEntity<PaymentResponseDto> creerCompteMarchand(@Valid @RequestBody ActiverMarchantRequest request) {
        PaymentResponseDto response = paymentService.creerCompteMarchandSepare(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 🔥 ENDPOINT 4: CONSULTER SOLDE D'UN TYPE DE COMPTE
     */
    @GetMapping("/solde/{telephone}/{typeCompte}")
    public ResponseEntity<?> consulterSoldeParType(
            @PathVariable String telephone,
            @PathVariable String typeCompte) {
        
        try {
            float solde = paymentService.getSolde(telephone, typeCompte);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "telephone", telephone,
                "typeCompte", typeCompte,
                "solde", solde,
                "devise", "FCFA"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 🔥 ENDPOINT 5: CONSULTER SOLDE TOTAL (Client + Marchand)
     */
    @GetMapping("/solde-total/{telephone}")
    public ResponseEntity<?> consulterSoldeTotal(@PathVariable String telephone) {
        try {
            float soldeTotal = paymentService.getSoldeTotal(telephone);
            boolean aCompteMarchand = paymentService.aCompteMarchand(telephone);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "telephone", telephone,
                "soldeTotal", soldeTotal,
                "aCompteMarchand", aCompteMarchand,
                "devise", "FCFA"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 🔥 ENDPOINT 6: VÉRIFIER SI UN UTILISATEUR A UN COMPTE MARCHAND
     */
    @GetMapping("/a-compte-marchand/{telephone}")
    public ResponseEntity<?> verifierCompteMarchand(@PathVariable String telephone) {
        try {
            boolean aCompteMarchand = paymentService.aCompteMarchand(telephone);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "telephone", telephone,
                "aCompteMarchand", aCompteMarchand,
                "message", aCompteMarchand ? 
                    "Cet utilisateur a un compte marchand" : 
                    "Cet utilisateur n'a pas de compte marchand"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 🔥 ENDPOINT 7: DÉSACTIVER COMPTE MARCHAND
     */
    @PutMapping("/desactiver-compte-marchand/{telephone}")
    public ResponseEntity<PaymentResponseDto> desactiverCompteMarchand(@PathVariable String telephone) {
        PaymentResponseDto response = paymentService.desactiverCompteMarchand(telephone);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}