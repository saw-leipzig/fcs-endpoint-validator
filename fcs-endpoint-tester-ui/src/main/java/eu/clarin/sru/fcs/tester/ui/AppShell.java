package eu.clarin.sru.fcs.tester.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

/**
 * This class is used to configure the generated html host page used by the app
 */
@PWA(name = "FCS SRU Endpoint Conformance Tester", shortName = "FCS SRU Endpoint Conformance Tester")
@Theme("fcs-endpoint-tester")
public class AppShell implements AppShellConfigurator {

}