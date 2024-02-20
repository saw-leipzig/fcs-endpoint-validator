package eu.clarin.sru.fcs.validator.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class FCSEndpointValidatorServletContextListener implements ServletContextListener {
    // @WebListener - https://stackoverflow.com/a/36588744/9360161

    protected static final Logger logger = LoggerFactory.getLogger(FCSEndpointValidatorServletContextListener.class);

    // ----------------------------------------------------------------------

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("context initialized");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("context destroyed");
        FCSEndpointValidatorService.getInstance().shutdown();
    }

}