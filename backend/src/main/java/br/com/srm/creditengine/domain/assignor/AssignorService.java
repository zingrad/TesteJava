package br.com.srm.creditengine.domain.assignor;

import br.com.srm.creditengine.domain.exception.ResourceNotFoundException;
import br.com.srm.creditengine.infrastructure.assignor.AssignorRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignorService {

    private final AssignorRepository assignors;

    AssignorService(AssignorRepository assignors) {
        this.assignors = assignors;
    }

    @Transactional(readOnly = true)
    public List<Assignor> listAll() {
        return assignors.findAllByOrderByLegalNameAsc();
    }

    @Transactional(readOnly = true)
    public Assignor requireByTaxId(String taxId) {
        String normalized = taxId.replaceAll("\\D", "");
        return assignors.findByTaxId(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Cedente", normalized));
    }
}
