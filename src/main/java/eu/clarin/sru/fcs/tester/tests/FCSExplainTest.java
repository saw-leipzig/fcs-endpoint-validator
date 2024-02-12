package eu.clarin.sru.fcs.tester.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRUExplainResponse;
import eu.clarin.sru.client.fcs.ClarinFCSConstants;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription;
import eu.clarin.sru.client.fcs.DataViewAdvanced;
import eu.clarin.sru.client.fcs.DataViewHits;
import eu.clarin.sru.fcs.tester.FCSTestContext;
import eu.clarin.sru.fcs.tester.FCSTestProfile;

@Tag("explain")
@DisplayName("Explain")
public class FCSExplainTest extends AbstractFCSTest {

    @Test
    @Order(1000)
    @ClarinFCSAny
    @DisplayName("Regular explain request using default version")
    void doDefaultExplain(FCSTestContext context) throws SRUClientException {
        SRUExplainRequest req = context.createExplainRequest();
        SRUExplainResponse res = context.getClient().explain(req);
        assertEquals(0, res.getDiagnosticsCount(), "No diagnostics should exist.");
    }

    @Test
    @Order(1010)
    @ClarinFCSAnyOld
    @DisplayName("Explain without 'operation' and 'version' arguments")
    void doExplainWithoutArgs(FCSTestContext context) throws SRUClientException {
        assumeTrue(
                context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_1_0
                        || context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_LEGACY,
                "Skipped for FCS 2.0. Arguments 'operation' and 'version' are not used anymore.");

        SRUExplainRequest req = context.createExplainRequest();
        req.setExtraRequestData(SRUExplainRequest.X_MALFORMED_OPERATION, SRUExplainRequest.MALFORMED_OMIT);
        req.setExtraRequestData(SRUExplainRequest.X_MALFORMED_VERSION, SRUExplainRequest.MALFORMED_OMIT);
        SRUExplainResponse res = context.getClient().explain(req);
    }

    @Test
    @Order(1020)
    @ClarinFCSAnyOld
    @DisplayName("Explain without 'version' argument")
    void doExplainWithoutVersionArg(FCSTestContext context) throws SRUClientException {
        assumeTrue(
                context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_1_0
                        || context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_LEGACY,
                "Skipped for FCS 2.0. Argument 'version' is not used anymore.");

        SRUExplainRequest req = context.createExplainRequest();
        req.setExtraRequestData(SRUExplainRequest.X_MALFORMED_VERSION, SRUExplainRequest.MALFORMED_OMIT);
        SRUExplainResponse res = context.getClient().explain(req);
    }

    @Test
    @Order(1040)
    @ClarinFCSAnyOld
    @DisplayName("Explain without 'operation' argument")
    void doExplainWithoutOperationArg(FCSTestContext context) throws SRUClientException {
        assumeTrue(
                context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_1_0
                        || context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_LEGACY,
                "Skipped for FCS 2.0. Argument 'operation' is not used anymore.");

        SRUExplainRequest req = context.createExplainRequest();
        req.setExtraRequestData(SRUExplainRequest.X_MALFORMED_OPERATION, SRUExplainRequest.MALFORMED_OMIT);
        SRUExplainResponse res = context.getClient().explain(req);
    }

    @Test
    @Order(1050)
    @ClarinFCSAnyOld
    @DisplayName("Explain with invalid value for 'version' argument")
    void doExplainWithInvalidVersionArg(FCSTestContext context) throws SRUClientException {
        assumeTrue(
                context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_1_0
                        || context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_LEGACY,
                "Skipped for FCS 2.0. Argument 'version' is not used anymore.");

        SRUExplainRequest req = context.createExplainRequest();
        req.setExtraRequestData(SRUExplainRequest.X_MALFORMED_VERSION, "9.9");
        SRUExplainResponse res = context.getClient().explain(req);
        assertHasDiagnostic(res, "info:srw/diagnostic/1/5");
    }

    @Test
    @Order(1100)
    @ClarinFCS10
    @DisplayName("Check for a valid FCS endpoint description")
    void doExplainHasValidEndpointDescriptionInFCS10(FCSTestContext context) throws SRUClientException {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_1_0, "Only checked for FCS 1.0.");

        SRUExplainRequest req = context.createExplainRequest();
        req.setExtraRequestData(ClarinFCSConstants.X_FCS_ENDPOINT_DESCRIPTION, "true");
        SRUExplainResponse res = context.getClient().explain(req);

        List<ClarinFCSEndpointDescription> descs = res.getExtraResponseData(ClarinFCSEndpointDescription.class);
        assertNotNull(descs, "Endpoint did not return a CLARIN FCS endpoint description");
        assertEquals(1, descs.size(), "Endpoint must only return one instance of a CLARIN FCS endpoint description");

        // validate FCS 1.0 Endpoint Description
        ClarinFCSEndpointDescription desc = descs.get(0);
        assertEquals(1, desc.getVersion(),
                "FCS 1.0 endpoint must provide an endpoint description with version set to \"1\"");
        assertFalse(desc.getCapabilities().contains(ClarinFCSConstants.CAPABILITY_ADVANCED_SEARCH),
                "Capabilities indicate support for Advanced Search, but FCS 1.0 does not support advanced search");
        assertNotEquals(0, desc.getSupportedDataViews().size(),
                "No dataview declared. Endpoint must declare support for at least one data view");

        boolean found = false;
        for (ClarinFCSEndpointDescription.DataView dataview : desc.getSupportedDataViews()) {
            if (dataview.isMimeType(DataViewHits.TYPE)) {
                found = true;
                break;
            }
        }
        assertTrue(found,
                "Endpoint must declare support for Generic Hits dataview (mimeType = " + DataViewHits.TYPE + ")");

        assertNotEquals(0, desc.getResources().size(),
                "No resources declared. Endpoint must declare at least one Resource");
    }

    @Test
    @Order(1100)
    @ClarinFCS20
    @DisplayName("Check for a valid FCS endpoint description")
    void doExplainHasValidEndpointDescriptionInFCS20(FCSTestContext context) throws SRUClientException {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_2_0, "Only checked for FCS 2.0.");

        SRUExplainRequest req = context.createExplainRequest();
        req.setExtraRequestData(ClarinFCSConstants.X_FCS_ENDPOINT_DESCRIPTION, "true");
        SRUExplainResponse res = context.getClient().explain(req);

        List<ClarinFCSEndpointDescription> descs = res.getExtraResponseData(ClarinFCSEndpointDescription.class);
        assertNotNull(descs, "Endpoint did not return a CLARIN FCS endpoint description");
        assertEquals(1, descs.size(), "Endpoint must only return one instance of a CLARIN FCS endpoint description");

        // validate FCS 2.0 Endpoint Description
        ClarinFCSEndpointDescription desc = descs.get(0);
        assertEquals(2, desc.getVersion(),
                "FCS 2.0 endpoint must provide an endpoint description with version set to \"2\"");

        boolean supportsADV = desc.getCapabilities().contains(ClarinFCSConstants.CAPABILITY_ADVANCED_SEARCH);
        // TODO: store check that we support ADV in context properties?

        boolean foundHits = false;
        boolean foundAdv = false;
        for (ClarinFCSEndpointDescription.DataView dataview : desc.getSupportedDataViews()) {
            if (dataview.isMimeType(DataViewHits.TYPE)) {
                foundHits = true;
            }
            if (dataview.isMimeType(DataViewAdvanced.TYPE)) {
                if (supportsADV) {
                    foundAdv = true;
                }
            }
        }
        assertTrue(foundHits,
                "Endpoint must declare support for Generic Hits dataview (mimeType = " + DataViewHits.TYPE + ")");
        if (supportsADV) {
            assertTrue(foundAdv,
                    "Capabilites indicate support for Advanced Search, so Endpoint must declare support for Advanced dataview (mimeType = "
                            + DataViewHits.TYPE + ")");
        }

        assertNotEquals(0, desc.getSupportedDataViews().size(),
                "No dataview declared. Endpoint must declare support for at least one data view");

        assertNotEquals(0, desc.getResources().size(),
                "No resources declared. Endpoint must declare at least one Resource");
        if (supportsADV) {
            assertNotEquals(desc.getSupportedLayers().size(), 0,
                    "Capabilites indicate support for Advanced Search, so Endpoint must supported layers");
        }

    }

}
