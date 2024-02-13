package eu.clarin.sru.fcs.tester;

import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class FCSTestContextParameterResolver implements ParameterResolver {

    public static final String PROPERTY_TEST_CONTEXT_FACTORY_ID = FCSTestContextParameterResolver.class.getName()
            + ":TEST_CONTEXT_FACTORY_ID";

    private FCSTestContextFactory contextFactory;

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (parameterContext.getParameter().getType() == FCSTestContext.class) {
            return true;
        }

        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (parameterContext.getParameter().getType() == FCSTestContext.class) {
            return getFCSTestContextFactory(extensionContext).newFCSTestContext();
        }
        return null;
    }

    protected FCSTestContextFactory getFCSTestContextFactory(ExtensionContext extensionContext) {
        if (contextFactory == null) {
            // first check if the JUnit (Discorvery)Launcher has a possible factory id
            Optional<String> maybeFactoryId = extensionContext
                    .getConfigurationParameter(PROPERTY_TEST_CONTEXT_FACTORY_ID);
            if (maybeFactoryId.isPresent()) {
                // if so, try to get the factory if it exists
                final String factoryId = maybeFactoryId.get();
                contextFactory = FCSTestContextFactoryStore.get(factoryId);
            }
            // if no factory found or valid, create a new default one
            if (contextFactory == null) {
                contextFactory = FCSTestContextFactory.newInstance();
            }
        }
        return contextFactory;
    }

}
