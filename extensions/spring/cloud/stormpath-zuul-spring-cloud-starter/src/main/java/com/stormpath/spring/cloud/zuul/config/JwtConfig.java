package com.stormpath.spring.cloud.zuul.config;

import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Map;

/**
 * @since 1.3.0
 */
public class JwtConfig {

    private boolean enabled;

    private Map<String, ?> header;

    private Map<String, ?> claims;

    private String valueClaimName;

    private SignatureAlgorithm signatureAlgorithm;

    private Map<String, ?> key;

    public JwtConfig() {
        this.enabled = true;
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

    public String getValueClaimName() {
        return valueClaimName;
    }

    public void setValueClaimName(String valueClaimName) {
        this.valueClaimName = valueClaimName;
    }

    public Map<String, ?> getKey() {
        return key;
    }

    public void setKey(Map<String, ?> key) {
        this.key = key;
    }
}