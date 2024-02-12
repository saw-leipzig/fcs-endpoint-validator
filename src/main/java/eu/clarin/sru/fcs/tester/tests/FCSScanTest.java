package eu.clarin.sru.fcs.tester.tests;

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
import eu.clarin.sru.fcs.tester.FCSTestContext;
import eu.clarin.sru.fcs.tester.FCSTestProfile;
import eu.clarin.sru.fcs.tester.tests.AbstractFCSTest.Scan;

@Order(2000)
@Scan
@DisplayName("Scan")
public class FCSScanTest extends AbstractFCSTest {

    @Test
    @Order(2000)
    @ClarinFCSLegacy
    @DisplayName("Scan with missing 'scanClause' argument")
    void doScanWithMissingScanClause(FCSTestContext context) throws SRUClientException {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_LEGACY, "Only checked for Legacy FCS.");

        SRUScanRequest req = context.createScanRequest();
        req.setExtraRequestData(SRUScanRequest.X_MALFORMED_SCAN_CLAUSE, SRUScanRequest.MALFORMED_OMIT);
        SRUScanResponse res = context.getClient().scan(req);
        assertHasDiagnostic(res, "info:srw/diagnostic/1/7");
    }

    @Test
    @Order(2010)
    @ClarinFCSLegacy
    @DisplayName("Scan on 'fcs.resource = root'")
    void doScanOnRoot(FCSTestContext context) throws SRUClientException {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_LEGACY, "Only checked for Legacy FCS.");

        SRUScanRequest req = context.createScanRequest();
        req.setScanClause("fcs.resource=root");
        SRUScanResponse res = context.getClient().scan(req);

        assumeFalse(hasDiagnostic(res, "info:srw/diagnostic/1/4"), "Endpoint does not support 'scan' operation");

        assertEquals(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint");

        assumeTrue(res.getTermsCount() >= 1, "Scan on 'fcs.resource = root' should yield at least one collection");
    }

    @Test
    @Order(2030)
    @ClarinFCSLegacy
    @DisplayName("Scan on 'fcs.resource = root' with 'maximumTerms' with value 1")
    void doScanOnRootWithMaximumTermsIsOne(FCSTestContext context) throws SRUClientException {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_LEGACY, "Only checked for Legacy FCS.");

        SRUScanRequest req = context.createScanRequest();
        req.setScanClause("fcs.resource=root");
        req.setMaximumTerms(1);
        SRUScanResponse res = context.getClient().scan(req);

        assumeFalse(hasDiagnostic(res, "info:srw/diagnostic/1/4"), "Endpoint does not support 'scan' operation");
        assertEquals(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint.");
        assertEquals(1, res.getTermsCount(), "Endpoint did not honor 'maximumTerms' argument");
    }

    @Test
    @Order(2040)
    @ClarinFCSAny
    @DisplayName("Scan on 'fcs.resource = root' with bad 'maximumTerms' argument")
    void doScanOnRootWithInvalidMaximumTermsArg(FCSTestContext context) throws SRUClientException {
        SRUScanRequest req = context.createScanRequest();
        req.setScanClause("fcs.resource=root");
        req.setExtraRequestData(SRUScanRequest.X_MALFORMED_MAXIMUM_TERMS, "invalid");
        SRUScanResponse res = context.getClient().scan(req);
        assertHasDiagnostic(res, "info:srw/diagnostic/1/6");
    }

    @Test
    @Order(2050)
    @ClarinFCSAny
    @DisplayName("Scan on 'fcs.resource = root' with bad 'responsePosition' argument")
    void doScanOnRootWithInvalidResponsePositionArg(FCSTestContext context) throws SRUClientException {
        SRUScanRequest req = context.createScanRequest();
        req.setScanClause("fcs.resource=root");
        req.setExtraRequestData(SRUScanRequest.X_MALFORMED_RESPONSE_POSITION, "invalid");
        SRUScanResponse res = context.getClient().scan(req);
        assertHasDiagnostic(res, "info:srw/diagnostic/1/6");
    }

}
