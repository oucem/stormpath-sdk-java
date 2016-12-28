/*
 * Copyright 2015 Stormpath, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stormpath.spring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stormpath.sdk.account.Account;
import com.stormpath.sdk.application.Application;
import com.stormpath.sdk.authc.AuthenticationResult;
import com.stormpath.sdk.cache.Cache;
import com.stormpath.sdk.client.Client;
import com.stormpath.sdk.idsite.IdSiteResultListener;
import com.stormpath.sdk.lang.Assert;
import com.stormpath.sdk.lang.BiPredicate;
import com.stormpath.sdk.lang.Collections;
import com.stormpath.sdk.lang.Strings;
import com.stormpath.sdk.saml.SamlResultListener;
import com.stormpath.sdk.servlet.account.AccountResolver;
import com.stormpath.sdk.servlet.account.DefaultAccountResolver;
import com.stormpath.sdk.servlet.application.ApplicationResolver;
import com.stormpath.sdk.servlet.application.DefaultApplicationResolver;
import com.stormpath.sdk.servlet.authz.RequestAuthorizer;
import com.stormpath.sdk.servlet.config.CookieConfig;
import com.stormpath.sdk.servlet.config.RegisterEnabledPredicate;
import com.stormpath.sdk.servlet.config.RegisterEnabledResolver;
import com.stormpath.sdk.servlet.config.impl.AccessTokenCookieConfig;
import com.stormpath.sdk.servlet.config.impl.RefreshTokenCookieConfig;
import com.stormpath.sdk.servlet.csrf.CsrfTokenManager;
import com.stormpath.sdk.servlet.csrf.DefaultCsrfTokenManager;
import com.stormpath.sdk.servlet.csrf.DisabledCsrfTokenManager;
import com.stormpath.sdk.servlet.event.RequestEvent;
import com.stormpath.sdk.servlet.event.RequestEventListener;
import com.stormpath.sdk.servlet.event.RequestEventListenerAdapter;
import com.stormpath.sdk.servlet.event.TokenRevocationRequestEventListener;
import com.stormpath.sdk.servlet.event.impl.Publisher;
import com.stormpath.sdk.servlet.event.impl.RequestEventPublisher;
import com.stormpath.sdk.servlet.filter.ContentNegotiationResolver;
import com.stormpath.sdk.servlet.filter.ControllerConfig;
import com.stormpath.sdk.servlet.filter.DefaultContentNegotiationResolver;
import com.stormpath.sdk.servlet.filter.DefaultFilterChainManager;
import com.stormpath.sdk.servlet.filter.DefaultLoginPageRedirector;
import com.stormpath.sdk.servlet.filter.DefaultServerUriResolver;
import com.stormpath.sdk.servlet.filter.DefaultUsernamePasswordRequestFactory;
import com.stormpath.sdk.servlet.filter.DefaultWrappedServletRequestFactory;
import com.stormpath.sdk.servlet.filter.FilterChainManager;
import com.stormpath.sdk.servlet.filter.FilterChainResolver;
import com.stormpath.sdk.servlet.filter.Filters;
import com.stormpath.sdk.servlet.filter.MeFilter;
import com.stormpath.sdk.servlet.filter.PathMatchingFilterChainResolver;
import com.stormpath.sdk.servlet.filter.PrioritizedFilterChainResolver;
import com.stormpath.sdk.servlet.filter.ServerUriResolver;
import com.stormpath.sdk.servlet.filter.StormpathFilter;
import com.stormpath.sdk.servlet.filter.UsernamePasswordRequestFactory;
import com.stormpath.sdk.servlet.filter.WrappedServletRequestFactory;
import com.stormpath.sdk.servlet.filter.account.AccountResolverFilter;
import com.stormpath.sdk.servlet.filter.account.AuthenticationResultSaver;
import com.stormpath.sdk.servlet.filter.account.AuthorizationHeaderAccountResolver;
import com.stormpath.sdk.servlet.filter.account.CookieAccountResolver;
import com.stormpath.sdk.servlet.filter.account.CookieAuthenticationResultSaver;
import com.stormpath.sdk.servlet.filter.account.DefaultJwtAccountResolver;
import com.stormpath.sdk.servlet.filter.account.JwtAccountResolver;
import com.stormpath.sdk.servlet.filter.account.JwtSigningKeyResolver;
import com.stormpath.sdk.servlet.filter.mvc.ControllerFilter;
import com.stormpath.sdk.servlet.filter.oauth.AccessTokenAuthenticationRequestFactory;
import com.stormpath.sdk.servlet.filter.oauth.AccessTokenResultFactory;
import com.stormpath.sdk.servlet.filter.oauth.DefaultAccessTokenAuthenticationRequestFactory;
import com.stormpath.sdk.servlet.filter.oauth.DefaultAccessTokenRequestAuthorizer;
import com.stormpath.sdk.servlet.filter.oauth.DefaultAccessTokenResultFactory;
import com.stormpath.sdk.servlet.filter.oauth.DefaultRefreshTokenAuthenticationRequestFactory;
import com.stormpath.sdk.servlet.filter.oauth.DefaultRefreshTokenResultFactory;
import com.stormpath.sdk.servlet.filter.oauth.OriginAccessTokenRequestAuthorizer;
import com.stormpath.sdk.servlet.filter.oauth.RefreshTokenAuthenticationRequestFactory;
import com.stormpath.sdk.servlet.filter.oauth.RefreshTokenResultFactory;
import com.stormpath.sdk.servlet.form.Field;
import com.stormpath.sdk.servlet.http.InvalidMediaTypeException;
import com.stormpath.sdk.servlet.http.MediaType;
import com.stormpath.sdk.servlet.http.Resolver;
import com.stormpath.sdk.servlet.http.Saver;
import com.stormpath.sdk.servlet.http.authc.AccountStoreResolver;
import com.stormpath.sdk.servlet.http.authc.AuthorizationHeaderAuthenticator;
import com.stormpath.sdk.servlet.http.authc.BasicAuthenticationScheme;
import com.stormpath.sdk.servlet.http.authc.BearerAuthenticationScheme;
import com.stormpath.sdk.servlet.http.authc.DisabledAccountStoreResolver;
import com.stormpath.sdk.servlet.http.authc.HeaderAuthenticator;
import com.stormpath.sdk.servlet.http.authc.HttpAuthenticationScheme;
import com.stormpath.sdk.servlet.i18n.DefaultMessageContext;
import com.stormpath.sdk.servlet.i18n.MessageContext;
import com.stormpath.sdk.servlet.idsite.DefaultIdSiteOrganizationResolver;
import com.stormpath.sdk.servlet.idsite.IdSiteOrganizationContext;
import com.stormpath.sdk.servlet.mvc.AbstractController;
import com.stormpath.sdk.servlet.mvc.AbstractSocialCallbackController;
import com.stormpath.sdk.servlet.mvc.AccessTokenController;
import com.stormpath.sdk.servlet.mvc.ChangePasswordController;
import com.stormpath.sdk.servlet.mvc.ContentNegotiatingFieldValueResolver;
import com.stormpath.sdk.servlet.mvc.Controller;
import com.stormpath.sdk.servlet.mvc.DefaultExpandsResolver;
import com.stormpath.sdk.servlet.mvc.DefaultViewResolver;
import com.stormpath.sdk.servlet.mvc.DisabledWebHandler;
import com.stormpath.sdk.servlet.mvc.ErrorModelFactory;
import com.stormpath.sdk.servlet.mvc.ExpandsResolver;
import com.stormpath.sdk.servlet.mvc.ForgotPasswordController;
import com.stormpath.sdk.servlet.mvc.FormController;
import com.stormpath.sdk.servlet.mvc.IdSiteController;
import com.stormpath.sdk.servlet.mvc.IdSiteLogoutController;
import com.stormpath.sdk.servlet.mvc.IdSiteResultController;
import com.stormpath.sdk.servlet.mvc.JacksonView;
import com.stormpath.sdk.servlet.mvc.LoginController;
import com.stormpath.sdk.servlet.mvc.LoginErrorModelFactory;
import com.stormpath.sdk.servlet.mvc.LogoutController;
import com.stormpath.sdk.servlet.mvc.MeController;
import com.stormpath.sdk.servlet.mvc.RegisterController;
import com.stormpath.sdk.servlet.mvc.RequestFieldValueResolver;
import com.stormpath.sdk.servlet.mvc.SamlController;
import com.stormpath.sdk.servlet.mvc.SamlResultController;
import com.stormpath.sdk.servlet.mvc.VerifyController;
import com.stormpath.sdk.servlet.mvc.View;
import com.stormpath.sdk.servlet.mvc.ViewModel;
import com.stormpath.sdk.servlet.mvc.ViewResolver;
import com.stormpath.sdk.servlet.mvc.WebHandler;
import com.stormpath.sdk.servlet.mvc.provider.AccountStoreModelFactory;
import com.stormpath.sdk.servlet.mvc.provider.ExternalAccountStoreModelFactory;
import com.stormpath.sdk.servlet.mvc.provider.FacebookCallbackController;
import com.stormpath.sdk.servlet.mvc.provider.GithubCallbackController;
import com.stormpath.sdk.servlet.mvc.provider.GoogleCallbackController;
import com.stormpath.sdk.servlet.mvc.provider.LinkedinCallbackController;
import com.stormpath.sdk.servlet.oauth.AccessTokenValidationStrategy;
import com.stormpath.sdk.servlet.oauth.impl.JwtTokenSigningKeyResolver;
import com.stormpath.sdk.servlet.organization.DefaultOrganizationNameKeyResolver;
import com.stormpath.sdk.servlet.saml.DefaultSamlOrganizationResolver;
import com.stormpath.sdk.servlet.saml.SamlOrganizationContext;
import com.stormpath.sdk.servlet.util.DefaultGrantTypeValidator;
import com.stormpath.sdk.servlet.util.GrantTypeValidator;
import com.stormpath.sdk.servlet.util.IsLocalhostResolver;
import com.stormpath.sdk.servlet.util.RemoteAddrResolver;
import com.stormpath.sdk.servlet.util.SecureRequiredExceptForLocalhostResolver;
import com.stormpath.sdk.servlet.util.SubdomainResolver;
import com.stormpath.spring.mvc.AccessTokenControllerConfig;
import com.stormpath.spring.mvc.ChangePasswordControllerConfig;
import com.stormpath.spring.mvc.DisabledHandlerMapping;
import com.stormpath.spring.mvc.ForgotPasswordControllerConfig;
import com.stormpath.spring.mvc.LoginControllerConfig;
import com.stormpath.spring.mvc.LogoutControllerConfig;
import com.stormpath.spring.mvc.MessageContextRegistrar;
import com.stormpath.spring.mvc.RegisterControllerConfig;
import com.stormpath.spring.mvc.SingleNamedViewResolver;
import com.stormpath.spring.mvc.SpringMessageSource;
import com.stormpath.spring.mvc.SpringView;
import com.stormpath.spring.mvc.TemplateLayoutInterceptor;
import com.stormpath.spring.mvc.VerifyControllerConfig;
import com.stormpath.spring.util.SpringPatternMatcher;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @since 1.0.RC4
 */
@SuppressWarnings({"SpringFacetCodeInspection", "SpringJavaAutowiredMembersInspection"})
public abstract class AbstractStormpathWebMvcConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AbstractStormpathWebMvcConfiguration.class);

    private static final String PRODUCES_SUPPORTED_TYPES_MSG = "stormpath.web.produces property value must " +
        "specify either " + MediaType.APPLICATION_JSON_VALUE + " or " + MediaType.TEXT_HTML_VALUE + " or both.  " +
        "Other media types for this property are not currently supported.";

    // =================== Authentication Components ==========================

    @Value("#{ @environment['stormpath.web.authc.savers.cookie.enabled'] ?: true }")
    protected boolean cookieAuthenticationResultSaverEnabled;

    //session state storage should explicitly be enabled due to the performance impact it might have in
    //larger-scale environments (session state = shared state that must be distributed/clustered):
    @Value("#{ @environment['stormpath.web.authc.savers.session.enabled'] ?: false }")
    protected boolean sessionAuthenticationResultSaverEnabled;

    // ================  Account JWT properties  ===================

    @Value("#{ @environment['stormpath.web.account.jwt.ttl'] ?: 259200 }") //3 days by default
    protected long accountJwtTtl;

    @Value("#{ @environment['stormpath.web.account.jwt.signatureAlgorithm'] ?: 'HS256' }")
    protected SignatureAlgorithm accountJwtSignatureAlgorithm;

    // ================  HTTP Servlet Request behavior  ===================

    @Value("#{ @environment['stormpath.web.request.remoteUser.strategy'] ?: 'username' }")
    protected String requestRemoteUserStrategy;

    @Value("#{ @environment['stormpath.web.request.userPrincipal.strategy'] ?: 'account' }")
    protected String requestUserPrincipalStrategy;

    @Value("#{ @environment['stormpath.web.request.client.attributeNames'] ?: 'client' }")
    protected String requestClientAttributeNames;

    @Value("#{ @environment['stormpath.web.request.application.attributeNames'] ?: 'application' }")
    protected String requestApplicationAttributeNames;

    @Value("#{ @environment['stormpath.web.csrf.token.enabled'] ?: true }")
    protected boolean csrfTokenEnabled;

    @Value("#{ @environment['stormpath.web.csrf.token.ttl'] ?: 3600000 }") //1 hour (unit is millis)
    protected long csrfTokenTtl;

    @Value("#{ @environment['stormpath.web.csrf.token.name'] ?: 'csrfToken'}")
    protected String csrfTokenName;

    @Value("#{ @environment['stormpath.web.nonce.cache.name'] ?: 'com.stormpath.sdk.servlet.nonces' }")
    protected String nonceCacheName;

    @Value("#{ @environment['stormpath.web.http.authc.challenge'] ?: true }")
    protected boolean httpAuthenticationChallenge;

    // ================  StormpathFilter properties  ===================

    @Value("#{ @environment['stormpath.web.stormpathFilter.enabled'] ?: true }")
    protected boolean stormpathFilterEnabled;

    @Value("#{ @environment['stormpath.web.stormpathFilter.order'] ?: T(org.springframework.core.Ordered).HIGHEST_PRECEDENCE }")
    protected int stormpathFilterOrder;

    @Value("#{ @environment['stormpath.web.stormpathFilter.urlPatterns'] ?: '/*' }")
    protected String stormpathFilterUrlPatterns;

    @Value("#{ @environment['stormpath.web.stormpathFilter.servletNames'] }")
    protected String stormpathFilterServletNames;

    @Value("#{ @environment['stormpath.web.stormpathFilter.dispatcherTypes'] ?: 'REQUEST, INCLUDE, FORWARD, ERROR' }")
    protected String stormpathFilterDispatcherTypes;

    @Value("#{ @environment['stormpath.web.stormpathFilter.matchAfter'] ?: false }")
    protected boolean stormpathFilterMatchAfter;

    // ================  Static resources support ===================

    @Value("#{ @environment['stormpath.web.assets.handlerMapping.order'] ?: T(org.springframework.core.Ordered).HIGHEST_PRECEDENCE + 2}")
    protected int staticResourceHandlerMappingOrder = Integer.MIN_VALUE;

    @Value("#{ @environment['stormpath.web.assets.enabled'] ?: true}")
    protected boolean assetsEnabled;

    @Value("#{ @environment['stormpath.web.assets.defaultServletName'] ?: null}")
    protected String defaultServletName;

    @Value("#{ @environment['stormpath.web.assets.js.enabled'] ?: true}")
    protected boolean jsEnabled;

    @Value("#{ @environment['stormpath.web.assets.css.enabled'] ?: true}")
    protected boolean cssEnabled;

    // ================  'Head' view template properties  ===================

    @Value("#{ @environment['stormpath.web.head.view'] ?: 'stormpath/head' }")
    protected String headView;

    @Value("#{ @environment['stormpath.web.head.fragmentSelector'] ?: 'head' }")
    protected String headFragmentSelector;

    @Value("#{ @environment['stormpath.web.head.cssUris'] ?: '//fonts.googleapis.com/css?family=Open+Sans:300italic,300,400italic,400,600italic,600,700italic,700,800italic,800 //netdna.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css /assets/css/stormpath.css' }")
    protected String headCssUris;

    @Value("#{ @environment['stormpath.web.head.extraCssUris'] }")
    protected String headExtraCssUris;

    // ================  Register Controller properties  ===================

    @Value("#{ @environment['stormpath.web.register.autoLogin'] ?: false }")
    protected boolean registerAutoLogin;

    // ================  Logout Controller properties  ===================

    @Value("#{ @environment['stormpath.web.logout.invalidateHttpSession'] ?: true }")
    protected boolean logoutInvalidateHttpSession;

    // ================  ID Site properties  ===================

    @Value("#{ @environment['stormpath.web.idSite.enabled'] ?: false }")
    protected boolean idSiteEnabled;

    @Value("#{ @environment['stormpath.web.idSite.loginUri'] }")
    protected String idSiteLoginUri; //null by default as it is assumed the id site root is the same as the login page (usually)

    @Value("#{ @environment['stormpath.web.idSite.registerUri'] ?: '/#/register' }")
    protected String idSiteRegisterUri;

    @Value("#{ @environment['stormpath.web.idSite.forgotUri'] ?: '/#/forgot' }")
    protected String idSiteForgotUri;

    @Value("#{ @environment['stormpath.web.idSite.useSubdomain'] }")
    protected Boolean idSiteUseSubdomain;

    @Value("#{ @environment['stormpath.web.idSite.showOrganizationField'] }")
    protected Boolean idSiteShowOrganizationField;

    @Value("#{ @environment['stormpath.web.callback.enabled'] ?: true }")
    protected boolean callbackEnabled;

    @Value("#{ @environment['stormpath.web.callback.uri'] ?: '/stormpathCallback' }")
    protected String callbackUri;

    // ================  Me Controller properties ==================

    @Value("#{ @environment['stormpath.web.me.enabled'] ?: true }")
    protected boolean meEnabled;

    @Value("#{ @environment['stormpath.web.me.uri'] ?: '/me' }")
    protected String meUri;

    // ================  Content negotiation support properties  ===================

    @Value("#{ @environment['stormpath.web.produces'] ?: 'application/json, text/html' }")
    protected String produces;

    @Value("#{ @environment['stormpath.web.social.google.uri'] ?: '/callbacks/google' }")
    protected String googleCallbackUri;

    @Value("#{ @environment['stormpath.web.social.facebook.uri'] ?: '/callbacks/facebook' }")
    protected String facebookCallbackUri;

    @Value("#{ @environment['stormpath.web.social.linkedin.uri'] ?: '/callbacks/linkedin' }")
    protected String linkedinCallbackUri;

    @Value("#{ @environment['stormpath.web.social.github.uri'] ?: '/callbacks/github' }")
    protected String githubCallbackUri;

    @Value("#{ @environment['stormpath.web.application.domain'] }")
    protected String baseDomainName;

    //Spring's ThymeleafViewResolver defaults to an order of Ordered.LOWEST_PRECEDENCE - 5.  We want to ensure that this
    //JSON view resolver has a slightly higher precedence to ensure that JSON is rendered and not a Thymeleaf template.
    @Value("#{ @environment['stormpath.web.json.view.resolver.order'] ?: T(org.springframework.core.Ordered).LOWEST_PRECEDENCE - 10 }")
    protected int jsonViewResolverOrder;

    @Value("#{ @environment['stormpath.web.jsp.view.resolver.order'] ?: T(org.springframework.core.Ordered).LOWEST_PRECEDENCE}")
    protected int jspViewResolverOrder;

    // ================  CORS properties ==================

    @Value("#{ @environment['stormpath.web.cors.enabled'] ?: true }")
    protected boolean corsEnabled;

    @Value("#{ @environment['stormpath.web.cors.allowed.originUris'] }")
    protected String corsAllowedOrigins;

    @Value("#{ @environment['stormpath.web.cors.allowed.headers'] ?: 'Content-Type,Accept,X-Requested-With,remember-me' }")
    protected String corsAllowedHeaders;

    @Value("#{ @environment['stormpath.web.cors.allowed.methods'] ?: 'POST,GET,OPTIONS,DELETE,PUT' }")
    protected String corsAllowedMethods;

    @Autowired(required = false)
    protected PathMatcher pathMatcher;

    @Autowired(required = false)
    protected UrlPathHelper urlPathHelper;

    @Autowired
    protected Client client;

    @Autowired
    @Qualifier("stormpathApplication")
    protected Application application;

    @Autowired //will never be null because of our MessageSourceDefinitionPostProcessor
    protected MessageSource messageSource;

    @Autowired(required = false)
    protected LocaleResolver localeResolver;

    @Autowired(required = false)
    protected LocaleChangeInterceptor localeChangeInterceptor;

    @Autowired(required = false)
    @Qualifier("springSecurityIdSiteResultListener")
    protected IdSiteResultListener springSecurityIdSiteResultListener;

    @Autowired(required = false)
    @Qualifier("springSecuritySamlResultListener")
    protected SamlResultListener springSecuritySamlResultListener;

    @Autowired(required = false)
    protected ErrorModelFactory loginErrorModelFactory;

    @Autowired(required = false)
    protected ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected Environment environment;

    @Autowired
    protected ServletContext servletContext;

    @Autowired(required = false)
    @Qualifier("loginPreHandler")
    protected WebHandler loginPreHandler = new DisabledWebHandler();

    @Autowired(required = false)
    @Qualifier("loginPostHandler")
    protected WebHandler loginPostHandler = new DisabledWebHandler();

    @Autowired(required = false)
    @Qualifier("registerPreHandler")
    protected WebHandler registerPreHandler = new DisabledWebHandler();

    @Autowired(required = false)
    @Qualifier("registerPostHandler")
    protected WebHandler registerPostHandler = new DisabledWebHandler();

    @Autowired //all view resolvers in the spring app context. key: bean name, value: resolver
    private Map<String, org.springframework.web.servlet.ViewResolver> viewResolvers;

    private static class AccessibleResourceHandlerRegistry extends ResourceHandlerRegistry {
        public AccessibleResourceHandlerRegistry(ApplicationContext applicationContext, ServletContext servletContext) {
            super(applicationContext, servletContext);
        }

        public HandlerMapping toHandlerMapping() { //make the `getHandlerMapping()` method accessible:
            return getHandlerMapping();
        }
    }

    //https://github.com/stormpath/stormpath-sdk-java/issues/765
    public HandlerMapping stormpathStaticResourceHandlerMapping() {

        if (!assetsEnabled || (!cssEnabled && !jsEnabled)) {
            return new DisabledHandlerMapping();
        }

        AccessibleResourceHandlerRegistry registry =
            new AccessibleResourceHandlerRegistry(applicationContext, servletContext);
        registry.setOrder(staticResourceHandlerMappingOrder);

        if (cssEnabled) {
            registry.addResourceHandler("/assets/css/*stormpath.css")
                //reference the actual files in the stormpath-sdk-servlet .jar:
                .addResourceLocations("classpath:/META-INF/resources/assets/css/");
        }

        if (jsEnabled) {
            registry.addResourceHandler("/assets/js/*stormpath.js")
                //reference the actual files in the stormpath-sdk-servlet .jar:
                .addResourceLocations("classpath:/META-INF/resources/assets/js/");
        }

        return registry.toHandlerMapping();
    }

    private void addRoutes(final DefaultFilterChainManager mgr) throws ServletException {

        if (stormpathLoginConfig().isEnabled()) {
            addFilter(mgr, stormpathLoginController(), stormpathLoginConfig());
            addFilter(mgr, stormpathFacebookCallbackController(), "facebook", facebookCallbackUri);
            addFilter(mgr, stormpathGithubCallbackController(), "github", githubCallbackUri);
            addFilter(mgr, stormpathGoogleCallbackController(), "google", googleCallbackUri);
            addFilter(mgr, stormpathLinkedinCallbackController(), "linkedin", linkedinCallbackUri);
        }
        if (stormpathLogoutConfig().isEnabled()) {
            addFilter(mgr, stormpathLogoutController(), stormpathLogoutConfig());
        }
        boolean registerEnabled = stormpathRegisterConfig().isEnabled();
        if (stormpathRegisterEnabledPredicate().test(registerEnabled, application)) {
            addFilter(mgr, stormpathRegisterController(), stormpathRegisterConfig());
        }
        if (stormpathVerifyConfig().isEnabled()) {
            addFilter(mgr, stormpathVerifyController(), stormpathVerifyConfig());
        }
        if (stormpathForgotPasswordConfig().isEnabled()) {
            addFilter(mgr, stormpathForgotPasswordController(), stormpathForgotPasswordConfig());
        }
        if (stormpathChangePasswordConfig().isEnabled()) {
            addFilter(mgr, stormpathChangePasswordController(), stormpathChangePasswordConfig());
        }

        AccessTokenControllerConfig accessTokenControllerConfig = stormpathAccessTokenConfig();
        if (accessTokenControllerConfig.isEnabled()) {
            addFilter(mgr, stormpathAccessTokenController(), accessTokenControllerConfig.getControllerKey(), accessTokenControllerConfig.getAccessTokenUri());
        }

        if (idSiteEnabled) {
            addFilter(mgr, stormpathIdSiteResultController(), "idSiteResult", callbackUri);
        }
        if (callbackEnabled) {
            addFilter(mgr, stormpathSamlController(), "saml", "/saml"); //TODO: why isn't this a configurable uri?
            addFilter(mgr, stormpathSamlResultController(), "samlResult", callbackUri);
        }
        if (meEnabled) {
            //me filter is a little different than the others:
            addMeFilter(mgr);
        }
    }

    public View stormpathControllerView() {
        List<org.springframework.web.servlet.ViewResolver> l = new ArrayList<>(viewResolvers.values());
        org.springframework.web.servlet.ViewResolver vr = stormpathJsonViewResolver();
        if (!l.contains(vr)) {
            l.add(vr);
        }
        InternalResourceViewResolver irvr = stormpathJspViewResolver();
        if (!l.contains(irvr)) {
            l.add(irvr);
        }
        return new SpringView(l, stormpathSpringLocaleResolver(), stormpathLayoutInterceptor());
    }

    public View stormpathJacksonView() {
        JacksonView view = new JacksonView();
        view.setObjectMapper(objectMapper);
        return view;
    }

    public ViewResolver stormpathControllerViewResolver() {

        ViewResolver fixed = new ViewResolver() {
            @Override
            public View getView(ViewModel model, HttpServletRequest request) {
                return stormpathControllerView();
            }
        };

        return new DefaultViewResolver(fixed, stormpathJacksonView(), stormpathProducedMediaTypes());

    }

    public ApplicationResolver stormpathApplicationResolver() {
        return new DefaultApplicationResolver();
    }

    public Resolver<Boolean> stormpathRegisterEnabledResolver() {
        return new RegisterEnabledResolver(
            stormpathRegisterConfig().isEnabled(),
            stormpathApplicationResolver(),
            stormpathRegisterEnabledPredicate()
        );
    }

    public BiPredicate<Boolean, Application> stormpathRegisterEnabledPredicate() {
        return new RegisterEnabledPredicate();
    }

    public Controller stormpathGoogleCallbackController() {
        return configure(new GoogleCallbackController());
    }

    public Controller stormpathGithubCallbackController() {
        return configure(new GithubCallbackController());
    }

    public Controller stormpathFacebookCallbackController() {
        return configure(new FacebookCallbackController());
    }

    public Controller stormpathLinkedinCallbackController() {
        return configure(new LinkedinCallbackController());
    }

    public HandlerInterceptor stormpathLayoutInterceptor() {
        TemplateLayoutInterceptor interceptor = new TemplateLayoutInterceptor();
        interceptor.setHeadViewName(headView);
        interceptor.setHeadFragmentSelector(headFragmentSelector);

        //deal w/ URIs:
        String[] uris = StringUtils.tokenizeToStringArray(headCssUris, " \t");
        Set<String> uriSet = new LinkedHashSet<>();
        if (uris != null && uris.length > 0) {
            java.util.Collections.addAll(uriSet, uris);
        }

        uris = StringUtils.tokenizeToStringArray(headExtraCssUris, " \t");
        if (uris != null && uris.length > 0) {
            java.util.Collections.addAll(uriSet, uris);
        }

        if (!Collections.isEmpty(uriSet)) {
            List<String> list = new ArrayList<>();
            list.addAll(uriSet);
            interceptor.setHeadCssUris(list);
        }

        try {
            interceptor.afterPropertiesSet();
        } catch (Exception e) {
            String msg = "Unable to initialize stormpathLayoutInterceptor: " + e.getMessage();
            throw new BeanInitializationException(msg, e);
        }

        return interceptor;
    }

    /**
     * @since 1.0.0
     */
    public List<MediaType> stormpathProducedMediaTypes() {

        String mediaTypes = Strings.clean(produces);
        Assert.notNull(mediaTypes, "stormpath.web.produces property value cannot be null or empty.");

        try {
            return MediaType.parseMediaTypes(mediaTypes);
        } catch (InvalidMediaTypeException e) {
            String msg = "Unable to parse value in stormpath.web.produces property: " + e.getMessage();
            throw new IllegalArgumentException(msg, e);
        }
    }

    public org.springframework.web.servlet.View stormpathJsonView() {
        MappingJackson2JsonView jsonView = new MappingJackson2JsonView(objectMapper);
        // 786: Suppress Jackson's setting a Cache-Control, since they're set in individual controllers
        // Without this, we'll end up with duplicate Cache-Control headers
        jsonView.setDisableCaching(false);
        return jsonView;
    }

    //Requires a bean named 'stormpathJsonView' to be available:
    public org.springframework.web.servlet.ViewResolver stormpathJsonViewResolver() {
        return new SingleNamedViewResolver(View.STORMPATH_JSON_VIEW_NAME, stormpathJsonView(), jsonViewResolverOrder);
    }

    public InternalResourceViewResolver stormpathJspViewResolver() {
        InternalResourceViewResolver bean = new InternalResourceViewResolver();
        bean.setOrder(jspViewResolverOrder);
        bean.setViewClass(JstlView.class);
        bean.setPrefix("/WEB-INF/jsp/");
        bean.setSuffix(".jsp");
        return bean;
    }

    public AccountStoreResolver stormpathAccountStoreResolver() {
        return new DisabledAccountStoreResolver();
    }

    public UsernamePasswordRequestFactory stormpathUsernamePasswordRequestFactory() {
        return new DefaultUsernamePasswordRequestFactory(stormpathAccountStoreResolver());
    }

    public AccessTokenCookieProperties accessTokenCookieProperties() {
        return new AccessTokenCookieProperties();
    }

    public RefreshTokenCookieProperties refreshTokenCookieProperties() {
        return new RefreshTokenCookieProperties();
    }

    public CookieConfig stormpathRefreshTokenCookieConfig() {
        return new RefreshTokenCookieConfig(refreshTokenCookieProperties());
    }

    public CookieConfig stormpathAccessTokenCookieConfig() {
        return new AccessTokenCookieConfig(accessTokenCookieProperties());
    }

    public Resolver<String> stormpathRemoteAddrResolver() {
        return new RemoteAddrResolver();
    }

    public Resolver<Boolean> stormpathLocalhostResolver() {
        return new IsLocalhostResolver(stormpathRemoteAddrResolver());
    }

    public Resolver<Boolean> stormpathSecureResolver() {
        return new SecureRequiredExceptForLocalhostResolver(stormpathLocalhostResolver());
    }

    public Saver<AuthenticationResult> stormpathCookieAuthenticationResultSaver() {

        if (cookieAuthenticationResultSaverEnabled) {
            return new CookieAuthenticationResultSaver(
                stormpathAccessTokenCookieConfig(),
                stormpathRefreshTokenCookieConfig(),
                stormpathSecureResolver()
            );
        }

        //otherwise, return a dummy saver:
        return DisabledAuthenticationResultSaver.INSTANCE;
    }

    public List<Saver<AuthenticationResult>> stormpathAuthenticationResultSavers() {

        List<Saver<AuthenticationResult>> savers = new ArrayList<Saver<AuthenticationResult>>();

        Saver<AuthenticationResult> saver = stormpathCookieAuthenticationResultSaver();
        if (!(saver instanceof DisabledAuthenticationResultSaver)) {
            savers.add(saver);
        }

        return savers;
    }

    public Saver<AuthenticationResult> stormpathAuthenticationResultSaver() {

        List<Saver<AuthenticationResult>> savers = stormpathAuthenticationResultSavers();

        if (Collections.isEmpty(savers)) {
            String msg = "No Saver<AuthenticationResult> instances have been enabled or configured.  This is " +
                "required to save authentication result state.";
            throw new IllegalStateException(msg);
        }

        return new AuthenticationResultSaver(savers);
    }

    public JwtSigningKeyResolver stormpathJwtSigningKeyResolver() {
        return new JwtTokenSigningKeyResolver();
    }

    public RequestEventListener stormpathRequestEventListener() {
        return new RequestEventListenerAdapter();
    }

    public Publisher<RequestEvent> stormpathRequestEventPublisher() {
        List<RequestEventListener> listeners = new ArrayList<RequestEventListener>();
        listeners.add(new TokenRevocationRequestEventListener()); //revoke access and refresh tokens after logout
        listeners.add(stormpathRequestEventListener());
        return new RequestEventPublisher(listeners);
    }

    public String stormpathCsrfTokenSigningKey() {
        return client.getApiKey().getSecret();
    }

    public JwtAccountResolver stormpathJwtAccountResolver() {
        return new DefaultJwtAccountResolver(stormpathJwtSigningKeyResolver());
    }

    public Cache<String, String> stormpathNonceCache() {
        return client.getCacheManager().getCache(nonceCacheName);
    }

    public CsrfTokenManager stormpathCsrfTokenManager() {

        if (csrfTokenEnabled) {
            return new DefaultCsrfTokenManager(csrfTokenName, stormpathNonceCache(), stormpathCsrfTokenSigningKey(), csrfTokenTtl);
        }

        //otherwise disabled, return dummy implementation (NullObject design pattern):
        return new DisabledCsrfTokenManager(csrfTokenName);
    }

    public RequestFieldValueResolver stormpathFieldValueResolver() {
        ContentNegotiatingFieldValueResolver contentNegotiatingFieldValueResolver = new ContentNegotiatingFieldValueResolver();
        contentNegotiatingFieldValueResolver.setProduces(stormpathProducedMediaTypes());
        return contentNegotiatingFieldValueResolver;
    }

    public AccessTokenResultFactory stormpathAccessTokenResultFactory() {
        return new DefaultAccessTokenResultFactory(application);
    }

    /**
     * @since 1.0.RC8.3
     */
    public RefreshTokenResultFactory stormpathRefreshTokenResultFactory() {
        return new DefaultRefreshTokenResultFactory(application);
    }

    public WrappedServletRequestFactory stormpathWrappedServletRequestFactory() {
        return new DefaultWrappedServletRequestFactory(
            stormpathUsernamePasswordRequestFactory(), stormpathAuthenticationResultSaver(),
            stormpathRequestEventPublisher(), requestUserPrincipalStrategy, requestRemoteUserStrategy
        );
    }

    public HttpAuthenticationScheme stormpathBasicAuthenticationScheme() {
        return new BasicAuthenticationScheme(stormpathUsernamePasswordRequestFactory());
    }

    public HttpAuthenticationScheme stormpathBearerAuthenticationScheme() {
        return new BearerAuthenticationScheme(stormpathJwtSigningKeyResolver(), AccessTokenValidationStrategy.fromName(stormpathAccessTokenConfig().getAccessTokenValidationStrategy()));
    }

    public List<HttpAuthenticationScheme> stormpathHttpAuthenticationSchemes() {
        // The HTTP spec says that more well-supported authentication schemes should be listed first when the challenge
        // is sent.  The default Stormpath header authenticator implementation will send challenge entries in the
        // specified list order.  Since 'basic' is a more well-supported scheme than 'bearer' we order basic with
        // higher priority than bearer.
        return Arrays.asList(stormpathBasicAuthenticationScheme(), stormpathBearerAuthenticationScheme());
    }

    public HeaderAuthenticator stormpathAuthorizationHeaderAuthenticator() {
        return new AuthorizationHeaderAuthenticator(
            stormpathHttpAuthenticationSchemes(), httpAuthenticationChallenge, stormpathRequestEventPublisher()
        );
    }

    public Resolver<Account> stormpathAuthorizationHeaderAccountResolver() {
        return new AuthorizationHeaderAccountResolver(stormpathAuthorizationHeaderAuthenticator(), callbackUri);
    }

    public Resolver<Account> stormpathCookieAccountResolver() {
        return new CookieAccountResolver(
            stormpathAccessTokenCookieConfig(),
            stormpathRefreshTokenCookieConfig(),
            stormpathJwtAccountResolver(),
            stormpathCookieAuthenticationResultSaver(),
            stormpathAccessTokenResultFactory());
    }

    public List<Resolver<Account>> stormpathAccountResolvers() {

        //the order determines which locations are checked.  One an account is found, the remaining locations are
        //skipped, so we must order them based on preference:
        List<Resolver<Account>> resolvers = new ArrayList<Resolver<Account>>(3);
        resolvers.add(stormpathAuthorizationHeaderAccountResolver());
        resolvers.add(stormpathCookieAccountResolver());

        return resolvers;
    }

    public Resolver<List<String>> stormpathSubdomainResolver() {
        SubdomainResolver resolver = new SubdomainResolver();
        resolver.setBaseDomainName(baseDomainName);
        return resolver;
    }

    public Resolver<String> stormpathOrganizationNameKeyResolver() {
        DefaultOrganizationNameKeyResolver resolver = new DefaultOrganizationNameKeyResolver();
        resolver.setSubdomainResolver(stormpathSubdomainResolver());
        return resolver;
    }

    public Resolver<IdSiteOrganizationContext> stormpathIdSiteOrganizationResolver() {
        DefaultIdSiteOrganizationResolver resolver = new DefaultIdSiteOrganizationResolver();
        resolver.setOrganizationNameKeyResolver(stormpathOrganizationNameKeyResolver());
        resolver.setUseSubdomain(idSiteUseSubdomain);
        resolver.setShowOrganizationField(idSiteShowOrganizationField);
        return resolver;
    }

    /**
     * @since 1.0.RC8
     */
    public Resolver<SamlOrganizationContext> stormpathSamlOrganizationResolver() {
        DefaultSamlOrganizationResolver resolver = new DefaultSamlOrganizationResolver();
        resolver.setOrganizationNameKeyResolver(stormpathOrganizationNameKeyResolver());
        return resolver;
    }

    protected Controller createIdSiteController(String idSiteUri) {
        IdSiteController controller = new IdSiteController();
        controller.setServerUriResolver(stormpathServerUriResolver());
        controller.setIdSiteUri(idSiteUri);
        controller.setCallbackUri(callbackUri);
        controller.setAlreadyLoggedInUri(stormpathLoginConfig().getNextUri());
        controller.setIdSiteOrganizationResolver(stormpathIdSiteOrganizationResolver());
        controller.setNextUri(stormpathLoginConfig().getNextUri());
        controller.init();
        return controller;
    }

    /**
     * @since 1.0.RC8
     */
    protected Controller stormpathSamlController() {
        SamlController controller = new SamlController();
        controller.setServerUriResolver(stormpathServerUriResolver());
        controller.setCallbackUri(callbackUri);
        controller.setAlreadyLoggedInUri(stormpathLoginConfig().getNextUri());
        controller.setNextUri(stormpathLoginConfig().getNextUri());
        controller.setSamlOrganizationResolver(stormpathSamlOrganizationResolver());
        controller.init();
        return controller;
    }

    public ErrorModelFactory stormpathLoginErrorModelFactory() {
        return new LoginErrorModelFactory(stormpathMessageSource());
    }

    protected String createForwardView(String uri) {
        Assert.hasText("uri cannot be null or empty.");
        assert uri != null;
        if (!uri.startsWith("forward:")) {
            uri = "forward:" + uri;
        }
        return uri;
    }


    public AccountStoreModelFactory stormpathAccountStoreModelFactory() {
        return new ExternalAccountStoreModelFactory();
    }

    // ========================== Login =======================================

    public ControllerConfig stormpathLoginConfig() {
        return new LoginControllerConfig();
    }

    public Controller stormpathLoginController() {

        if (idSiteEnabled) {
            return createIdSiteController(idSiteLoginUri);
        }

        //otherwise standard login controller:
        LoginController c = configure(new LoginController(), stormpathLoginConfig());
        c.setForgotPasswordEnabled(stormpathForgotPasswordConfig().isEnabled());
        c.setForgotPasswordUri(stormpathForgotPasswordConfig().getUri());
        c.setVerifyEnabled(stormpathVerifyConfig().isEnabled());
        c.setVerifyUri(stormpathVerifyConfig().getUri());
        c.setRegisterEnabledResolver(stormpathRegisterEnabledResolver());
        c.setRegisterUri(stormpathRegisterConfig().getUri());
        c.setLogoutUri(stormpathLogoutConfig().getUri());
        c.setApplicationResolver(stormpathApplicationResolver());
        c.setAuthenticationResultSaver(stormpathAuthenticationResultSaver());
        c.setAccountStoreModelFactory(stormpathAccountStoreModelFactory());
        c.setPreLoginHandler(loginPreHandler);
        c.setPostLoginHandler(loginPostHandler);
        c.setIdSiteEnabled(idSiteEnabled);
        c.setCallbackEnabled(callbackEnabled);

        init(c);

        return c;
    }

    // ========================== Logout =======================================

    public ControllerConfig stormpathLogoutConfig() {
        return new LogoutControllerConfig();
    }

    public Controller stormpathLogoutController() {

        LogoutController c = idSiteEnabled ? new IdSiteLogoutController() : new LogoutController();
        c.setNextUri(stormpathLogoutConfig().getNextUri());
        c.setProduces(stormpathProducedMediaTypes());
        c.setInvalidateHttpSession(logoutInvalidateHttpSession);

        if (idSiteEnabled) {
            IdSiteLogoutController idslc = (IdSiteLogoutController) c;
            idslc.setServerUriResolver(stormpathServerUriResolver());
            idslc.setIdSiteResultUri(callbackUri);
            idslc.setIdSiteOrganizationResolver(stormpathIdSiteOrganizationResolver());
            c = idslc;
        }

        return init(c);
    }

    // ========================== Forgot Password =======================================

    public ControllerConfig stormpathForgotPasswordConfig() {
        return new ForgotPasswordControllerConfig();
    }

    public Controller stormpathForgotPasswordController() {

        if (idSiteEnabled) {
            return createIdSiteController(idSiteForgotUri);
        }

        ForgotPasswordController c = configure(new ForgotPasswordController(), stormpathForgotPasswordConfig());
        c.setLoginUri(stormpathLoginConfig().getUri());
        c.setAccountStoreResolver(stormpathAccountStoreResolver());

        return init(c);
    }

    public LocaleResolver stormpathSpringLocaleResolver() {
        if (localeResolver != null) {
            return localeResolver;
        }

        //otherwise create a default:
        return new CookieLocaleResolver();
    }

    public LocaleChangeInterceptor stormpathLocaleChangeInterceptor() {

        if (localeChangeInterceptor != null) {
            return localeChangeInterceptor;
        }

        //otherwise create a default:
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang");
        return lci;
    }

    public Set<String> stormpathRequestClientAttributeNames() {
        Set<String> set = new LinkedHashSet<>();
        set.addAll(Strings.commaDelimitedListToSet(requestClientAttributeNames));
        //we always want the client to be available as an attribute by it's own class name:
        set.add(Client.class.getName());
        return set;
    }

    public Set<String> stormpathRequestApplicationAttributeNames() {
        Set<String> set = new LinkedHashSet<>();
        set.addAll(Strings.commaDelimitedListToSet(requestApplicationAttributeNames));
        set.add(Application.class.getName());
        return set;
    }

    public Resolver<Locale> stormpathLocaleResolver() {

        final LocaleResolver localeResolver = stormpathSpringLocaleResolver();

        return new Resolver<Locale>() {
            @Override
            public Locale get(HttpServletRequest request, HttpServletResponse response) {
                return localeResolver.resolveLocale(request);
            }
        };
    }

    public MessageContextRegistrar stormpathMessageContextRegistrar() {
        MessageContext ctx = new DefaultMessageContext(stormpathMessageSource(), stormpathLocaleResolver());
        return new MessageContextRegistrar(ctx, servletContext);
    }

    public com.stormpath.sdk.servlet.i18n.MessageSource stormpathMessageSource() {
        return new SpringMessageSource(this.messageSource);
    }

    public ControllerConfig stormpathRegisterConfig() {
        return new RegisterControllerConfig();
    }

    public Controller stormpathRegisterController() {

        if (idSiteEnabled) {
            return createIdSiteController(idSiteRegisterUri);
        }

        //otherwise standard registration:
        RegisterController c = configure(new RegisterController(), stormpathRegisterConfig());
        c.setClient(client);
        c.setAuthenticationResultSaver(stormpathAuthenticationResultSaver());
        c.setLoginUri(stormpathLoginConfig().getUri());
        c.setVerifyViewName(stormpathVerifyConfig().getView());
        c.setAutoLogin(registerAutoLogin);
        c.setPreRegisterHandler(registerPreHandler);
        c.setPostRegisterHandler(registerPostHandler);
        c.setAccountStoreResolver(stormpathAccountStoreResolver());

        return init(c);
    }

    public ControllerConfig stormpathVerifyConfig() {
        return new VerifyControllerConfig();
    }

    public Controller stormpathVerifyController() {

        if (idSiteEnabled) {
            return createIdSiteController(null);
        }

        VerifyController c = configure(new VerifyController(), stormpathVerifyConfig());
        c.setLoginUri(stormpathLoginConfig().getUri());
        c.setLoginNextUri(stormpathLoginConfig().getNextUri());
        c.setClient(client);
        c.setAutoLogin(registerAutoLogin);
        c.setAuthenticationResultSaver(stormpathAuthenticationResultSaver());
        c.setAccountStoreResolver(stormpathAccountStoreResolver());

        return init(c);
    }

    public ChangePasswordControllerConfig stormpathChangePasswordConfig() {
        return new ChangePasswordControllerConfig();
    }

    public Controller stormpathChangePasswordController() {

        if (idSiteEnabled) {
            return createIdSiteController(null);
        }

        ChangePasswordController c = configure(new ChangePasswordController(), stormpathChangePasswordConfig());
        c.setForgotPasswordUri(stormpathForgotPasswordConfig().getUri());
        c.setLoginUri(stormpathLoginConfig().getUri());
        c.setLoginNextUri(stormpathLoginConfig().getNextUri());
        c.setErrorUri(stormpathChangePasswordConfig().getErrorUri());
        c.setAutoLogin(stormpathChangePasswordConfig().isAutoLogin());
        c.setAuthenticationResultSaver(stormpathAuthenticationResultSaver());

        return init(c);
    }

    // ========================== Access Token =======================================


    /**
     * @since 1.2.0
     */
    public AccessTokenControllerConfig stormpathAccessTokenConfig() {
        return new AccessTokenControllerConfig();
    }

    public Controller stormpathAccessTokenController() {

        AccessTokenController c = new AccessTokenController();
        c.setEventPublisher(stormpathRequestEventPublisher());
        c.setAccessTokenAuthenticationRequestFactory(stormpathAccessTokenAuthenticationRequestFactory());
        c.setAccessTokenResultFactory(stormpathAccessTokenResultFactory());
        c.setRefreshTokenAuthenticationRequestFactory(stormpathRefreshTokenAuthenticationRequestFactory());
        c.setRefreshTokenResultFactory(stormpathRefreshTokenResultFactory());
        c.setAccountSaver(stormpathAuthenticationResultSaver());
        c.setRequestAuthorizer(stormpathAccessTokenRequestAuthorizer());
        c.setBasicAuthenticationScheme(stormpathBasicAuthenticationScheme());
        c.setGrantTypeValidator(stormpathGrantTypeStatusValidator());

        return init(c);
    }

    /**
     * @since 1.2.0
     */
    public GrantTypeValidator stormpathGrantTypeStatusValidator() {
        AccessTokenControllerConfig config = stormpathAccessTokenConfig();

        DefaultGrantTypeValidator grantTypeStatusValidator = new DefaultGrantTypeValidator();
        grantTypeStatusValidator.setClientCredentialsGrantTypeEnabled(config.isClientCredentialsGrantTypeEnabled());
        grantTypeStatusValidator.setPasswordGrantTypeEnabled(config.isPasswordGrantTypeEnabled());

        return grantTypeStatusValidator;
    }

    public Controller stormpathIdSiteResultController() {
        IdSiteResultController controller = new IdSiteResultController();
        controller.setLoginNextUri(stormpathLoginConfig().getNextUri());
        controller.setRegisterNextUri(stormpathRegisterConfig().getNextUri());
        controller.setLogoutController(stormpathLogoutController());
        controller.setAuthenticationResultSaver(stormpathAuthenticationResultSaver());
        controller.setEventPublisher(stormpathRequestEventPublisher());
        if (springSecurityIdSiteResultListener != null) {
            controller.addIdSiteResultListener(springSecurityIdSiteResultListener);
        }
        controller.init();
        return controller;
    }

    public Controller stormpathMeController() {
        MeController controller = new MeController();

        controller.setExpandsResolver(stormpathMeExpandsResolver());
        controller.setObjectMapper(objectMapper);
        controller.setProduces(stormpathProducedMediaTypes());
        controller.setUri(meUri);
        controller.setLoginPageRedirector(new DefaultLoginPageRedirector(stormpathLoginConfig().getUri()));
        controller.setApplicationResolver(stormpathApplicationResolver());

        init(controller);

        return controller;
    }

    /**
     * @since 1.2.0
     */
    public ExpandsResolver stormpathMeExpandsResolver(){
        List<String> expandedAccountAttributes = new ArrayList<>();

        getPropertiesStartingWith((ConfigurableEnvironment) environment, "stormpath.web.me.expand");

        Pattern pattern = Pattern.compile("^stormpath\\.web\\.me\\.expand\\.(\\w+)$");

        for (String key : getPropertiesStartingWith((ConfigurableEnvironment) environment, "stormpath.web.me.expand").keySet()) {
            Matcher matcher = pattern.matcher(key);
            if (matcher.find()) {
                if (environment.getProperty(key, Boolean.class, false)) {
                    expandedAccountAttributes.add(matcher.group(1));
                }
            }
        }

        return new DefaultExpandsResolver(expandedAccountAttributes);
    }

    public Controller stormpathSamlResultController() {
        SamlResultController controller = new SamlResultController();
        controller.setLoginNextUri(stormpathLoginConfig().getNextUri());
        controller.setLogoutController(stormpathLogoutController());
        controller.setAuthenticationResultSaver(stormpathAuthenticationResultSaver());
        controller.setEventPublisher(stormpathRequestEventPublisher());
        if (springSecuritySamlResultListener != null) {
            controller.addSamlResultListener(springSecuritySamlResultListener);
        }
        controller.init();
        return controller;
    }

    public AccessTokenAuthenticationRequestFactory stormpathAccessTokenAuthenticationRequestFactory() {
        return new DefaultAccessTokenAuthenticationRequestFactory(stormpathAccountStoreResolver());
    }

    /**
     * @since 1.0.RC8.3
     */
    public RefreshTokenAuthenticationRequestFactory stormpathRefreshTokenAuthenticationRequestFactory() {
        return new DefaultRefreshTokenAuthenticationRequestFactory();
    }

    public RequestAuthorizer stormpathAccessTokenRequestAuthorizer() {
        return new DefaultAccessTokenRequestAuthorizer(
            stormpathSecureResolver(), stormpathOriginAccessTokenRequestAuthorizer()
        );
    }

    public Set<String> stormpathAccessTokenAuthorizedOriginUris() {
        return Strings.delimitedListToSet(stormpathAccessTokenConfig().getAccessTokenAuthorizedOriginUris(), " \t");
    }

    public RequestAuthorizer stormpathOriginAccessTokenRequestAuthorizer() {
        return new OriginAccessTokenRequestAuthorizer(
            stormpathServerUriResolver(), stormpathLocalhostResolver(), stormpathAccessTokenAuthorizedOriginUris(),
            stormpathProducedMediaTypes()
        );
    }

    public ServerUriResolver stormpathServerUriResolver() {
        return new DefaultServerUriResolver();
    }

    public AccountResolver stormpathAccountResolver() {
        return new DefaultAccountResolver();
    }

    public ContentNegotiationResolver stormpathContentNegotiationResolver() {
        return new DefaultContentNegotiationResolver();
    }

    private void configure(AbstractController c) {
        c.setAccountResolver(stormpathAccountResolver());
        c.setContentNegotiationResolver(stormpathContentNegotiationResolver());
        c.setEventPublisher(stormpathRequestEventPublisher());
        c.setLocaleResolver(stormpathLocaleResolver());
        c.setMessageSource(stormpathMessageSource());
        c.setProduces(stormpathProducedMediaTypes());
    }

    private <T extends FormController> T configure(T controller, ControllerConfig cr) {
        configure(controller);
        controller.setUri(cr.getUri());
        controller.setNextUri(cr.getNextUri());
        controller.setView(cr.getView());
        controller.setControllerKey(cr.getControllerKey());
        controller.setCsrfTokenManager(stormpathCsrfTokenManager());
        controller.setFieldValueResolver(stormpathFieldValueResolver());
        List<Field> fields = cr.getFormFields();
        if (!Collections.isEmpty(fields)) { //might be empty if the fields are static / configured within the controller
            controller.setFormFields(fields);
        }
        return controller;
    }

    private <T extends AbstractSocialCallbackController> T configure(T c) {
        configure((AbstractController) c);
        c.setNextUri(stormpathLoginConfig().getUri());
        c.setAuthenticationResultSaver(stormpathAuthenticationResultSaver());
        c.setApplicationResolver(stormpathApplicationResolver());
        return c;
    }

    private <T extends AbstractController> T init(T c) {
        try {
            c.init();
            return c;
        } catch (Exception e) {
            String msg = "Unable to initialize controller [" + c + "]: " + e.getMessage();
            throw new BeanInitializationException(msg, e);
        }
    }

    private Filter wrapForSpringLocaleSupport(final Filter filter) {
        return new Filter() {
            @Override
            public void init(FilterConfig filterConfig) throws ServletException {
            }

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse resp = (HttpServletResponse) response;

                //spring requires these shenanigans:
                ServletRequestAttributes attributes = new ServletRequestAttributes(req, resp);
                try {
                    RequestContextHolder.setRequestAttributes(attributes);
                    stormpathLocaleChangeInterceptor().preHandle(req, resp, null);
                    filter.doFilter(req, resp, chain);
                } finally {
                    RequestContextHolder.resetRequestAttributes();
                }
            }

            @Override
            public void destroy() {
            }
        };
    }

    private Filter createFilter(Controller controller, String name) {
        try {
            ControllerFilter filter = new ControllerFilter();
            filter.setViewResolver(stormpathControllerViewResolver());
            filter.setProducedMediaTypes(stormpathProducedMediaTypes());
            filter.setController(controller);
            Filter f = Filters.builder().setFilter(filter).setServletContext(servletContext).setName(name).build();
            return wrapForSpringLocaleSupport(f);
        } catch (ServletException e) {
            String msg = "Unable to create filter '" + name + "' to wrap controller [" + controller + "]: " + e.getMessage();
            throw new BeanCreationException(msg, e);
        }
    }

    private void addFilter(DefaultFilterChainManager mgr, Controller controller, ControllerConfig config) throws ServletException {
        addFilter(mgr, controller, config.getControllerKey(), config.getUri());
    }

    private void addFilter(DefaultFilterChainManager mgr, Controller controller, String name, String uri) throws ServletException {
        Filter filter = createFilter(controller, name);
        mgr.addFilter(name, filter);
        mgr.createChain(uri, name);
    }

    private void addMeFilter(DefaultFilterChainManager mgr) throws ServletException {
        //me filter is a little different than the others:
        String name = "me";
        MeFilter meFilter = new MeFilter();
        meFilter.setProducedMediaTypes(stormpathProducedMediaTypes());
        meFilter.setController(stormpathMeController());
        Filter f = Filters.builder().setFilter(meFilter).setServletContext(servletContext).setName(name).build();
        mgr.addFilter(name, f);
        mgr.createChain(meUri, name);
    }

    public FilterChainResolver stormpathFilterChainResolver() {

        PathMatchingFilterChainResolver resolver = new PathMatchingFilterChainResolver(servletContext);
        resolver.setFilterChainManager(stormpathFilterChainManager());
        if (pathMatcher != null) {
            resolver.setPathMatcher(new SpringPatternMatcher(pathMatcher));
        }

        // The account resolver filter always executes immediately after the StormpathFilter but
        // before any other configured filters in the chain:
        //
        // Note that we don't do this as a bean defined outside of this method because we don't want Spring
        // to discover it and add it to the general filter chain.  It is used only by the PrioritizedFilterChainResolver
        AccountResolverFilter accountResolverFilter = new AccountResolverFilter();
        accountResolverFilter.setEnabled(stormpathFilterEnabled);
        accountResolverFilter.setResolvers(stormpathAccountResolvers());
        accountResolverFilter.setOauthEndpointUri(stormpathAccessTokenConfig().getAccessTokenUri());
        List<Filter> priorityFilters = Collections.<Filter>toList(accountResolverFilter);

        if (corsEnabled) {
            priorityFilters.add(newCorsFilter());
        }

        return new PrioritizedFilterChainResolver(resolver, priorityFilters);
    }

    public FilterChainManager stormpathFilterChainManager() {
        DefaultFilterChainManager mgr = new DefaultFilterChainManager(servletContext);
        try {
            addRoutes(mgr);
            return mgr;
        } catch (ServletException e) {
            String msg = "Could not create Filter Chain Manager: " + e.getMessage();
            throw new BeanCreationException(msg, e);
        }
    }

    protected StormpathFilter newStormpathFilter() {
        StormpathFilter filter = new StormpathFilter();
        filter.setClient(client);
        filter.setApplication(application);
        filter.setEnabled(stormpathFilterEnabled);
        filter.setClientRequestAttributeNames(stormpathRequestClientAttributeNames());
        filter.setApplicationRequestAttributeNames(stormpathRequestApplicationAttributeNames());
        filter.setFilterChainResolver(stormpathFilterChainResolver());
        filter.setWrappedServletRequestFactory(stormpathWrappedServletRequestFactory());
        return filter;
    }

    protected static class DisabledAuthenticationResultSaver implements Saver<AuthenticationResult> {

        protected static final DisabledAuthenticationResultSaver INSTANCE = new DisabledAuthenticationResultSaver();

        @Override
        public void set(HttpServletRequest request, HttpServletResponse response, AuthenticationResult value) {
            //no-op
        }
    }

    //The code below is taken out of http://stackoverflow.com/questions/23506471/spring-access-all-environment-properties-as-a-map-or-properties-object
    public static Map<String, Object> getPropertiesStartingWith(ConfigurableEnvironment aEnv, String aKeyPrefix) {
        Map<String, Object> result = new HashMap<String, Object>();

        Map<String, Object> map = getAllProperties(aEnv);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();

            if (key.startsWith(aKeyPrefix)) {
                result.put(key, entry.getValue());
            }
        }

        return result;
    }

    protected static Map<String, Object> getAllProperties(ConfigurableEnvironment aEnv) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (PropertySource propertySource : aEnv.getPropertySources()) {
            addAll(result, getAllProperties(propertySource));
        }
        return result;
    }

    protected static Map<String, Object> getAllProperties(PropertySource<?> aPropSource) {
        Map<String, Object> result = new HashMap<String, Object>();

        if (aPropSource instanceof CompositePropertySource) {
            CompositePropertySource cps = (CompositePropertySource) aPropSource;
            for (PropertySource<?> propertySource : cps.getPropertySources()) {
                addAll(result, getAllProperties(propertySource));
            }
            return result;
        }

        if (aPropSource instanceof EnumerablePropertySource<?>) {
            EnumerablePropertySource<?> ps = (EnumerablePropertySource<?>) aPropSource;
            for (String propertyName : ps.getPropertyNames()) {
                result.put(propertyName, ps.getProperty(propertyName));
            }

            return result;
        }

        return result;
    }

    private static void addAll(Map<String, Object> aBase, Map<String, Object> aToBeAdded) {
        for (Map.Entry<String, Object> entry : aToBeAdded.entrySet()) {
            if (aBase.containsKey(entry.getKey())) {
                continue;
            }

            aBase.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Fix for https://github.com/stormpath/stormpath-sdk-java/issues/699
     *
     * @since 1.2.0
     */
    public Filter newCorsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(stormpathCorsAllowedOrigins());
        config.setAllowedHeaders(stormpathCorsAllowedHeaders());
        config.setAllowedMethods(stormpathCorsAllowedMethods());
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * Fix for https://github.com/stormpath/stormpath-sdk-java/issues/699
     *
     * @since 1.2.0
     */
    public List<String> stormpathCorsAllowedOrigins() {
        if (Strings.hasText(corsAllowedOrigins)) {
            return Arrays.asList(Strings.split(corsAllowedOrigins));
        }

        return java.util.Collections.emptyList();
    }

    /**
     * Fix for https://github.com/stormpath/stormpath-sdk-java/issues/699
     *
     * @since 1.2.0
     */
    public List<String> stormpathCorsAllowedMethods() {
        if (Strings.hasText(corsAllowedOrigins)) {
            return Arrays.asList(Strings.split(corsAllowedMethods));
        }

        return java.util.Collections.emptyList();
    }

    /**
     * Fix for https://github.com/stormpath/stormpath-sdk-java/issues/699
     *
     * @since 1.2.0
     */
    public List<String> stormpathCorsAllowedHeaders() {
        if (Strings.hasText(corsAllowedOrigins)) {
            return Arrays.asList(Strings.split(corsAllowedHeaders));
        }

        return java.util.Collections.emptyList();
    }
}

