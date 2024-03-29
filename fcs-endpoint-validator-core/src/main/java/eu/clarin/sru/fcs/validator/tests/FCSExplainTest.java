package eu.clarin.sru.fcs.validator.tests;

import static eu.clarin.sru.client.fcs.ClarinFCSConstants.CAPABILITY_ADVANCED_SEARCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUExplainRequest;
import eu.clarin.sru.client.SRUExplainResponse;
import eu.clarin.sru.client.SRUExtraResponseData;
import eu.clarin.sru.client.fcs.ClarinFCSConstants;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription;
import eu.clarin.sru.client.fcs.ClarinFCSEndpointDescription.ResourceInfo;
import eu.clarin.sru.client.fcs.DataViewAdvanced;
import eu.clarin.sru.client.fcs.DataViewHits;
import eu.clarin.sru.fcs.validator.FCSTestContext;
import eu.clarin.sru.fcs.validator.FCSTestProfile;
import eu.clarin.sru.fcs.validator.tests.AbstractFCSTest.Explain;
import eu.clarin.sru.fcs.validator.util.LanguagesISO693;

@Order(1000)
@Explain
@DisplayName("Explain")
public class FCSExplainTest extends AbstractFCSTest {

    private static final Logger logger = LoggerFactory.getLogger(FCSExplainTest.class);

    // ----------------------------------------------------------------------
    // SRU: default explain response on base URI

    @Test
    @Order(1000)
    @ClarinFCSAny
    @DisplayName("Regular explain request using default version")
    @Expected("No errors or diagnostics")
    void doDefaultExplain(FCSTestContext context) throws SRUClientException {
        SRUExplainRequest req = context.createExplainRequest();
        SRUExplainResponse res = context.getClient().explain(req);
        assertEqualsElseWarn(0, res.getDiagnosticsCount(), "One or more unexpected diagnostic reported by endpoint.");
    }

    // ----------------------------------------------------------------------
    // SRU: invalid parameter usage

    @Test
    @Order(1010)
    @ClarinFCSAnyOld
    @DisplayName("Explain without 'operation' and 'version' arguments")
    @Expected("An plain explain response with SRU version choosen by server")
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
    @Expected("An plain explain response with SRU version choosen by server")
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
    @Expected("An plain explain response with SRU version argument")
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
    @Expected("Expecting diagnostic \"info:srw/diagnostic/1/5\"")
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

    // ----------------------------------------------------------------------
    // FCS: EndpointDescription SRU:recordData

    @Test
    @Order(1100)
    @ClarinFCS10
    @DisplayName("Check for a valid FCS endpoint description")
    @Expected("Expecting exactly one valid FCS endpoint decription conforming to FCS 1.0 spec")
    void doExplainHasValidEndpointDescriptionInFCS10(FCSTestContext context) throws SRUClientException {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_1_0, "Only checked for FCS 1.0.");

        SRUExplainRequest req = context.createExplainRequest();
        req.setExtraRequestData(ClarinFCSConstants.X_FCS_ENDPOINT_DESCRIPTION, "true");
        SRUExplainResponse res = context.getClient().explain(req);

        List<ClarinFCSEndpointDescription> descs = res.getExtraResponseData(ClarinFCSEndpointDescription.class);
        assertNotNull(descs, "Endpoint did not return a CLARIN FCS endpoint description");
        assertEquals(1, descs.size(),
                "Endpoint must only return one instance of a CLARIN FCS endpoint description");

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
                "Endpoint must declare support for Generic Hits dataview (mimeType = "
                        + DataViewHits.TYPE + ")");

        assertNotEquals(0, desc.getResources().size(),
                "No resources declared. Endpoint must declare at least one Resource");
    }

    @Test
    @Order(1100)
    @ClarinFCS20
    @LexFCS
    @DisplayName("Check for a valid FCS endpoint description")
    @Expected("Expecting exactly one valid FCS endpoint decription conforming to FCS 2.0 spec")
    void doExplainHasValidEndpointDescriptionInFCS20(FCSTestContext context) throws SRUClientException {
        assumeTrue(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_2_0
                || context.getFCSTestProfile() == FCSTestProfile.LEX_FCS, "Only checked for FCS 2.0.");

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

        boolean supportsADV = desc.getCapabilities().contains(ClarinFCSConstants.CAPABILITY_ADVANCED_SEARCH);

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
                "Endpoint must declare support for Generic Hits dataview (mimeType = "
                        + DataViewHits.TYPE + ")");
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

    // ----------------------------------------------------------------------
    // FCS: mandatory en/eng localized strings for resources (title, description,
    // institution)
    // NOTE: this is already check when parsing the EndpointDescription but only
    // logged as warning

    @Test
    // @Disabled
    @Order(1300)
    @ClarinFCS10
    @ClarinFCS20
    @LexFCS
    @DisplayName("Check for a required english localized strings in FCS endpoint description")
    @Expected("Expecting exactly one valid FCS endpoint decription conforming to FCS 2.0 spec (All resources should have at least on string with 'en'/'eng' 'xml:lang' tag.)")
    void doExplainHasValidEnglishLocalizedResourceStringsInEndpointDescription(FCSTestContext context)
            throws SRUClientException {
        assumeFalse(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_LEGACY,
                "Legacy FCS does not have an endpoint description");

        SRUExplainRequest req = context.createExplainRequest();
        req.setExtraRequestData(ClarinFCSConstants.X_FCS_ENDPOINT_DESCRIPTION, "true");
        SRUExplainResponse res = context.getClient().explain(req);

        // common sanity checks
        List<ClarinFCSEndpointDescription> descs = res.getExtraResponseData(ClarinFCSEndpointDescription.class);
        assertNotNull(descs, "Endpoint did not return a CLARIN FCS endpoint description");
        assertEquals(1, descs.size(),
                "Endpoint must only return one instance of a CLARIN FCS endpoint description");
        ClarinFCSEndpointDescription desc = descs.get(0);

        // validate FCS Endpoint Description Resource elements
        assertHasEnglishStringsInResourcesRecursive(desc.getResources(), context.isStrictMode());
    }

    private void assertHasEnglishStringsInResourcesRecursive(List<ResourceInfo> resources, boolean strict) {
        if (resources == null) {
            return;
        }

        // do we only allow two letter codes (ISO639-1) or also three letter codes
        // (ISO639-2/3)?
        // https://www.loc.gov/standards/iso639-2/faq.html#1
        // xml:lang --> ISO 639.2,
        // https://www.data2type.de/xml-xslt-xslfo/xml/xml-in-a-nutshell/internationalisierung/xml-lang
        // XML XSD is also not quite clear...
        final Set<String> REQUIRED_LANGS = Set.of("en", "eng");

        for (ResourceInfo resource : resources) {
            boolean hasEngTitle = resource.getTitle().keySet().stream().anyMatch(lang -> REQUIRED_LANGS.contains(lang));
            if (strict) {
                assertTrue(hasEngTitle,
                        String.format("Missing required english title in Resource %s", resource.getPid()));
            } else {
                assumeTrueElseWarn(hasEngTitle,
                        String.format("Missing required english title in Resource %s", resource.getPid()));
            }

            if (resource.getDescription() != null) {
                boolean hasEngDescription = resource.getDescription().keySet().stream()
                        .anyMatch(lang -> REQUIRED_LANGS.contains(lang));
                if (strict) {
                    assertTrue(hasEngDescription,
                            String.format("Missing required english description in Resource %s", resource.getPid()));
                } else {
                    assumeTrueElseWarn(hasEngDescription,
                            String.format("Missing required english description in Resource %s", resource.getPid()));
                }
            }

            if (resource.getInstitution() != null) {
                boolean hasEngInstitution = resource.getInstitution().keySet().stream()
                        .anyMatch(lang -> REQUIRED_LANGS.contains(lang));
                if (strict) {
                    assertTrue(hasEngInstitution,
                            String.format("Missing required english institution in Resource %s", resource.getPid()));
                } else {
                    assumeTrueElseWarn(hasEngInstitution,
                            String.format("Missing required english institution in Resource %s", resource.getPid()));
                }
            }

            assertHasEnglishStringsInResourcesRecursive(resource.getSubResources(), strict);
        }
    }

    // ----------------------------------------------------------------------
    // FCS: valid language codes in EndpointDescription > Resources

    @Test
    @Order(1400)
    @ClarinFCS10
    @ClarinFCS20
    @LexFCS
    @DisplayName("Check for valid ISO639-3 language codes in <Resource> in FCS endpoint description")
    @Expected("Expecting valid FCS endpoint description with all Resource elements having valid ISO639-3 language codes.")
    void doExplainResourcesHaveValidLanguageCodes(FCSTestContext context) throws SRUClientException {
        assumeFalse(context.getFCSTestProfile() == FCSTestProfile.CLARIN_FCS_LEGACY,
                "Legacy FCS does not have an endpoint description");

        SRUExplainRequest req = context.createExplainRequest();
        req.setExtraRequestData(ClarinFCSConstants.X_FCS_ENDPOINT_DESCRIPTION, "true");
        SRUExplainResponse res = context.getClient().explain(req);

        // common sanity checks
        List<ClarinFCSEndpointDescription> descs = res.getExtraResponseData(ClarinFCSEndpointDescription.class);
        assertNotNull(descs, "Endpoint did not return a CLARIN FCS endpoint description");
        assertEquals(1, descs.size(), "Endpoint must only return one instance of a CLARIN FCS endpoint description");
        ClarinFCSEndpointDescription desc = descs.get(0);

        // validate FCS Endpoint Description Resource elements
        assertValidLanguageCodesForResourcesRecursive(desc.getResources());
    }

    private void assertValidLanguageCodesForResourcesRecursive(List<ResourceInfo> resources) {
        if (resources == null) {
            return;
        }

        LanguagesISO693 iso639 = LanguagesISO693.getInstance();
        for (ResourceInfo resource : resources) {
            assertNotEquals(0, resource.getLanguages().size(), "Languages list must not be empty!");

            List<String> invalidLanguages = resource.getLanguages().stream().filter(Predicate.not(iso639::isCode_3))
                    .collect(Collectors.toList());
            if (!invalidLanguages.isEmpty()) {
                fail(String.format(
                        "Resource must not contain invalid ISO639-3 language code. Expected none but found unknown codes: %s.",
                        invalidLanguages.stream().collect(Collectors.joining("\", \"", "\"", "\""))));
            }

            assertValidLanguageCodesForResourcesRecursive(resource.getSubResources());
        }
    }

    // ----------------------------------------------------------------------
    // Aggregator Minimum Compliance
    // NOTE: legacy fcs with scan is not tested here
    // TODO: do we still want to test FCS 1.0?

    @Test
    @Order(1900)
    @ClarinFCS10
    @ClarinFCS20
    @LexFCS
    @ClarinFCSForAggregator
    @Category("explain (aggregator)")
    @DisplayName("Check for valid Explain response required for Minimum FCS Aggregator Compliance")
    @Expected("Valid Explain response with at least one resource")
    void doExplainForFCSAggregator(FCSTestContext context) throws SRUClientException {
        // assumeTrue(context.getFCSTestProfile() == FCSTestProfile.AGGREGATOR_MIN_FCS,
        // "Only checked for Minimum FCS Aggregator Compliance.");

        // see ScanCrawler#start()
        SRUExplainRequest req = context.createExplainRequest();
        req.setExtraRequestData(ClarinFCSConstants.X_FCS_ENDPOINT_DESCRIPTION, "true");
        req.setParseRecordDataEnabled(true);
        SRUExplainResponse res = context.getClient().explain(req);

        // NOTE: we will fail if we do not find any resources!
        // see ScanCrawler#onSuccess()
        assertTrue(res.hasExtraResponseData(), "Expected ExtraResponseData on Explain response");
        logger.debug("Explain request url: {}", res.getRequest().getRequestedURI());

        boolean foundClarinFCSEndpointDescription = false;
        for (SRUExtraResponseData data : res.getExtraResponseData()) {
            if (data instanceof ClarinFCSEndpointDescription) {
                foundClarinFCSEndpointDescription = true;

                ClarinFCSEndpointDescription desc = (ClarinFCSEndpointDescription) data;

                if (desc.getVersion() == 2) {
                    logger.info("Would set endpoint FCS protocol version to '2'");
                    if (desc.getCapabilities().contains(CAPABILITY_ADVANCED_SEARCH)) {
                        logger.info("Would add 'adv' search capability to endpoint");
                    }
                } else {
                    logger.info("Would set endpoint FCS protocol version to '1'");
                }

                assertNotNull(desc.getResources());
                // NOTE: we do not recurse through those resources for now
            }
        }
        assertTrue(foundClarinFCSEndpointDescription,
                "ExtraResponseData with a ClarinFCSEndpointDescription is required!");
        // NOTE: it should theoretically be allowed to return multiple
        // ClarinFCSEndpointDescription records
    }

}
