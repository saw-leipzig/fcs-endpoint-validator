package eu.clarin.sru.fcs.tester.tests;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.IncompleteExecutionException;
import org.opentest4j.TestAbortedException;

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
    @Category("explain")
    public static @interface Explain {
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Tag("scan")
    @Category("scan")
    public static @interface Scan {
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Tag("searchRetrieve")
    @Category("searchRetrieve")
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

    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Category {
        String value();
    }

    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Expected {
        String value();
    }
    // ----------------------------------------------------------------------

    /**
     * Specialization of {@link TestAbortedException} used to indicate that a test
     * was aborted during execution with a warning.
     * 
     * @see TestAbortedException
     * @see IncompleteExecutionException
     */
    public static class TestAbortedWithWarningException extends TestAbortedException {

        private static final long serialVersionUID = 1L;

        public TestAbortedWithWarningException() {
        }

        public TestAbortedWithWarningException(String message) {
            super(message);
        }

        public TestAbortedWithWarningException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static <V> V abortWithWarning() {
        throw new TestAbortedWithWarningException();
    }

    public static <V> V abortWithWarning(String message) {
        throw new TestAbortedWithWarningException(message);
    }

    public static <V> V abortWithWarning(Supplier<String> messageSupplier) {
        throw new TestAbortedWithWarningException(messageSupplier.get());
    }

    protected static void throwAbortWithWarning(String message) {
        if (StringUtils.isNotBlank(message)) {
            throw new TestAbortedWithWarningException(message);
        }
        throw new TestAbortedWithWarningException();
    }

    public static void assumeTrueElseWarn(boolean assumption, String message) throws TestAbortedException {
        if (!assumption) {
            throwAbortWithWarning(message);
        }
    }

    public static void assertEqualsElseWarn(int expected, int actual, String message) {
        // AssertEquals.assertEquals(expected, actual, message);
        if (expected != actual) {
            warnNotEqual(expected, actual, message);
        }
    }

    private static void warnNotEqual(Object expected, Object actual, Object messageOrSupplier) {
        // AssertEquals.failNotEqual
        AssertionFailedError e = assertionFailure().message(messageOrSupplier).expected(expected).actual(actual)
                .build();
        throw new TestAbortedWithWarningException(e.getMessage(), e.getCause());
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
