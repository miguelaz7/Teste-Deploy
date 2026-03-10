CREATE TABLE IF NOT EXISTS viagens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    viagem_id VARCHAR(80) NOT NULL UNIQUE,
    rota VARCHAR(120) NOT NULL,
    data_hora DATETIME NOT NULL,
    autocarro_id VARCHAR(80) NOT NULL
);

CREATE TABLE IF NOT EXISTS validacoes_bilhete (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    viagem_fk BIGINT NOT NULL,
    numero_passageiros INT NOT NULL,
    tipo_bilhete VARCHAR(50) NOT NULL,
    valor_pago DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_validacao_viagem
        FOREIGN KEY (viagem_fk) REFERENCES viagens(id)
);
