package com.stormpath.spring.cloud.zuul.config;

import com.stormpath.sdk.convert.Conversion;
import com.stormpath.sdk.convert.ResourceConverter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @since 1.3.0
 */
@ConfigurationProperties("stormpath.zuul.account.header")
public class StormpathZuulAccountHeaderConfig {

    @SuppressWarnings("WeakerAccess")
    public static final String DEFAULT_NAME = "X-Forwarded-Account";

    private String name;

    private Conversion value; //config for how the value should be rendered, not an actual value

    private JwtConfig jwt;

    public StormpathZuulAccountHeaderConfig() {
        this.name = DEFAULT_NAME;
        this.value = ResourceConverter.DEFAULT_CONFIG;
        this.jwt = new JwtConfig();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Conversion getValue() {
        return value;
    }

    public void setValue(Conversion value) {
        this.value = value;
    }

    public JwtConfig getJwt() {
        return jwt;
    }

    public void setJwt(JwtConfig jwt) {
        this.jwt = jwt;
    }
}
