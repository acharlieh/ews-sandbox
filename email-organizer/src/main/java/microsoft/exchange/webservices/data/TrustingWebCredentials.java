/**************************************************************************
 * copyright file="WebCredentials.java" company="Microsoft" Copyright (c) Microsoft Corporation. All rights reserved.
 * Defines the WebCredentials.java.
 **************************************************************************/
package microsoft.exchange.webservices.data;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/***
 * WebCredentials is used for password-based authentication schemes such as basic, digest, NTLM, and Kerberos
 * authentication.
 */
public class TrustingWebCredentials extends ExchangeCredentials {

    /** The domain. */
    private final String domain;

    /** The user. */
    private final String user;

    /** The pwd. */
    private final String pwd;

    /** The use default credentials. */
    private boolean useDefaultCredentials = true;

    /**
     * Gets the domain.
     * @return the domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Gets the user.
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * Gets the pwd.
     * @return the pwd
     */
    public String getPwd() {
        return pwd;
    }

    /**
     * Checks if is use default credentials.
     * @return true, if is use default credentials
     */
    public boolean isUseDefaultCredentials() {
        return useDefaultCredentials;
    }

    /***
     * Initializes a new instance to use default network credentials.
     */
    public TrustingWebCredentials() {
        useDefaultCredentials = true;
        this.user = null;
        this.pwd = null;
        this.domain = null;
    }

    /***
     * Initializes a new instance to use specified credentials.
     * @param userName Account user name.
     * @param password Account password.
     * @param domain Account domain.
     */
    public TrustingWebCredentials(final String userName, final String password, final String domain) {
        if (userName == null || password == null) {
            throw new IllegalArgumentException("User name or password can not be null"); //$NON-NLS-1$
        }

        this.domain = domain;
        this.user = userName;
        this.pwd = password;
        useDefaultCredentials = false;
    }

    /***
     * Initializes a new instance to use specified credentials.
     * @param username The user name.
     * @param password The password.
     */
    public TrustingWebCredentials(final String username, final String password) {
        this(username, password, ""); //$NON-NLS-1$
    }

    /***
     * This method is called to apply credentials to a service request before the request is made.
     * @param client The request.
     */
    @Override
    protected void prepareWebRequest(final HttpWebRequest client) {
        if (useDefaultCredentials) {
            client.setUseDefaultCredentials(true);
        } else {
            client.setCredentails(domain, user, pwd);
        }
        final X509TrustManager trustManager = new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain, final String authType)
                    throws CertificateException {

            }

            @Override
            public void checkClientTrusted(final X509Certificate[] chain, final String authType)
                    throws CertificateException {

            }
        };
        try {
            client.setClientCertificates(trustManager);
        } catch (final EWSHttpException e) {
            e.printStackTrace();
        }
    }
}