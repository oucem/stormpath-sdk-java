package com.stormpath.spring.cloud.zuul.config;

/**
 * @since 1.3.0
 */
public class ValueClaimConfig {

    private boolean enabled;

    private String name;

    public ValueClaimConfig() {
        this.enabled = true;
        this.name = "account";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
