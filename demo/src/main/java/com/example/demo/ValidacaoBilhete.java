package com.example.demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "validacoes_bilhete")
public class ValidacaoBilhete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "viagem_fk", nullable = false)
    private Viagem viagem;

    @Column(name = "numero_passageiros", nullable = false)
    private Integer numeroPassageiros;

    @Column(name = "tipo_bilhete", nullable = false)
    private String tipoBilhete;

    @Column(name = "valor_pago", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorPago;

    public Long getId() {
        return id;
    }

    public Viagem getViagem() {
        return viagem;
    }

    public void setViagem(Viagem viagem) {
        this.viagem = viagem;
    }

    public Integer getNumeroPassageiros() {
        return numeroPassageiros;
    }

    public void setNumeroPassageiros(Integer numeroPassageiros) {
        this.numeroPassageiros = numeroPassageiros;
    }

    public String getTipoBilhete() {
        return tipoBilhete;
    }

    public void setTipoBilhete(String tipoBilhete) {
        this.tipoBilhete = tipoBilhete;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public void setValorPago(BigDecimal valorPago) {
        this.valorPago = valorPago;
    }
}
