package eu.clarin.sru.fcs.validator.ui;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.Theme;

/**
 * This class is used to configure the generated html host page used by the app
 */
@SpringBootApplication
@ServletComponentScan
@Push
@Theme(value = "fcs-endpoint-validator") // variant=Lumo.DARK
public class AppShell implements AppShellConfigurator {

    private static final long serialVersionUID = 6839932476927563900L;

    static {
        // @formatter:off
        // this seems to throw errors but still change the log level as wanted?
        // try (InputStream is = FCSEndpointValidator.class.getClassLoader().getResourceAsStream("logging.properties")) {
        //     java.util.logging.LogManager.getLogManager().readConfiguration(is);
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
        // @formatter:on

        if (!SLF4JBridgeHandler.isInstalled()) {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
        }
        // @formatter:off
        // java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME).setLevel(Level.ALL);
        // java.util.logging.Logger.getLogger("").setLevel(Level.ALL);

        // String name = AbstractFCSTest.class.getPackageName();
        // org.slf4j.LoggerFactory.getLogger(AppShellConfigurator.class.getName()).info("test package: {}", name);
        // @formatter:on
    }

    @Override
    public void configurePage(AppShellSettings settings) {
        // set favicon
        // NOTE: if @PWA used, it creates lots of default icons ...
        settings.addFavIcon("icon", "themes/fcs-endpoint-validator/images/favicon.ico", "32x32");
        settings.addLink("shortcut icon", "themes/fcs-endpoint-validator/images/favicon.ico");

        // metadata
        settings.addMetaTag("author", "Erik Körner");
        settings.addMetaTag("author", "Sächsische Akademie der Wissenschaften zu Leipzig");
        settings.addMetaTag("description", "An FCS SRU Endpoint Validator to check protocol conformity of"
                + " Federated Search Content Endpoint Implementations by testing responses for given request URLs.");
        settings.addMetaTag("keywords", "FCS, SRU, Endpoint, Validator, Federated Content Search, FCS Endpoint");

        // crawler settings
        settings.addMetaTag("robots", "index,nofollow");

        settings.setPageTitle("FCS SRU Endpoint Validator");
    }

    public static void main(String[] args) {
        SpringApplication.run(AppShell.class, args);
    }

}