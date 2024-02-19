package eu.clarin.sru.fcs.validator.ui;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * This class is used to configure the generated html host page used by the app
 */
@PWA(name = "FCS SRU Endpoint Validator", shortName = "FCS SRU Endpoint Validator")
@SpringBootApplication
@ServletComponentScan
@Push
// @EnableAsync
@Theme(value = "fcs-endpoint-validator") // variant=Lumo.DARK
public class AppShell implements AppShellConfigurator {

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

    public static void main(String[] args) {
        SpringApplication.run(AppShell.class, args);
    }

}