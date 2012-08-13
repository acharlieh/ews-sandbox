package info.huggard.charlie.ews.config;

import info.huggard.charlie.ews.Configuration.Values;

import java.util.HashMap;
import java.util.Map;

import microsoft.exchange.webservices.data.ExchangeVersion;

/**
 * Defaults for Connection settings.
 * @author Charlie Huggard
 */
@SuppressWarnings("nls")
public class ConnectionDefaults extends AdditionalDefaults {
    private static final Map<String, String> defaultValues = new HashMap<String, String>() {
        {
            put("user", null);
            put("password", null);
            put("domain", null);
            put("version", ExchangeVersion.Exchange2010_SP1.name());
            put("protocol", "https");
            put("host", null);
            put("port", "443");
            put("path", "EWS/Exchange.asmx");
        }
        private static final long serialVersionUID = 4701060038922293032L;
    };

    private final Values connectionConfig;

    public ConnectionDefaults(final Values defaults, final Values connectionConfig) {
        super(defaults, defaultValues, null);
        this.connectionConfig = connectionConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getValue(final String key) {
        if (key.equals("uri")) {
            final String host = connectionConfig.getValue("host");
            if (host != null) {
                final String protocol = connectionConfig.getValue("protocol");
                final Integer port = Integer.parseInt(connectionConfig.getValue("port"));
                final String path = connectionConfig.getValue("path");
                return String.format("%s://%s:%d/%s", protocol, host, port, path);
            }
            return null;
        }
        return super.getValue(key);
    }
}
