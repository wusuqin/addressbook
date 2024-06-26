package org.service;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "app")
public interface ApplicationProperties {
    @WithName("carddav.location")
    String addressbookLocation();
}
