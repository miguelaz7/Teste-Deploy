package com.example.demo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "viagens")
public class Viagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "viagem_id", nullable = false, unique = true)
    private String viagemId;

    @Column(nullable = false)
    private String rota;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "autocarro_id", nullable = false)
    private String autocarroId;

    @OneToMany(mappedBy = "viagem", cascade = CascadeType.ALL)
    private List<ValidacaoBilhete> validacoes = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getViagemId() {
        return viagemId;
    }

    public void setViagemId(String viagemId) {
        this.viagemId = viagemId;
    }

    public String getRota() {
        return rota;
    }

    public void setRota(String rota) {
        this.rota = rota;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getAutocarroId() {
        return autocarroId;
    }

    public void setAutocarroId(String autocarroId) {
        this.autocarroId = autocarroId;
    }

    public List<ValidacaoBilhete> getValidacoes() {
        return validacoes;
    }
}
