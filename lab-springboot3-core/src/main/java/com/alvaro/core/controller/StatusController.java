package com.alvaro.core.controller;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.core.dto.StatusServico;

@RestController
@RequestMapping("api/status")
public class StatusController {

    @GetMapping
    public ResponseEntity<StatusServico> status() {

        Thread t = Thread.currentThread();

        StatusServico resposta = new StatusServico("Core Banking API", "OPERACIONAL", t.toString(), t.isVirtual(),
                Instant.now());

        return ResponseEntity.ok(resposta);
    }

}
