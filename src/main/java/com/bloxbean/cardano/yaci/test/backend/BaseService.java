package com.bloxbean.cardano.yaci.test.backend;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseService {

    private final String baseUrl;
    private final String projectId;

    public BaseService(String baseUrl, String projectId) {
        this.baseUrl = baseUrl;
        this.projectId = projectId;

        if (log.isDebugEnabled()) {
            log.debug("Base URL : " + baseUrl);
        }
    }

    protected Feign.Builder getFeign() {
        return Feign.builder()
                .decoder(new JacksonDecoder());
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
