package eu.clarin.sru.fcs.validator.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUDiagnostic;
import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRUExplainResponse;
import eu.clarin.sru.client.SRURecord;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUSurrogateRecordData;
import eu.clarin.sru.client.fcs.ClarinFCSConstants;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription;
import eu.clarin.sru.client.fcs.ClarinFCSRecordData;
import eu.clarin.sru.client.fcs.DataView;
import eu.clarin.sru.client.fcs.DataViewHits;
import eu.clarin.sru.client.fcs.Resource;
import eu.clarin.sru.client.fcs.Resource.ResourceFragment;
import eu.clarin.sru.fcs.validator.FCSTestConstants;
import eu.clarin.sru.fcs.validator.FCSTestContext;
import eu.clarin.sru.fcs.validator.FCSTestProfile;
import eu.clarin.sru.fcs.validator.tests.AbstractFCSTest.Category;
import eu.clarin.sru.fcs.validator.tests.AbstractFCSTest.SearchRetrieve;

@Order(5000)
@SearchRetrieve
@Category("searchRetrieve (lexical)")
@DisplayName("SearchRetrieve (Lexical)")
public class FCSSearchLexicalTest extends AbstractFCSTest {

    private static final String FCS_RECORD_SCHEMA = ClarinFCSRecordData.RECORD_SCHEMA;

    // TODO: supply via context, maybe make repeatable and change?
    private final String randomSearchTerm = RandomStringUtils.randomAlphanumeric(16);
    private final String unicodeSearchTerm = "öäüÖÄÜß€";

    private static ClarinFCSEndpointDescription endpointDescription;

    // ----------------------------------------------------------------------

    @BeforeAll
    static void fetchEndpointDescription(FCSTestContext context) throws SRUClientException {
        SRUExplainRequest req = context.createExplainRequest();
        req.setExtraRequestData(ClarinFCSConstants.X_FCS_ENDPOINT_DESCRIPTION, "true");
        SRUExplainResponse res = context.getClient().explain(req);
        List<ClarinFCSEndpointDescription> descs = res.getExtraResponseData(ClarinFCSEndpointDescription.class);

        assertNotNull(descs, "Endpoint did not return a CLARIN FCS endpoint description");
        assertEquals(1, descs.size(),
                "Endpoint must only return one instance of a CLARIN FCS endpoint description");

        endpointDescription = descs.get(0);
    }

    // ----------------------------------------------------------------------

    @Test
    @Order(5010)
    @LexFCS
    @DisplayName("Search for random string with LexCQL")
    @Expected("No errors or diagnostics (and zero or more records)")
    void doRandomLexCQLSearch(FCSTestContext context) throws SRUClientException {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.LEX_FCS, "Only checked for LexFCS (FCS 2.0).");
        assumeTrue(endpointDescription != null, "Endpoint did not supply a valid Endpoint Description?");

        // do we support ADV search?
        boolean supportsLex = endpointDescription.getCapabilities().contains(FCSTestConstants.CAPABILITY_LEX_SEARCH);
        assumeTrue(supportsLex, "Endpoint claims no support for Lex Search");

        // ----------------------------------------------

        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(FCSTestConstants.QUERY_TYPE_LEX, escapeCQL(getRandomSearchTerm()));
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertEqualsElseWarn(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint.");
    }

    @Test
    @Order(5020)
    @LexFCS
    @DisplayName("Search for user specified search term with LexFCS and requesting endpoint to return results in CLARIN-FCS record schema '"
            + FCS_RECORD_SCHEMA + "'")
    @Expected("Expecting at least one record in CLARIN-FCS record schema (without any surrogate diagnostics)")
    void doLexFCSSearchAndRequestRecordSchema(FCSTestContext context) throws SRUClientException {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.LEX_FCS, "Only checked for LexFCS (FCS 2.0).");
        assumeTrue(endpointDescription != null, "Endpoint did not supply a valid Endpoint Description?");

        // do we support ADV search?
        boolean supportsLex = endpointDescription.getCapabilities().contains(FCSTestConstants.CAPABILITY_LEX_SEARCH);
        assumeTrue(supportsLex, "Endpoint claims no support for Lex Search");

        // ----------------------------------------------

        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(FCSTestConstants.QUERY_TYPE_LEX, escapeCQL(context.getUserSearchTerm()));
        req.setRecordSchema(FCS_RECORD_SCHEMA);
        req.setMaximumRecords(5);
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);

        assertFalse(hasDiagnostic(res, "info:srw/diagnostic/1/66"),
                "Endpoint claims to not support FCS record schema (" + FCS_RECORD_SCHEMA + ")");
        // assumeTrueElseWarn
        assumeTrue(0 != res.getRecordsCount(), "Endpoint has no results for search term >> "
                + context.getUserSearchTerm() + " <<. Please supply a different search term.");
        assertTrue(res.getRecordsCount() <= req.getMaximumRecords(),
                "Endpoint did not honor upper requested limit for \"maximumRecords\" parameter (up to "
                        + req.getMaximumRecords() + " records where requested and endpoint delivered "
                        + res.getRecordsCount() + " results)");

        for (SRURecord record : res.getRecords()) {
            assertTrue(
                    record.isRecordSchema(SRUSurrogateRecordData.RECORD_SCHEMA)
                            || record.isRecordSchema(FCS_RECORD_SCHEMA),
                    "Endpoint does not supply results in FCS record schema (" + FCS_RECORD_SCHEMA + ")");

            if (record.isRecordSchema(SRUSurrogateRecordData.RECORD_SCHEMA)) {
                final SRUSurrogateRecordData data = (SRUSurrogateRecordData) record.getRecordData();
                final SRUDiagnostic diag = data.getDiagnostic();

                assertFalse(isDiagnostic(diag, "info:srw/diagnostic/1/67"),
                        "Endpoint cannot render record in CLARIN-FCS record format and returned surrogate diagnostic \"info:srw/diagnostic/1/67\" instead.");
                assertFalse(isDiagnostic(diag, "info:clarin/sru/diagnostic/2"),
                        "Endpoint sent one or more records with record schema of '" + diag.getDetails()
                                + "' instead of '"
                                + FCS_RECORD_SCHEMA + "'.");

                StringBuilder sb = new StringBuilder();
                sb.append("Endpoint returned unexpected surrgogate diagnostic \"").append(diag.getURI()).append("\"");
                if (diag.getDetails() != null) {
                    sb.append("; details = \"").append(diag.getDetails()).append("\"");
                }
                if (diag.getMessage() != null) {
                    sb.append("; message = \"").append(diag.getMessage()).append("\"");
                }
                fail(sb.toString());
            } else if (record.isRecordSchema(FCS_RECORD_SCHEMA)) {
                final ClarinFCSRecordData data = (ClarinFCSRecordData) record.getRecordData();
                final Resource resource = data.getResource();

                // TODO: for now we do not have any other dataview besides HITS

                boolean foundHits = false;
                if (resource.hasDataViews()) {
                    for (DataView dataView : resource.getDataViews()) {
                        if (dataView.isMimeType(DataViewHits.TYPE)) {
                            foundHits = true;
                        }
                    }
                }
                if (resource.hasResourceFragments()) {
                    for (ResourceFragment fragment : resource.getResourceFragments()) {
                        for (DataView dataView : fragment.getDataViews()) {
                            if (dataView.isMimeType(DataViewHits.TYPE)) {
                                foundHits = true;
                            }
                        }
                    }
                }
                assertTrue(foundHits, "Endpoint did not provide mandatory HITS dataview in results");
            }
        }
    }

    // TODO: more with different lex operators
    // lemma = word
    // lemma = "word"
    // lemma = "word" OR lemma = word
    // lemma = "word" AND lemma = word
    // pos = random
    // def = random
    // senseRef = random

    // ----------------------------------------------------------------------

    protected String escapeCQL(String q) {
        if (q.contains(" ")) {
            return "\"" + q + "\"";
        } else {
            return q;
        }
    }

    protected String getRandomSearchTerm() {
        return randomSearchTerm;
    }

    public String getUnicodeSearchTerm() {
        return unicodeSearchTerm;
    }

}
