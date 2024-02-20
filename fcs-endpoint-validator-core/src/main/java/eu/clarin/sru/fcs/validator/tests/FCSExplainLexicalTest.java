package eu.clarin.sru.fcs.validator.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRUExplainResponse;
import eu.clarin.sru.client.fcs.ClarinFCSConstants;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription;
import eu.clarin.sru.fcs.validator.FCSTestConstants;
import eu.clarin.sru.fcs.validator.FCSTestContext;
import eu.clarin.sru.fcs.validator.FCSTestProfile;
import eu.clarin.sru.fcs.validator.tests.AbstractFCSTest.Category;
import eu.clarin.sru.fcs.validator.tests.AbstractFCSTest.Explain;

@Order(1500)
@Explain
@Category("explain (lexical)")
@DisplayName("Explain (Lexical)")
public class FCSExplainLexicalTest extends AbstractFCSTest {

    // ----------------------------------------------------------------------
    // LexFCS: EndpointDescription

    @Test
    @Order(1510)
    @LexFCS
    @DisplayName("Check for a valid FCS endpoint description with 'lex-search' capability")
    @Expected("Expecting exactly one valid FCS endpoint decription conforming to FCS 2.0 spec with a 'http://clarin.eu/fcs/capability/lex-search' capability.")
    void doExplainHasValidEndpointDescriptionInFCS20ForLexFCS(FCSTestContext context) throws SRUClientException {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.LEX_FCS, "Only checked for FCS 2.0.");

        SRUExplainRequest req = context.createExplainRequest();
        req.setExtraRequestData(ClarinFCSConstants.X_FCS_ENDPOINT_DESCRIPTION, "true");
        SRUExplainResponse res = context.getClient().explain(req);

        List<ClarinFCSEndpointDescription> descs = res.getExtraResponseData(ClarinFCSEndpointDescription.class);
        assertNotNull(descs, "Endpoint did not return a CLARIN FCS endpoint description");
        assertEquals(1, descs.size(),
                "Endpoint must only return one instance of a CLARIN FCS endpoint description");

        // validate FCS 2.0 Endpoint Description
        ClarinFCSEndpointDescription desc = descs.get(0);
        assertEquals(2, desc.getVersion(),
                "FCS 2.0 endpoint must provide an endpoint description with version set to \"2\"");

        boolean supportsLex = desc.getCapabilities().contains(FCSTestConstants.CAPABILITY_LEX_SEARCH);

        // assert since it is required for LexFCS searches!
        assertTrue(supportsLex, "Endpoint must declare support for LexFCS using Capability = "
                + FCSTestConstants.CAPABILITY_LEX_SEARCH + "!");

        assertNotEquals(0, desc.getResources().size(),
                "No resources declared. Endpoint must declare at least one Resource");
    }

}
