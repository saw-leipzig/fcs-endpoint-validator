package eu.clarin.sru.fcs.validator.tests;

import static eu.clarin.sru.fcs.validator.FCSTestConstants.SEARCH_RESOURCE_HANDLE_LEGACY_PARAMETER;
import static eu.clarin.sru.fcs.validator.FCSTestConstants.SEARCH_RESOURCE_HANDLE_PARAMETER;
import static eu.clarin.sru.fcs.validator.FCSTestConstants.SEARCH_RESOURCE_HANDLE_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import eu.clarin.sru.client.SRUClientConstants;
import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUDiagnostic;
import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRUExplainResponse;
import eu.clarin.sru.client.SRURecord;
import eu.clarin.sru.client.SRUSearchRetrieveRequest;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.client.SRUSurrogateRecordData;
import eu.clarin.sru.client.SRUVersion;
import eu.clarin.sru.client.fcs.ClarinFCSConstants;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription;
import eu.clarin.sru.client.fcs.ClarinFCSRecordData;
import eu.clarin.sru.client.fcs.DataView;
import eu.clarin.sru.client.fcs.DataViewAdvanced;
import eu.clarin.sru.client.fcs.DataViewGenericDOM;
import eu.clarin.sru.client.fcs.DataViewGenericString;
import eu.clarin.sru.client.fcs.DataViewHits;
import eu.clarin.sru.client.fcs.LegacyClarinFCSRecordData;
import eu.clarin.sru.client.fcs.LegacyDataViewKWIC;
import eu.clarin.sru.client.fcs.Resource;
import eu.clarin.sru.client.fcs.Resource.ResourceFragment;
import eu.clarin.sru.fcs.validator.FCSTestContext;
import eu.clarin.sru.fcs.validator.FCSTestProfile;
import eu.clarin.sru.fcs.validator.tests.AbstractFCSTest.SearchRetrieve;;

@Order(3000)
@SearchRetrieve
@DisplayName("SearchRetrieve")
public class FCSSearchTest extends AbstractFCSTest {

    private static final Logger logger = LoggerFactory.getLogger(FCSSearchTest.class);

    private static final String FCS_RECORD_SCHEMA = ClarinFCSRecordData.RECORD_SCHEMA;
    private static final String FCS10_RECORD_SCHEMA = "http://clarin.eu/fcs/1.0";
    @SuppressWarnings("deprecation")
    private static final String MIME_TYPE_KWIC = LegacyDataViewKWIC.TYPE;

    // TODO: supply via context, maybe make repeatable and change?
    private final String randomSearchTerm = RandomStringUtils.randomAlphanumeric(16);
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
    // FCS: random searches

    @Test
    @Order(3000)
    @ClarinFCSAny
    @DisplayName("Search for random string")
    @Expected("No errors or diagnostics (and zero or more records)")
    void doRandomCQLSearch(FCSTestContext context) throws SRUClientException {
        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL, escapeCQL(getRandomSearchTerm()));
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertEqualsElseWarn(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint.");
    }

    @Test
    @Order(Integer.MIN_VALUE)
    @ClarinFCSAny
    @DisplayName("Searching for random string with CLARIN FCS record schema '" + ClarinFCSRecordData.RECORD_SCHEMA
            + "'")
    @Expected("No errors or diagnostics")
    void doRandomSearchWithRecordSchema(FCSTestContext context) throws SRUClientException {
        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL, escapeCQL(getRandomSearchTerm()));
        req.setRecordSchema(ClarinFCSRecordData.RECORD_SCHEMA);
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertEqualsElseWarn(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint.");
    }

    // ----------------------------------------------------------------------
    // SRU: invalid parameters usage, CQL

    @Test
    @Order(3030)
    @ClarinFCSLegacy
    @DisplayName("Search with missing 'query' argument")
    @Expected("Expecting diagnostic \"info:srw/diagnostic/1/7\"")
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
    @Expected("Expecting diagnostic \"info:srw/diagnostic/1/71\"")
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
    @Expected("Expecting diagnostic \"info:srw/diagnostic/1/6\"")
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
    @Expected("Expecting diagnostic \"info:srw/diagnostic/1/6\"")
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
    @Expected("Expecting diagnostic \"info:srw/diagnostic/1/6\"")
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
    @Expected("Expecting diagnostic \"info:srw/diagnostic/1/6\"")
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
    @Expected("Expecting diagnostic \"info:srw/diagnostic/1/10\"")
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
    @Expected("Expecting diagnostic \"info:srw/diagnostic/1/61\"")
    void doSearchWithOutOfRangeRecordPositionArg(FCSTestContext context) throws SRUClientException {
        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL, escapeCQL(getRandomSearchTerm()));
        req.setStartRecord(Integer.MAX_VALUE);
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertHasDiagnostic(res, "info:srw/diagnostic/1/61");
    }

    // ----------------------------------------------------------------------
    // SRU: UTF-8 support

    @Test
    @Order(3200)
    @ClarinFCSAny
    @DisplayName("Search for string with non-ASCII characters encoded as UTF-8")
    @Expected("No errors or diagnostics (and zero or more records)")
    void doSearchWithNonASCIIQuery(FCSTestContext context) throws SRUClientException {
        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL, escapeCQL(getUnicodeSearchTerm()));
        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);
        assertEqualsElseWarn(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint.");
    }

    // ----------------------------------------------------------------------
    // SRU/FCS: search with user input for known results

    @Test
    @Order(3600)
    @ClarinFCSLegacy
    @DisplayName("Search for user specified search term and requesting endpoint to return results in CLARIN-FCS record schema '"
            + FCS10_RECORD_SCHEMA + "'")
    @Expected("Expecting at least one record in CLARIN-FCS legacy record schema (without any surrogate diagnostics)")
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
    @LexFCS
    @DisplayName("Search for user specified search term and requesting endpoint to return results in CLARIN-FCS record schema '"
            + FCS_RECORD_SCHEMA + "'")
    @Expected("Expecting at least one record in CLARIN-FCS record schema (without any surrogate diagnostics)")
    void doFCSSearchAndRequestRecordSchema(FCSTestContext context) throws SRUClientException {
        assumeFalse(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_LEGACY, "Not check for Legacy FCS.");

        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(ClarinFCSConstants.QUERY_TYPE_CQL, escapeCQL(context.getUserSearchTerm()));
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
    @Expected("Expecting at least one record in CLARIN-FCS record schema (without any surrogate diagnostics)")
    void doFCS20SearchAndRequestRecordSchema(FCSTestContext context) throws SRUClientException {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_2_0, "Only checked for FCS 2.0.");
        assumeTrue(endpointDescription != null, "Endpoint did not supply a valid Endpoint Description?");

        // do we support ADV search?
        boolean supportsADV = endpointDescription.getCapabilities()
                .contains(ClarinFCSConstants.CAPABILITY_ADVANCED_SEARCH);
        assumeTrue(supportsADV, "Endpoint claims no support for Advanced Search");

        // ----------------------------------------------

        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setQuery(ClarinFCSConstants.QUERY_TYPE_FCS, escapeFCS(context.getUserSearchTerm()));
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
    // Aggregator Minimum Compliance
    // TODO: do we still want to test FCS 1.0/Legacy?

    @SuppressWarnings("deprecation")
    @Test
    @Order(4900)
    @ClarinFCSLegacy
    @ClarinFCS10
    @ClarinFCS20
    @LexFCS
    @ClarinFCSForAggregator
    @Category("searchRetrieve (aggregator)")
    @DisplayName("Check for valid SearchRetreive response required for Minimum FCS Aggregator Compliance")
    @Expected("Valid SearchRetrieve response with at least one result")
    void doSearchRetrieveForFCSAggregator(FCSTestContext context) throws SRUClientException {
        // assumeTrue(context.getFCSTestProfile() == FCSTestProfile.AGGREGATOR_MIN_FCS,
        // "Only checked for Minimum FCS Aggregator Compliance.");

        boolean legacy = context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_LEGACY;

        // see Search#executeSearch(), Aggregator#startSearch, RestService#postSearch
        SRUSearchRetrieveRequest req = context.createSearchRetrieveRequest();
        req.setVersion(Optional.ofNullable(context.getFCSTestProfile().getSRUVersion()).orElse(SRUVersion.VERSION_1_2));
        req.setStartRecord(1);
        req.setMaximumRecords(10);
        req.setRecordSchema(legacy
                ? LegacyClarinFCSRecordData.RECORD_SCHEMA
                : ClarinFCSRecordData.RECORD_SCHEMA);

        // add search term
        req.setQuery(SRUClientConstants.QUERY_TYPE_CQL, escapeCQL(context.getUserSearchTerm()));

        // add resource pid(s)
        String[] pids = context.getUserResourcePids();
        if (pids != null) {
            req.setExtraRequestData(legacy
                    ? SEARCH_RESOURCE_HANDLE_LEGACY_PARAMETER
                    : SEARCH_RESOURCE_HANDLE_PARAMETER,
                    Arrays.stream(pids).collect(Collectors.joining(SEARCH_RESOURCE_HANDLE_SEPARATOR)));
        }

        SRUSearchRetrieveResponse res = context.getClient().searchRetrieve(req);

        // NOTE: we will fail if we do not get any results!
        // see Search ~#~ onSuccess()

        logger.info("searchRetrieve request url: {}", res.getRequest().getRequestedURI());

        if (res.hasDiagnostics()) {
            logger.error("diagnostic for url: {}", res.getRequest().getRequestedURI());
            for (final SRUDiagnostic diagnostic : res.getDiagnostics()) {
                logger.warn("Diagnostic: uri={}, message={}, detail={}", diagnostic.getURI(), diagnostic.getMessage(),
                        diagnostic.getDetails());
            }
        }

        assertTrue(res.hasRecords(), "Expected SearchRetrieve response to contain at least one record!");

        int nextRecordPosition = 1;
        for (final SRURecord record : res.getRecords()) {
            nextRecordPosition += 1;
            if (record.isRecordSchema(ClarinFCSRecordData.RECORD_SCHEMA)) {
                final ClarinFCSRecordData rd = (ClarinFCSRecordData) record.getRecordData();
                final Resource resource = rd.getResource();
                final String pid = resource.getPid();
                final String reference = resource.getRef();
                logger.debug("Resource ref={}, pid={}, dataViews={}", reference, pid, resource.hasDataViews());

                int countDVs = 0;
                if (resource.hasDataViews()) {
                    countDVs += aggregatorSearchResultProcessDataViews(resource.getDataViews(), pid, reference);
                }

                if (resource.hasResourceFragments()) {
                    for (final Resource.ResourceFragment fragment : resource.getResourceFragments()) {
                        logger.debug("ResourceFragment: ref={}, pid={}, dataViews={}", fragment.getRef(),
                                fragment.getPid(), fragment.hasDataViews());
                        if (fragment.hasDataViews()) {
                            countDVs += aggregatorSearchResultProcessDataViews(fragment.getDataViews(),
                                    fragment.getPid() != null ? fragment.getPid() : pid,
                                    fragment.getRef() != null ? fragment.getRef() : reference);
                        }
                    }
                }
                assertTrue(countDVs > 0, "SearchRetrieve should have at least returned one DataView!");
                logger.info("Number of (non-generic) Data Views found: {}", countDVs);

            } else if (record.isRecordSchema(SRUSurrogateRecordData.RECORD_SCHEMA)) {
                SRUSurrogateRecordData r = (SRUSurrogateRecordData) record.getRecordData();
                logger.warn("Surrogate diagnostic: uri={}, message={}, detail={}", r.getURI(), r.getMessage(),
                        r.getDetails());
            } else {
                logger.warn("Unsupported schema: {}", record.getRecordSchema());
            }
        }

        logger.info("NextRecordPosition (computed): {}", nextRecordPosition);
        logger.info("NextRecordPosition (SearchRetrieve Response): {}", res.getNextRecordPosition());
        logger.info("NumberOfRecords: {}", res.getNumberOfRecords());
    }

    // from eu.clarin.sru.fcs.aggregator.search.Result
    private int aggregatorSearchResultProcessDataViews(List<DataView> dataViews, String pid, String reference) {
        int countNonGenericDVs = 0;
        for (DataView dataview : dataViews) {
            if (dataview instanceof DataViewGenericDOM) {
                final DataViewGenericDOM view = (DataViewGenericDOM) dataview;
                final Node root = view.getDocument().getFirstChild();
                logger.debug("DataView (generic dom): root element <{}> / {}", root.getNodeName(),
                        root.getOwnerDocument().hashCode());
            } else if (dataview instanceof DataViewGenericString) {
                final DataViewGenericString view = (DataViewGenericString) dataview;
                logger.debug("DataView (generic string): data = {}", view.getContent());
            } else if (dataview instanceof DataViewHits) {
                final DataViewHits hits = (DataViewHits) dataview;
                logger.debug("DataViewHits: hit-count = {}", hits.getHitCount());
                // Kwic kwic = new Kwic(hits, pid, reference);
                // kwics.add(kwic);
                // logger.debug("DataViewHits: {}", kwic.getFragments());
                countNonGenericDVs += 1;
            } else if (dataview instanceof DataViewAdvanced) {
                final DataViewAdvanced adv = (DataViewAdvanced) dataview;
                logger.debug("DataViewAdvanced: num-layers = {}", adv.getLayers().size());
                // List<AdvancedLayer> advLayersSingleGroup = new ArrayList<>();
                for (DataViewAdvanced.Layer layer : adv.getLayers()) {
                    logger.debug("DataViewAdvanced layer: {}", adv.getUnit(), layer.getId());
                    // AdvancedLayer aLayer = new AdvancedLayer(layer, pid, reference);
                    // advLayersSingleGroup.add(aLayer);
                }
                // advLayers.add(advLayersSingleGroup);
                countNonGenericDVs += 1;
            }
        }
        return countNonGenericDVs;
    }

    // ----------------------------------------------------------------------

    // TODO: validate ADV dataview
    // - layer ids
    // - empty / missing spans

    // TODO: validate CQL search
    // - search term appears in search results
    // - [warning] check if result set deterministic
    // -> (requests for 1. 1-5, 2. 2-5, 3. 6-10)

    // TODO: split into multiple search tests
    // - sru/cql
    // - ADV
    // - Lex

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
