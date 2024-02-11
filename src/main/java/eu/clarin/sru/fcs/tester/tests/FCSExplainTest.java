package eu.clarin.sru.fcs.tester.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRUExplainResponse;
import eu.clarin.sru.fcs.tester.FCSTestContext;

@Tag("explain")
@DisplayName("Explain")
public class FCSExplainTest extends AbstractFCSTest {

    @DisplayName("Regular explain request using default version")
    @Test
    void doDefaultExplain(FCSTestContext context) throws SRUClientException {
        SRUExplainRequest req = context.createExplainRequest();
        SRUExplainResponse res = context.getClient().explain(req);
        assertEquals(res.getDiagnosticsCount(), 0, "No diagnostics should exist.");
    }

}
