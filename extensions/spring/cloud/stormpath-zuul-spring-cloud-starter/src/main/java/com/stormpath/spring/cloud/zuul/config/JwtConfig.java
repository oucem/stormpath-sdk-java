package com.stormpath.spring.cloud.zuul.config;

import java.util.Map;

/**
 * @since 1.3.0
 */
public class JwtConfig {

    private boolean enabled;

    private Map<String, ?> header;

    private Map<String, ?> claims;

    private ValueClaimConfig valueClaim;

    private JwkConfig key;

    public JwtConfig() {
        this.enabled = true;
        this.valueClaim = new ValueClaimConfig();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, ?> getHeader() {
        return header;
    }

    public void setHeader(Map<String, ?> header) {
        this.header = header;
    }

    public Map<String, ?> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, ?> claims) {
        this.claims = claims;
    }

    public ValueClaimConfig getValueClaim() {
        return valueClaim;
    }

    public void setValueClaim(ValueClaimConfig valueClaim) {
        this.valueClaim = valueClaim;
    }

    public JwkConfig getKey() {
        return key;
    }

    public void setKey(JwkConfig key) {
        this.key = key;
    }
}