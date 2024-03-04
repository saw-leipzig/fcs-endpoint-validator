package eu.clarin.sru.fcs.validator.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@EnableConfigurationProperties(FCSEndpointValidatorProperties.class)
public class FCSEndpointValidatorConfiguration {

    // @Autowired
    // private Environment env;

}
