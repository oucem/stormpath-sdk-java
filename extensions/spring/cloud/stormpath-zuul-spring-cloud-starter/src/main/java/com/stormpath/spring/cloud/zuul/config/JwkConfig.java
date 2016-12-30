package com.stormpath.spring.cloud.zuul.config;

/**
 * @since 1.3.0
 */
public class JwkConfig {

    private String alg;

    private boolean enabled;

    private String encoding;

    private String k;

    private String kid;

    public JwkConfig() {
        this.enabled = true;
    }

    public String getAlg() {
        return alg;
    }

    public void setAlg(String alg) {
        this.alg = alg;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getK() {
        return k;
    }

    public void setK(String k) {
        this.k = k;
    }

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }
}
