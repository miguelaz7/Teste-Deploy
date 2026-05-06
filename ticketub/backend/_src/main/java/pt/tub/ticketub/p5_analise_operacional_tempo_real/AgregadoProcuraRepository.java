package pt.tub.ticketub.p5_analise_operacional_tempo_real;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgregadoProcuraRepository extends JpaRepository<AgregadoProcura, Long> {
    List<AgregadoProcura> findByPerspectiva(String perspectiva);
    Optional<AgregadoProcura> findByPerspectivaAndChave(String perspectiva, String chave);
    @Query("SELECT a FROM AgregadoProcura a WHERE a.perspectiva = :p ORDER BY a.totalValidacoes DESC")
    List<AgregadoProcura> findByPerspectivaOrderByTotal(@Param("p") String perspectiva);
}
