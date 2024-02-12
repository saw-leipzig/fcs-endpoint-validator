package eu.clarin.sru.fcs.tester.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import eu.clarin.sru.client.SRUDiagnostic;
import eu.clarin.sru.client.SRUExplainResponse;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.client.SRUSearchRetrieveResponse;
import eu.clarin.sru.fcs.tester.FCSTestContext;
import eu.clarin.sru.fcs.tester.FCSTestContextParameterResolver;
import eu.clarin.sru.fcs.tester.FCSTestProfile;

@ExtendWith(FCSTestContextParameterResolver.class)
public abstract class AbstractFCSTest {

    protected boolean isLegacyFCS(FCSTestContext context) {
        // https://github.com/junit-team/junit5/blob/r5.10.2/junit-jupiter-engine/src/main/java/org/junit/jupiter/engine/extension/DisabledCondition.java
        // https://github.com/junit-team/junit5/blob/r5.10.2/junit-jupiter-api/src/main/java/org/junit/jupiter/api/Disabled.java
        // https://github.com/junit-team/junit5/blob/r5.10.2/junit-jupiter-api/src/main/java/org/junit/jupiter/api/Assumptions.java
        // https://github.com/junit-team/junit5/blob/r5.10.2/junit-jupiter-api/src/main/java/org/junit/jupiter/api/Assertions.java
        return false;
    }

    // ----------------------------------------------------------------------

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Tag("explain")
    public static @interface Explain {
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Tag("scan")
    public static @interface Scan {
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Tag("searchRetrieve")
    public static @interface SearchRetrieve {
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Tag("clarin-fcs-legacy")
    public static @interface ClarinFCSLegacy {
        public static final FCSTestProfile profile = FCSTestProfile.CLARIN_FCS_LEGACY;
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Tag("clarin-fcs-1.0")
    public static @interface ClarinFCS10 {
        public static final FCSTestProfile profile = FCSTestProfile.CLARIN_FCS_1_0;
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Tag("clarin-fcs-2.0")
    public static @interface ClarinFCS20 {
        public static final FCSTestProfile profile = FCSTestProfile.CLARIN_FCS_2_0;
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @ClarinFCS20
    @ClarinFCS10
    @ClarinFCSLegacy
    public static @interface ClarinFCSAny {
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @ClarinFCS10
    @ClarinFCSLegacy
    public static @interface ClarinFCSAnyOld {
    }

    // ----------------------------------------------------------------------

    protected boolean hasDiagnostic(SRUExplainResponse res, String uri) {
        return hasDiagnostic(res.getDiagnostics(), uri);
    }

    protected boolean hasDiagnostic(SRUScanResponse res, String uri) {
        return hasDiagnostic(res.getDiagnostics(), uri);
    }

    protected boolean hasDiagnostic(SRUSearchRetrieveResponse res, String uri) {
        return hasDiagnostic(res.getDiagnostics(), uri);
    }

    protected boolean hasDiagnostic(List<SRUDiagnostic> diagnostics, String uri) {
        if (diagnostics != null) {
            for (SRUDiagnostic diagnostic : diagnostics) {
                if (uri.equals(diagnostic.getURI())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isDiagnostic(SRUDiagnostic d, String uri) {
        return uri.equals(d.getURI());
    }

    // ----------------------------------------------------------------------

    protected void assertHasDiagnostic(SRUExplainResponse res, String uri) {
        assertTrue(hasDiagnostic(res, uri), String.format("Endpoint did not report expected diagnostic: %s", uri));
    }

    protected void assertHasDiagnostic(SRUScanResponse res, String uri) {
        assertTrue(hasDiagnostic(res, uri), String.format("Endpoint did not report expected diagnostic: %s", uri));
    }

    protected void assertHasDiagnostic(SRUSearchRetrieveResponse res, String uri) {
        assertTrue(hasDiagnostic(res, uri), String.format("Endpoint did not report expected diagnostic: %s", uri));
    }

}
