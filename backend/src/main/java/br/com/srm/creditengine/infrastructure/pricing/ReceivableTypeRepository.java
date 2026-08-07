package br.com.srm.creditengine.infrastructure.pricing;

import br.com.srm.creditengine.domain.pricing.ReceivableType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableTypeRepository extends JpaRepository<ReceivableType, String> {

    List<ReceivableType> findByActiveTrueOrderByCodeAsc();
}
