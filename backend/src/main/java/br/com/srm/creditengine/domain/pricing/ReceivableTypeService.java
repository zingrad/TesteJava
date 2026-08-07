package br.com.srm.creditengine.domain.pricing;

import br.com.srm.creditengine.domain.exception.BusinessRuleException;
import br.com.srm.creditengine.domain.exception.ResourceNotFoundException;
import br.com.srm.creditengine.infrastructure.pricing.ReceivableTypeRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceivableTypeService {

    private final ReceivableTypeRepository types;

    ReceivableTypeService(ReceivableTypeRepository types) {
        this.types = types;
    }

    @Transactional(readOnly = true)
    public List<ReceivableType> listActive() {
        return types.findByActiveTrueOrderByCodeAsc();
    }

    @Transactional(readOnly = true)
    public ReceivableType requireActive(String code) {
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        ReceivableType type = types.findById(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de recebivel", normalized));

        if (!type.active()) {
            throw new BusinessRuleException("RECEIVABLE_TYPE_INACTIVE",
                    "O tipo de recebivel %s nao esta mais disponivel para operacao.".formatted(normalized));
        }
        return type;
    }
}
