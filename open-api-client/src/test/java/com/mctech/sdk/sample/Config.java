package com.mctech.sdk.sample;

import java.util.Properties;

class Config extends Properties {
    public String getAccessId() {
        return (String) this.getOrDefault("credential.accessId", "{accessId}");
    }

    public String getSecretKey() {
        return (String) this.getOrDefault("credential.secretKey", "{secretKey}");
    }

    public String getBaseUrl() {
        return (String) this.getOrDefault("baseUrl", "{baseUrl}");
    }

    public String getApiPath() {
        return (String) this.getOrDefault("apiPath", "{apiPath}");
    }

    public String getIntegrationId() {
        return (String) this.getOrDefault("integrationId", "{integratedId}");
    }
}
