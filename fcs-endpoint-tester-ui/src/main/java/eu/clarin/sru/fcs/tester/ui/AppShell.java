package eu.clarin.sru.fcs.tester.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
// import com.vaadin.flow.theme.lumo.Lumo;

/**
 * This class is used to configure the generated html host page used by the app
 */
@PWA(name = "FCS SRU Endpoint Conformance Tester", shortName = "FCS SRU Endpoint Conformance Tester")
@Theme(value = "fcs-endpoint-tester") // variant=Lumo.DARK
public class AppShell implements AppShellConfigurator {

}