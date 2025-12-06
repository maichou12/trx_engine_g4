package com.groupeisi.m2gl.trx_engine_g4.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SmsService {
    /**
     * Simule l'envoi d'un SMS
     */
    public boolean sendSms(String to, String message) {
        log.info("--- SMS SIMULÉ ---");
        log.info("À : {}", to);
        log.info("Message : {}", message);
        log.info("--- FIN SIMULATION ---");
        // Dans une application réelle, ceci appellerait un service comme Twilio, AfricasTalking, etc.
        return true;
    }
}