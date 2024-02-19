package eu.clarin.sru.fcs.validator;

import java.util.HashMap;
import java.util.Map;

public final class FCSTestContextFactoryStore {

    /**
     * Mapping of factory id (UUID for test session) to factory instance.
     */
    private static final Map<String, FCSTestContextFactory> factories = new HashMap<>();

    // ----------------------------------------------------------------------

    public static FCSTestContextFactory get(String id) {
        synchronized (factories) {
            return factories.get(id);
        }
    }

    public static void set(String id, FCSTestContextFactory factory) {
        synchronized (factories) {
            factories.put(id, factory);
        }
    }

    public static FCSTestContextFactory remove(String id) {
        synchronized (factories) {
            return factories.remove(id);
        }
    }

    public static void clear(String id) {
        synchronized (factories) {
            factories.clear();
        }
    }

}
