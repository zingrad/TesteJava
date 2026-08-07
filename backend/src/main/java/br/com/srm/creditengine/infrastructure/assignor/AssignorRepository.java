package br.com.srm.creditengine.infrastructure.assignor;

import br.com.srm.creditengine.domain.assignor.Assignor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignorRepository extends JpaRepository<Assignor, Long> {

    Optional<Assignor> findByTaxId(String taxId);

    List<Assignor> findAllByOrderByLegalNameAsc();
}
