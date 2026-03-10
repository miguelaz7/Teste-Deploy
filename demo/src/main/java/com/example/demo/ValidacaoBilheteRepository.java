package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;

public interface ValidacaoBilheteRepository extends JpaRepository<ValidacaoBilhete, Long> {
	boolean existsByViagemAndNumeroPassageirosAndTipoBilheteAndValorPago(
			Viagem viagem,
			Integer numeroPassageiros,
			String tipoBilhete,
			BigDecimal valorPago
	);
}
