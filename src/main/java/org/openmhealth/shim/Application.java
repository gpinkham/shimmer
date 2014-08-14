package org.openmhealth.shim;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "org.openmhealth")
@EnableWebSecurity
@RestController
public class Application extends WebSecurityConfigurerAdapter {

    @Autowired
    private AccessParametersRepo accessParametersRepo;

    @Autowired
    private AuthorizationRequestParametersRepo authParametersRepo;

    @Autowired
    private ShimRegistry shimRegistry;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        /**
         * Allow full anonymous authentication.
         */
        http.authorizeRequests().anyRequest().permitAll();
    }

    /**
     * Endpoint for triggering domain approval.
     *
     * @param username          - The user record for which we're authorizing a shim.
     * @param clientRedirectUrl -  The URL to which the external shim client (i.e, hibpone) will
     *                          be redirected after authorization is complete.
     * @param shim              - The shim registry key of the shim we're approving
     * @return - AuthorizationRequest parameters, including a boolean
     * flag if already authorized.
     */
    @RequestMapping("/authorize/{shim}")
    public
    @ResponseBody
    AuthorizationRequestParameters authorize(@RequestParam(value = "username") String username,
                                             @RequestParam(value = "client_redirect_url", defaultValue = "oob")
                                             String clientRedirectUrl,
                                             @PathVariable("shim") String shim) throws ShimException {
        setPassThroughAuthentication(username, shim);
        AuthorizationRequestParameters authParams =
            shimRegistry.getShim(shim).getAuthorizationRequestParameters(
                username, Collections.<String, String>emptyMap());
        /**
         * Save authorization parameters to local repo. They will be
         * re-fetched via stateKey upon approval.
         */
        authParams.setUsername(username);
        authParams.setClientRedirectUrl(clientRedirectUrl);
        authParametersRepo.save(authParams);
        return authParams;
    }

    /**
     * Endpoint for removing authorizations for a given user and shim.
     *
     * @param username - The user record for which we're removing shim access.
     * @param shim     - The shim registry key of the shim authorization we're removing.
     * @return - Simple response message.
     */
    @RequestMapping(value = "/de-authorize/{shim}", method = RequestMethod.DELETE)
    public
    @ResponseBody
    List<String> removeAuthorization(@RequestParam(value = "username") String username,
                                     @PathVariable("shim") String shim) throws ShimException {
        List<AccessParameters> accessParameters =
            accessParametersRepo.findAllByUsernameAndShimKey(username, shim);
        for (AccessParameters accessParameter : accessParameters) {
            accessParametersRepo.delete(accessParameter);
        }
        return Arrays.asList("Success: Authorization Removed.");
    }

    /**
     * Endpoint for handling approvals from external data providers
     *
     * @param servletRequest - Request posted by the external data provider.
     * @return - AuthorizationResponse object with details
     * and result: authorize, error, or denied.
     */
    @RequestMapping(value = "/authorize/{shim}/callback",
        method = {RequestMethod.POST, RequestMethod.GET})
    public
    @ResponseBody
    AuthorizationResponse approve(@PathVariable("shim") String shim,
                                  HttpServletRequest servletRequest,
                                  HttpServletResponse servletResponse) throws ShimException {
        String stateKey = servletRequest.getParameter("state");
        AuthorizationRequestParameters authParams = authParametersRepo.findByStateKey(stateKey);
        if (authParams == null) {
            throw new ShimException("Invalid state key, original access " +
                "request not found. Cannot authorize.");
        } else {
            setPassThroughAuthentication(authParams.getUsername(), shim);
            AuthorizationResponse response =
                shimRegistry.getShim(shim).handleAuthorizationResponse(servletRequest);
            /**
             * Save the access parameters to local repo.
             * They will be re-fetched via username and path parameters
             * for future requests.
             */
            response.getAccessParameters().setUsername(authParams.getUsername());
            response.getAccessParameters().setShimKey(shim);
            accessParametersRepo.save(response.getAccessParameters());

            /**
             * At this point the authorization is complete, if the authorization request
             * required a client redirect we do it now, else just return
             * the authorization response.
             */
            if (authParams.getRedirectUri() != null) {
                try {
                    servletResponse.sendRedirect(authParams.getRedirectUri());
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new ShimException("Error occurred redirecting to :"
                        + authParams.getRedirectUri());
                }
                return null;
            }
            return response;
        }
    }

    /**
     * Endpoint for retrieving data from shims.
     *
     * @return - The shim data response wrapper with data from the shim.
     */
    @RequestMapping(value = "/data/{shim}/{dataType}")
    public
    @ResponseBody
    ShimDataResponse data(@RequestParam(value = "username") String username,
                          @PathVariable("shim") String shim,
                          @PathVariable("dataType") String dataTypeKey,
                          @RequestParam(value = "normalize",
                              required = false, defaultValue = "") String normalize,
                          HttpServletRequest servletRequest) throws ShimException {
        setPassThroughAuthentication(username, shim);

        ShimDataRequest shimDataRequest =
            ShimDataRequest.fromHttpRequest(servletRequest);
        shimDataRequest.setDataTypeKey(dataTypeKey);
        shimDataRequest.setNormalize(!"".equals(normalize));

        AccessParameters accessParameters =
            accessParametersRepo.findByUsernameAndShimKey(
                username, shim, new Sort(Sort.Direction.DESC, "dateCreated"));

        if (accessParameters == null) {
            throw new ShimException("User '"
                + username + "' has not authorized shim: '" + shim + "'");
        }
        shimDataRequest.setAccessParameters(accessParameters);
        return shimRegistry.getShim(shim).getData(shimDataRequest);
    }

    /**
     * Sets pass through authentication required by spring.
     */
    private void setPassThroughAuthentication(String username, String shim) {
        SecurityContextHolder.getContext()
            .setAuthentication(new ShimAuthentication(username, shim));
    }
}
