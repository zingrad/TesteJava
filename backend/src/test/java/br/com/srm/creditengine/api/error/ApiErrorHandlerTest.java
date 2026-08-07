package br.com.srm.creditengine.api.error;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.srm.creditengine.domain.exception.BusinessRuleException;
import br.com.srm.creditengine.domain.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = ApiErrorHandlerTest.StubController.class)
@Import({ApiErrorHandler.class, ApiErrorHandlerTest.StubController.class})
class ApiErrorHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void mapsResourceNotFoundToNotFound() throws Exception {
        mockMvc.perform(get("/stub/not-found/42"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value(containsString("42")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void mapsBusinessRuleToUnprocessableEntity() throws Exception {
        mockMvc.perform(get("/stub/business-rule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("FX_RATE_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail").value("Sem cotação vigente para o par informado."));
    }

    @Test
    void mapsOptimisticLockingToConflict() throws Exception {
        mockMvc.perform(get("/stub/stale"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
    }

    @Test
    void reportsEveryRejectedFieldOnBodyValidation() throws Exception {
        mockMvc.perform(post("/stub/receivable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentNumber": "  ", "faceValue": -10}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations.length()").value(2))
                .andExpect(jsonPath("$.violations[0].field").value("documentNumber"))
                .andExpect(jsonPath("$.violations[1].field").value("faceValue"));
    }

    @Test
    void mapsMalformedJsonToBadRequest() throws Exception {
        mockMvc.perform(post("/stub/receivable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"faceValue\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void mapsUnknownRouteToNotFoundWithoutLeakingResourceResolution() throws Exception {
        mockMvc.perform(get("/stub/there-is-no-such-route"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENDPOINT_NOT_FOUND"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.detail").value(not(containsString("static resource"))));
    }

    @Test
    void mapsWrongHttpMethodToMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/stub/business-rule"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void rejectsUnknownJsonProperty() throws Exception {
        mockMvc.perform(post("/stub/receivable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentNumber": "DUP-1", "faceValue": 100, "injected": true}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void hidesInternalDetailsAndReturnsIncidentCode() throws Exception {
        mockMvc.perform(get("/stub/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.incident").exists())
                .andExpect(jsonPath("$.detail").value(not(containsString("jdbc:postgresql"))));
    }

    record ReceivableRequest(
            @NotBlank String documentNumber,
            @Positive BigDecimal faceValue) {
    }

    @RestController
    @RequestMapping("/stub")
    static class StubController {

        @GetMapping("/not-found/{id}")
        void notFound(@PathVariable String id) {
            throw new ResourceNotFoundException("Cedente", id);
        }

        @GetMapping("/business-rule")
        void businessRule() {
            throw new BusinessRuleException("FX_RATE_UNAVAILABLE", "Sem cotação vigente para o par informado.");
        }

        @GetMapping("/stale")
        void stale() {
            throw new OptimisticLockingFailureException("settlement 7 was already settled");
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("connection to jdbc:postgresql://localhost:5432/srm_credit refused");
        }

        @PostMapping("/receivable")
        void receivable(@Valid @RequestBody ReceivableRequest request) {
        }
    }
}
