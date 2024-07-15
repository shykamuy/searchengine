package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "indexing-settings")
public class SitesListConfig {
    private List<SiteConfig> sites;

}
