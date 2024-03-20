package eu.clarin.sru.fcs.validator;

import java.net.URI;

import eu.clarin.sru.client.SRUClientConstants;

public final class FCSTestConstants {

    public static final URI CAPABILITY_LEX_SEARCH = URI.create("http://clarin.eu/fcs/capability/lex-search");
    public static final String QUERY_TYPE_LEX = "lex";

    public static final String X_INDENT_RESPONSE = SRUClientConstants.X_INDENT_RESPONSE;

    public static final String SEARCH_RESOURCE_HANDLE_LEGACY_PARAMETER = "x-cmd-context";
    public static final String SEARCH_RESOURCE_HANDLE_PARAMETER = "x-fcs-context";
    public static final String SEARCH_RESOURCE_HANDLE_SEPARATOR = ",";

}
