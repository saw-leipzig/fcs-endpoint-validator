package eu.clarin.sru.fcs.validator.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
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
    private final String randomCQLModifier = RandomStringUtils.randomAlphabetic(8);
    private final String unicodeSearchTerm = "öäüÖÄÜß€";

    private static ClarinFCSEndpointDescription endpointDescription;

    // ----------------------------------------------------------------------
    // EndpointDescription for all tests

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
    // LexFCS: valid searches

    @Test
    @Order(5010)
    @LexFCS
    @DisplayName("Search for random string with LexCQL")
    @Expected("No errors or diagnostics (and zero or more records)")
    void doRandomLexCQLSearch(FCSTestContext context) throws SRUClientException {
        assumeLexSearch(context);

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
        assumeLexSearch(context);

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

    // ----------------------------------------------------------------------
    // LexFCS: check quoting support (query terms with spaces)

    @Test
    @Order(5030)
    @LexFCS
    @DisplayName("Search for random string with LexCQL 'lemma = \"<random string>\"'")
    @Expected("No errors or diagnostics (and zero or more records)")
    void doRandomLexCQLSearchWithLemma(FCSTestContext context) throws SRUClientException {
        assumeLexSearch(context);

        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(FCSTestConstants.QUERY_TYPE_LEX, String.format("lemma = \"%s\"", getRandomSearchTerm()));
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertEqualsElseWarn(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint.");
    }

    @Test
    @Order(5035)
    @LexFCS
    @DisplayName("Search for random string with LexCQL 'lemma = <random string>'")
    @Expected("No errors or diagnostics (and zero or more records)")
    void doRandomLexCQLSearchWithLemmaUnquoted(FCSTestContext context) throws SRUClientException {
        assumeLexSearch(context);

        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(FCSTestConstants.QUERY_TYPE_LEX, String.format("lemma = %s", getRandomSearchTerm()));
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertEqualsElseWarn(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint.");
    }

    // ----------------------------------------------------------------------
    // LexFCS: check support for boolean operators / more complex queries

    @Test
    @Order(5100)
    @LexFCS
    @DisplayName("Search for \"<random string>\" 'OR' \"<random string>\" with LexFCS to test 'OR' boolean operator support")
    @Expected("No errors but an optional diagnostic if the 'OR' boolean operator is not supported (and zero or more records)")
    void doLexFCSSearchWithBoolOr(FCSTestContext context) throws SRUClientException {
        assumeLexSearch(context);

        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(FCSTestConstants.QUERY_TYPE_LEX,
                String.format("\"%s\" OR \"%s\"", getRandomSearchTerm(), getRandomSearchTerm()));
        req.setMaximumRecords(5);
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);

        String diag_query_syntax = "info:srw/diagnostic/1/10";
        assertFalse(hasDiagnostic(res.getDiagnostics(), diag_query_syntax),
                String.format("Endpoint failed to parse query with 'Query syntax error': %s", diag_query_syntax));

        // only valid for ADV search
        // @formatter:off
        // String diag_query_syntax_clarin = "http://clarin.eu/fcs/diagnostic/10";
        // String diag_query_syntax_clarin2 = "http://clarin.eu/fcs/diagnostic/11";
        // String diag_query_syntax_clarin3 = "http://clarin.eu/fcs/diagnostic/14";
        // assertFalse(hasDiagnostic(res.getDiagnostics(), diag_query_syntax_clarin),
        //         String.format(
        //                 "Endpoint failed to parse query with CLARIN FCS diagnostic 'General query syntax error': %s",
        //                 diag_query_syntax_clarin));
        // @formatter:on

        String diag_unsupported_bool = "info:srw/diagnostic/1/37";
        assumeFalse(hasDiagnostic(res.getDiagnostics(), diag_unsupported_bool),
                String.format("Endpoint indicates 'Unsupported boolean operator': %s", diag_unsupported_bool));

        // this is a bit of a stretch?
        String diag_toomany_bool = "info:srw/diagnostic/1/38";
        assumeFalse(hasDiagnostic(res.getDiagnostics(), diag_toomany_bool),
                String.format("Endpoint indicates 'Too many boolean operators in query': %s", diag_toomany_bool));

        // probably not the best diagnostic to use here
        String diag_feature_unsupported = "info:srw/diagnostic/1/48";
        assumeFalse(hasDiagnostic(res.getDiagnostics(), diag_feature_unsupported),
                String.format("Endpoint indicates 'Query feature unsupported': %s", diag_feature_unsupported));

        // probably not the best diagnostic to use here
        String diag_processquery_error = "info:srw/diagnostic/1/48";
        assertFalse(hasDiagnostic(res.getDiagnostics(), diag_processquery_error),
                String.format("Endpoint indicates 'Cannot process query; reason unknown': %s",
                        diag_processquery_error));

        // otherwise assume no diagnostics?
        assertEqualsElseWarn(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint.");
    }

    @Test
    @Order(5110)
    @LexFCS
    @DisplayName("Search for \"<random string>\" 'AND' \"<random string>\" with LexFCS to test 'AND' boolean operator support")
    @Expected("No errors but an optional diagnostic if the 'AND' boolean operator is not supported (and zero or more records)")
    void doLexFCSSearchWithBoolAnd(FCSTestContext context) throws SRUClientException {
        assumeLexSearch(context);

        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(FCSTestConstants.QUERY_TYPE_LEX,
                String.format("\"%s\" AND \"%s\"", getRandomSearchTerm(), getRandomSearchTerm()));
        req.setMaximumRecords(5);
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);

        String diag_query_syntax = "info:srw/diagnostic/1/10";
        assertFalse(hasDiagnostic(res.getDiagnostics(), diag_query_syntax),
                String.format("Endpoint failed to parse query with 'Query syntax error': %s", diag_query_syntax));

        String diag_unsupported_bool = "info:srw/diagnostic/1/37";
        assumeFalse(hasDiagnostic(res.getDiagnostics(), diag_unsupported_bool),
                String.format("Endpoint indicates 'Unsupported boolean operator': %s", diag_unsupported_bool));

        // this is a bit of a stretch?
        String diag_toomany_bool = "info:srw/diagnostic/1/38";
        assumeFalse(hasDiagnostic(res.getDiagnostics(), diag_toomany_bool),
                String.format("Endpoint indicates 'Too many boolean operators in query': %s", diag_toomany_bool));

        // probably not the best diagnostic to use here
        String diag_feature_unsupported = "info:srw/diagnostic/1/48";
        assumeFalse(hasDiagnostic(res.getDiagnostics(), diag_feature_unsupported),
                String.format("Endpoint indicates 'Query feature unsupported': %s", diag_feature_unsupported));

        // probably not the best diagnostic to use here
        String diag_processquery_error = "info:srw/diagnostic/1/48";
        assertFalse(hasDiagnostic(res.getDiagnostics(), diag_processquery_error),
                String.format("Endpoint indicates 'Cannot process query; reason unknown': %s",
                        diag_processquery_error));

        // otherwise assume no diagnostics?
        assertEqualsElseWarn(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint.");
    }

    // TODO: brackets?
    // - ( lemma = "word" )
    // - lemma = word OR ( lemma = "word" )
    // - lemma = word OR ( lemma = "word" AND lemma = word )
    // TODO: lemma = /xyzMODIFIER "word"

    // ----------------------------------------------------------------------
    // LexFCS: check support for other indexes/fields like pos/def/senseRef
    // TODO: xr$... fields ? (xr$synonymy, xr$hyponymy, xr$hypernymy, xr$meronymy,
    // xr$antonymy)

    @Test
    @Order(5200)
    @LexFCS
    @DisplayName("Search for random string with LexCQL 'pos = \"<random string>\"'")
    @Expected("No errors but maybe a diagnostic if not supported (and zero or more records)")
    void doLexFCSSearchWithPosField(FCSTestContext context) throws SRUClientException {
        assumeLexSearch(context);

        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(FCSTestConstants.QUERY_TYPE_LEX, String.format("pos = \"%s\"", getRandomSearchTerm()));
        req.setMaximumRecords(5);
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);

        String diag_query_syntax = "info:srw/diagnostic/1/10";
        assertFalse(hasDiagnostic(res.getDiagnostics(), diag_query_syntax),
                String.format("Endpoint failed to parse query with 'Query syntax error': %s", diag_query_syntax));

        // TODO: what diagnostic should be used here?
        String diag_feature_unsupported = "info:srw/diagnostic/1/48";
        assumeFalse(hasDiagnostic(res.getDiagnostics(), diag_feature_unsupported),
                String.format("Endpoint indicates 'Query feature unsupported': %s", diag_feature_unsupported));

        // probably not the best diagnostic to use here
        String diag_processquery_error = "info:srw/diagnostic/1/48";
        assertFalse(hasDiagnostic(res.getDiagnostics(), diag_processquery_error),
                String.format("Endpoint indicates 'Cannot process query; reason unknown': %s",
                        diag_processquery_error));

        // otherwise assume no diagnostics?
        assertEqualsElseWarn(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint.");
    }

    @Test
    @Order(5210)
    @LexFCS
    @DisplayName("Search for random string with LexCQL 'def = \"<random string>\"'")
    @Expected("No errors but maybe a diagnostic if not supported (and zero or more records)")
    void doLexFCSSearchWithDefField(FCSTestContext context) throws SRUClientException {
        assumeLexSearch(context);

        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(FCSTestConstants.QUERY_TYPE_LEX, String.format("def = \"%s\"", getRandomSearchTerm()));
        req.setMaximumRecords(5);
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);

        String diag_query_syntax = "info:srw/diagnostic/1/10";
        assertFalse(hasDiagnostic(res.getDiagnostics(), diag_query_syntax),
                String.format("Endpoint failed to parse query with 'Query syntax error': %s", diag_query_syntax));

        // TODO: what diagnostic should be used here?
        String diag_feature_unsupported = "info:srw/diagnostic/1/48";
        assumeFalse(hasDiagnostic(res.getDiagnostics(), diag_feature_unsupported),
                String.format("Endpoint indicates 'Query feature unsupported': %s", diag_feature_unsupported));

        // probably not the best diagnostic to use here
        String diag_processquery_error = "info:srw/diagnostic/1/48";
        assertFalse(hasDiagnostic(res.getDiagnostics(), diag_processquery_error),
                String.format("Endpoint indicates 'Cannot process query; reason unknown': %s",
                        diag_processquery_error));

        // otherwise assume no diagnostics?
        assertEqualsElseWarn(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint.");
    }

    @Test
    @Order(5230)
    @LexFCS
    @DisplayName("Search for random string with LexCQL 'senseRef = \"<random string>\"'")
    @Expected("No errors but maybe a diagnostic if not supported (and zero or more records)")
    void doLexFCSSearchWithSenseRefField(FCSTestContext context) throws SRUClientException {
        assumeLexSearch(context);

        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(FCSTestConstants.QUERY_TYPE_LEX, String.format("senseRef = \"%s\"", getRandomSearchTerm()));
        req.setMaximumRecords(5);
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);

        String diag_query_syntax = "info:srw/diagnostic/1/10";
        assertFalse(hasDiagnostic(res.getDiagnostics(), diag_query_syntax),
                String.format("Endpoint failed to parse query with 'Query syntax error': %s", diag_query_syntax));

        // TODO: what diagnostic should be used here?
        String diag_feature_unsupported = "info:srw/diagnostic/1/48";
        assumeFalse(hasDiagnostic(res.getDiagnostics(), diag_feature_unsupported),
                String.format("Endpoint indicates 'Query feature unsupported': %s", diag_feature_unsupported));

        // probably not the best diagnostic to use here
        String diag_processquery_error = "info:srw/diagnostic/1/48";
        assertFalse(hasDiagnostic(res.getDiagnostics(), diag_processquery_error),
                String.format("Endpoint indicates 'Cannot process query; reason unknown': %s",
                        diag_processquery_error));

        // otherwise assume no diagnostics?
        assertEqualsElseWarn(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint.");
    }

    // ----------------------------------------------------------------------

    // TODO: check for optional Hits-annotation types
    // lex-lemma / lex-pos / les-def

    // ----------------------------------------------------------------------

    protected void assumeLexSearch(FCSTestContext context) {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.LEX_FCS, "Only checked for LexFCS (FCS 2.0).");
        assumeTrue(endpointDescription != null, "Endpoint did not supply a valid Endpoint Description?");

        // do we support ADV search?
        boolean supportsLex = endpointDescription.getCapabilities().contains(FCSTestConstants.CAPABILITY_LEX_SEARCH);
        assumeTrue(supportsLex, "Endpoint claims no support for Lex Search");
    }

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

    protected String getRandomCQLModifier() {
        return randomCQLModifier;
    }

    public String getUnicodeSearchTerm() {
        return unicodeSearchTerm;
    }

}
