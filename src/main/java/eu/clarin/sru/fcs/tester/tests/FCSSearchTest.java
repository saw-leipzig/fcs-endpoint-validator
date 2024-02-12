package eu.clarin.sru.fcs.tester.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import eu.clarin.sru.client.SRUClientConstants;
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
import eu.clarin.sru.client.fcs.DataViewAdvanced;
import eu.clarin.sru.client.fcs.DataViewHits;
import eu.clarin.sru.client.fcs.LegacyClarinFCSRecordData;
import eu.clarin.sru.client.fcs.Resource;
import eu.clarin.sru.client.fcs.Resource.ResourceFragment;
import eu.clarin.sru.fcs.tester.FCSTestContext;
import eu.clarin.sru.fcs.tester.FCSTestProfile;
import eu.clarin.sru.fcs.tester.tests.AbstractFCSTest.SearchRetrieve;

@Order(3000)
@SearchRetrieve
@DisplayName("SearchRetrieve")
public class FCSSearchTest extends AbstractFCSTest {

    private static final String FCS_RECORD_SCHEMA = ClarinFCSRecordData.RECORD_SCHEMA;
    private static final String FCS10_RECORD_SCHEMA = "http://clarin.eu/fcs/1.0";
    private static final String MIME_TYPE_KWIC = "application/x-clarin-fcs-kwic+xml";

    // TODO: supply via context, maybe make repeatable and change?
    private final String randomSearchTerm = RandomStringUtils.randomAlphanumeric(16);
    private final String unicodeSearchTerm = "öäüÖÄÜß€";

    // ----------------------------------------------------------------------

    @Test
    @Order(3000)
    @ClarinFCSAny
    @DisplayName("Search for random string")
    void doRandomCQLSearch(FCSTestContext context) throws SRUClientException {
        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL, escapeCQL(getRandomSearchTerm()));
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertEquals(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint.");
    }

    @Test
    @Order(Integer.MIN_VALUE)
    @ClarinFCSAny
    @DisplayName("Searching for random string with CLARIN FCS record schema '" + ClarinFCSRecordData.RECORD_SCHEMA
            + "'")
    void doRandomSearchWithRecordSchema(FCSTestContext context) throws SRUClientException {
        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL, escapeCQL(getRandomSearchTerm()));
        req.setRecordSchema(ClarinFCSRecordData.RECORD_SCHEMA);
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertEquals(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint.");
    }

    @Test
    @Order(3030)
    @ClarinFCSLegacy
    @DisplayName("Search with missing 'query' argument")
    void doSearchWithoutQueryArg(FCSTestContext context) throws SRUClientException {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_LEGACY, "Only checked for Legacy FCS.");

        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setExtraRequestData(SRUSearchRetrieveRequest.X_MALFORMED_QUERY, SRUSearchRetrieveRequest.MALFORMED_OMIT);
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertHasDiagnostic(res, "info:srw/diagnostic/1/7");
    }

    @Test
    @Order(3040)
    @ClarinFCSAny
    @DisplayName("Search with invalid 'recordPacking' argument")
    void doSearchWithInvalidRecordPackingArg(FCSTestContext context) throws SRUClientException {
        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL, escapeCQL(getRandomSearchTerm()));
        req.setExtraRequestData(SRUSearchRetrieveRequest.X_MALFORMED_RECORD_XML_ESCAPING, "invalid");
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertHasDiagnostic(res, "info:srw/diagnostic/1/71");
    }

    @Test
    @Order(3050)
    @ClarinFCSAny
    @DisplayName("Search with invalid value for 'startRecord' argument")
    void doSearchWithInvalidStartRecordArg(FCSTestContext context) throws SRUClientException {
        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL, escapeCQL(getRandomSearchTerm()));
        req.setExtraRequestData(SRUSearchRetrieveRequest.X_MALFORMED_START_RECORD, "invalid");
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertHasDiagnostic(res, "info:srw/diagnostic/1/6");
    }

    @Test
    @Order(3060)
    @ClarinFCSAny
    @DisplayName("Search with invalid value of 0 for 'startRecord' argument")
    void doSearchWithInvalidStartRecordArgOfZero(FCSTestContext context) throws SRUClientException {
        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL, escapeCQL(getRandomSearchTerm()));
        req.setExtraRequestData(SRUSearchRetrieveRequest.X_MALFORMED_START_RECORD, "0");
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertHasDiagnostic(res, "info:srw/diagnostic/1/6");
    }

    @Test
    @Order(3070)
    @ClarinFCSAny
    @DisplayName("Search with invalid value for 'maximumRecords' argument")
    void doSearchWithInvalidMaximumRecordsArg(FCSTestContext context) throws SRUClientException {
        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL, escapeCQL(getRandomSearchTerm()));
        req.setExtraRequestData(SRUSearchRetrieveRequest.X_MALFORMED_MAXIMUM_RECORDS, "invalid");
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertHasDiagnostic(res, "info:srw/diagnostic/1/6");
    }

    @Test
    @Order(3080)
    @ClarinFCSAny
    @DisplayName("Search with invalid value of -1 for 'maximumRecords' argument")
    void doSearchWithInvalidMaximumRecordsArgOfNegativeOne(FCSTestContext context) throws SRUClientException {
        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL, escapeCQL(getRandomSearchTerm()));
        req.setExtraRequestData(SRUSearchRetrieveRequest.X_MALFORMED_MAXIMUM_RECORDS, "-1");
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertHasDiagnostic(res, "info:srw/diagnostic/1/6");
    }

    @Test
    @Order(3090)
    @ClarinFCSAny
    @DisplayName("Search with invalid query")
    void doSearchWithInvalidQueryArg(FCSTestContext context) throws SRUClientException {
        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL, escapeCQL(getRandomSearchTerm()));
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL, "\"" + getRandomSearchTerm() + "\" =");
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertHasDiagnostic(res, "info:srw/diagnostic/1/10");
    }

    @Test
    @Order(3100)
    @ClarinFCSAny
    @DisplayName("Search to provoke first record position out of range diagnostic")
    void doSearchWithOutOfRangeRecordPositionArg(FCSTestContext context) throws SRUClientException {
        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL, escapeCQL(getRandomSearchTerm()));
        req.setStartRecord(Integer.MAX_VALUE);
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertHasDiagnostic(res, "info:srw/diagnostic/1/61");
    }

    @Test
    @Order(3200)
    @ClarinFCSAny
    @DisplayName("Search for string with non-ASCII characters encoded as UTF-8")
    void doSearchWithNonASCIIQuery(FCSTestContext context) throws SRUClientException {
        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL, escapeCQL(getUnicodeSearchTerm()));
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertEquals(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint.");
    }

    @Test
    @Order(3600)
    @ClarinFCSLegacy
    @DisplayName("Search for user specified search term and requesting endpoint to return results in CLARIN-FCS record schema '"
            + FCS10_RECORD_SCHEMA + "'")
    @SuppressWarnings("deprecation")
    void doLegacyFCSSearchAndRequestRecordSchema(FCSTestContext context) throws SRUClientException {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_LEGACY, "Only checked for Legacy FCS.");

        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL, escapeCQL(context.getUserSearchTerm()));
        req.setRecordSchema(FCS10_RECORD_SCHEMA);
        req.setMaximumRecords(5);
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);

        assertFalse(hasDiagnostic(res, "info:srw/diagnostic/1/66"),
                "Endpoint claims to not support FCS record schema (" + FCS10_RECORD_SCHEMA + ")");
        assumeTrue(0 != res.getRecordsCount(), "Endpoint has no results for search term >> "
                + context.getUserSearchTerm() + " <<. Please supply a different search term.");
        assertTrue(res.getRecordsCount() <= req.getMaximumRecords(),
                "Endpoint did not honor upper requested limit for \"maximumRecords\" parameter (up to "
                        + req.getMaximumRecords() + " records where requested and endpoint delivered "
                        + res.getRecordsCount() + " results)");

        for (SRURecord record : res.getRecords()) {
            assertTrue(
                    record.isRecordSchema(SRUSurrogateRecordData.RECORD_SCHEMA)
                            || record.isRecordSchema(FCS10_RECORD_SCHEMA),
                    "Endpoint does not supply results in FCS record schema (" + FCS10_RECORD_SCHEMA + ")");

            if (record.isRecordSchema(SRUSurrogateRecordData.RECORD_SCHEMA)) {
                final SRUSurrogateRecordData data = (SRUSurrogateRecordData) record.getRecordData();
                final SRUDiagnostic diag = data.getDiagnostic();
                assertFalse(isDiagnostic(diag, "info:srw/diagnostic/1/67"),
                        "Endpoint cannot render record in CLARIN-FCS record format and returned surrogate diagnostic \"info:srw/diagnostic/1/67\" instead.");
                assertFalse(isDiagnostic(diag, "info:clarin/sru/diagnostic/2"),
                        "Endpoint sent one or more records with record schema of '" + diag.getDetails()
                                + "' instead of '"
                                + FCS10_RECORD_SCHEMA + "'.");

                StringBuilder sb = new StringBuilder();
                sb.append("Endpoint returned unexpected surrgogate diagnostic \"").append(diag.getURI()).append("\"");
                if (diag.getDetails() != null) {
                    sb.append("; details = \"").append(diag.getDetails()).append("\"");
                }
                if (diag.getMessage() != null) {
                    sb.append("; message = \"").append(diag.getMessage()).append("\"");
                }
                fail(sb.toString());
            } else if (record.isRecordSchema(FCS10_RECORD_SCHEMA)) {
                final LegacyClarinFCSRecordData data = (LegacyClarinFCSRecordData) record.getRecordData();
                final Resource resource = data.getResource();

                boolean foundKwic = false;
                if (resource.hasDataViews()) {
                    for (DataView dataView : resource.getDataViews()) {
                        if (dataView.isMimeType(MIME_TYPE_KWIC)) {
                            foundKwic = true;
                        }
                    }
                }
                if (resource.hasResourceFragments()) {
                    for (ResourceFragment fragment : resource.getResourceFragments()) {
                        for (DataView dataView : fragment.getDataViews()) {
                            if (dataView.isMimeType(MIME_TYPE_KWIC)) {
                                foundKwic = true;
                            }
                        }
                    }
                }
                assertTrue(foundKwic, "Endpoint did not provide mandatory KWIC dataview in results");
            }
        }
    }

    @Test
    @Order(4000)
    @ClarinFCS10
    @ClarinFCS20
    @DisplayName("Search for user specified search term and requesting endpoint to return results in CLARIN-FCS record schema '"
            + FCS_RECORD_SCHEMA + "'")
    void doFCSSearchAndRequestRecordSchema(FCSTestContext context) throws SRUClientException {
        assumeFalse(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_LEGACY, "Not check for Legacy FCS.");

        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(ClarinFCSConstants.QUERY_TYPE_CQL, escapeCQL(context.getUserSearchTerm()));
        req.setRecordSchema(FCS_RECORD_SCHEMA);
        req.setMaximumRecords(5);
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);

        assertFalse(hasDiagnostic(res, "info:srw/diagnostic/1/66"),
                "Endpoint claims to not support FCS record schema (" + FCS_RECORD_SCHEMA + ")");
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

    @Test
    @Order(4100)
    @ClarinFCS20
    @DisplayName("Advanced-Search for user specified search term and requesting endpoint to return results in CLARIN-FCS record schema '"
            + FCS_RECORD_SCHEMA + "'")
    void doFCS20SearchAndRequestRecordSchema(FCSTestContext context) throws SRUClientException {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_2_0, "Only checked for FCS 2.0.");

        // do we know if we support ADV search?
        SRUExplainRequest reqExplain = context.createExplainRequest();
        reqExplain.setExtraRequestData(ClarinFCSConstants.X_FCS_ENDPOINT_DESCRIPTION, "true");
        SRUExplainResponse resExplain = context.getClient().explain(reqExplain);
        List<ClarinFCSEndpointDescription> descs = resExplain.getExtraResponseData(ClarinFCSEndpointDescription.class);
        assertNotNull(descs, "Endpoint did not return a CLARIN FCS endpoint description");
        assertEquals(1, descs.size(),
                "Endpoint must only return one instance of a CLARIN FCS endpoint description");
        ClarinFCSEndpointDescription desc = descs.get(0);
        assertEquals(2, desc.getVersion(),
                "FCS 2.0 endpoint must provide an endpoint description with version set to \"2\"");

        boolean supportsADV = desc.getCapabilities().contains(ClarinFCSConstants.CAPABILITY_ADVANCED_SEARCH);

        // ----------------------------------------------

        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(ClarinFCSConstants.QUERY_TYPE_FCS, escapeFCS(context.getUserSearchTerm()));
        req.setRecordSchema(FCS_RECORD_SCHEMA);
        req.setMaximumRecords(5);
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);

        assertFalse(hasDiagnostic(res, "info:srw/diagnostic/1/66"),
                "Endpoint claims to not support FCS record schema (" + FCS_RECORD_SCHEMA + ")");
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

                boolean foundHits = false;
                boolean foundAdv = false;
                if (resource.hasDataViews()) {
                    for (DataView dataView : resource.getDataViews()) {
                        if (dataView.isMimeType(DataViewHits.TYPE)) {
                            foundHits = true;
                        }
                        if (dataView.isMimeType(DataViewAdvanced.TYPE)) {
                            foundAdv = true;
                        }
                    }
                }
                if (resource.hasResourceFragments()) {
                    for (ResourceFragment fragment : resource.getResourceFragments()) {
                        for (DataView dataView : fragment.getDataViews()) {
                            if (dataView.isMimeType(DataViewHits.TYPE)) {
                                foundHits = true;
                            }
                            if (dataView.isMimeType(DataViewAdvanced.TYPE)) {
                                foundAdv = true;
                            }
                        }
                    }
                }

                assertTrue(foundHits && foundAdv,
                        "Endpoint did not provide mandatory HITS and Advanced (ADV) dataviews in results");
                // TODO: do these next checks even happen?
                assertTrue(foundHits, "Endpoint did not provide mandatory HITS dataview in results");
                assertTrue(foundAdv, "Endpoint did not provide mandatory Advanced (ADV) dataview in results");
            }
        }
    }

    // ----------------------------------------------------------------------

    protected String escapeCQL(String q) {
        if (q.contains(" ")) {
            return "\"" + q + "\"";
        } else {
            return q;
        }
    }

    protected String escapeFCS(String q) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        for (int i = 0; i < q.length(); i++) {
            final char ch = q.charAt(i);
            switch (ch) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\'':
                    sb.append("\\'");
                    break;
                default:
                    sb.append(ch);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    protected String getRandomSearchTerm() {
        return randomSearchTerm;
    }

    public String getUnicodeSearchTerm() {
        return unicodeSearchTerm;
    }

}
