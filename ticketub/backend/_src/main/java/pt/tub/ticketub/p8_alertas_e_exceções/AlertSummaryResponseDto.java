package pt.tub.ticketub.p8_alertas_e_excecoes;

public record AlertSummaryResponseDto(
    long total,
    long open,
    long pending,
    long inProgress,
    long falsePositive,
    long resolved,
    long escalated,
    long critical
) {
}