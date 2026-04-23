package pt.tub.ticketub.p9_interoperabilidade;

import pt.tub.ticketub.p9_interoperabilidade.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StopRepository extends JpaRepository<Stop, String> {
}






