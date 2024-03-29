package eu.clarin.sru.fcs.validator.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.fcs.validator.FCSTestContext;
import eu.clarin.sru.fcs.validator.FCSTestProfile;
import eu.clarin.sru.fcs.validator.tests.AbstractFCSTest.Scan;

@Order(2000)
@Scan
@DisplayName("Scan")
public class FCSScanTest extends AbstractFCSTest {

    private static final String SRU_UNSUPPORTED_OPERATION = "info:srw/diagnostic/1/4";

    // ----------------------------------------------------------------------
    // SRU: invalid scan

    @Test
    @Order(2000)
    @ClarinFCSLegacy
    @DisplayName("Scan with missing 'scanClause' argument")
    @Expected("Expecting diagnostic \"info:srw/diagnostic/1/7\"")
    void doScanWithMissingScanClause(FCSTestContext context) throws SRUClientException {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_LEGACY, "Only checked for Legacy FCS.");

        SRUScanRequest req = context.createScanRequest();
        req.setExtraRequestData(SRUScanRequest.X_MALFORMED_SCAN_CLAUSE, SRUScanRequest.MALFORMED_OMIT);
        SRUScanResponse res = context.getClient().scan(req);
        assertHasDiagnostic(res, "info:srw/diagnostic/1/7");
    }

    // ----------------------------------------------------------------------
    // SRU: valid scan

    @Test
    @Order(2010)
    @ClarinFCSLegacy
    @DisplayName("Scan on 'fcs.resource = root'")
    @Expected("One or more terms returned within scan response")
    void doScanOnRoot(FCSTestContext context) throws SRUClientException {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_LEGACY, "Only checked for Legacy FCS.");

        SRUScanRequest req = context.createScanRequest();
        req.setScanClause("fcs.resource=root");
        SRUScanResponse res = context.getClient().scan(req);

        assumeFalse(hasDiagnostic(res, SRU_UNSUPPORTED_OPERATION), "Endpoint does not support 'scan' operation");

        assertEqualsElseWarn(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint");

        assumeTrueElseWarn(res.getTermsCount() >= 1,
                "Scan on 'fcs.resource = root' should yield at least one collection");
    }

    @Test
    @Order(2030)
    @ClarinFCSLegacy
    @DisplayName("Scan on 'fcs.resource = root' with 'maximumTerms' with value 1")
    @Expected("Exactly one term returned within scan response")
    void doScanOnRootWithMaximumTermsIsOne(FCSTestContext context) throws SRUClientException {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_LEGACY, "Only checked for Legacy FCS.");

        SRUScanRequest req = context.createScanRequest();
        req.setScanClause("fcs.resource=root");
        req.setMaximumTerms(1);
        SRUScanResponse res = context.getClient().scan(req);

        assumeFalse(hasDiagnostic(res, SRU_UNSUPPORTED_OPERATION), "Endpoint does not support 'scan' operation");
        assertEqualsElseWarn(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint.");
        assertEquals(1, res.getTermsCount(), "Endpoint did not honor 'maximumTerms' argument");
    }

    // ----------------------------------------------------------------------
    // SRU: invalid scan (for any SRU/FCS version)

    @Test
    @Order(2040)
    @ClarinFCSAny
    @DisplayName("Scan on 'fcs.resource = root' with bad 'maximumTerms' argument")
    @Expected("Expecting diagnostic \"info:srw/diagnostic/1/6\" (or also \"info:srw/diagnostic/1/4\" for non legacy FCS endpoints)")
    void doScanOnRootWithInvalidMaximumTermsArg(FCSTestContext context) throws SRUClientException {
        SRUScanRequest req = context.createScanRequest();
        req.setScanClause("fcs.resource=root");
        req.setExtraRequestData(SRUScanRequest.X_MALFORMED_MAXIMUM_TERMS, "invalid");
        SRUScanResponse res = context.getClient().scan(req);

        // FCS 1.x / 2.0 might just respond with SRU_UNSUPPORTED_OPERATION and not
        // parse/validate request parameters, this should be ok
        if (hasDiagnostic(res, SRU_UNSUPPORTED_OPERATION)) {
            assertTrue(context.getFCSTestProfile() != FCSTestProfile.CLARIN_FCS_LEGACY, "Diagnostics '"
                    + SRU_UNSUPPORTED_OPERATION + "' (SRU_UNSUPPORTED_OPERATION) is not valid for Legacy FCS!");
            return;
        }

        assertHasDiagnostic(res, "info:srw/diagnostic/1/6");
    }

    @Test
    @Order(2050)
    @ClarinFCSAny
    @DisplayName("Scan on 'fcs.resource = root' with bad 'responsePosition' argument")
    @Expected("Expecting diagnostic \"info:srw/diagnostic/1/6\" (or also \"info:srw/diagnostic/1/4\" for non legacy FCS endpoints)")
    void doScanOnRootWithInvalidResponsePositionArg(FCSTestContext context) throws SRUClientException {
        SRUScanRequest req = context.createScanRequest();
        req.setScanClause("fcs.resource=root");
        req.setExtraRequestData(SRUScanRequest.X_MALFORMED_RESPONSE_POSITION, "invalid");
        SRUScanResponse res = context.getClient().scan(req);

        // FCS 1.x / 2.0 might just respond with SRU_UNSUPPORTED_OPERATION and not
        // parse/validate request parameters, this should be ok
        if (hasDiagnostic(res, SRU_UNSUPPORTED_OPERATION)) {
            assertTrue(context.getFCSTestProfile() != FCSTestProfile.CLARIN_FCS_LEGACY, "Diagnostics '"
                    + SRU_UNSUPPORTED_OPERATION + "' (SRU_UNSUPPORTED_OPERATION) is not valid for Legacy FCS!");
            return;
        }

        assertHasDiagnostic(res, "info:srw/diagnostic/1/6");
    }

}
