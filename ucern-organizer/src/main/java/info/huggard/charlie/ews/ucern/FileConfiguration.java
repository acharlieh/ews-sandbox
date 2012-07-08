package info.huggard.charlie.ews.ucern;

import info.huggard.charlie.ews.ucern.UCernOrganizer.Configuration;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.List;
import java.util.Properties;

import microsoft.exchange.webservices.data.DeleteMode;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.ExchangeVersion;
import microsoft.exchange.webservices.data.Mailbox;
import microsoft.exchange.webservices.data.WebCredentials;
import microsoft.exchange.webservices.data.WellKnownFolderName;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

/**
 * Reads the configuration from a file.
 * @author Charlie Huggard
 */
@SuppressWarnings("all")
public class FileConfiguration implements Configuration {
    private final Properties props;
    private final ObjectMapper mapper;

    FileConfiguration(final String fileName, final ObjectMapper mapper) throws Exception {
        this.mapper = mapper;

        final Reader r = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
        try {
            props = new Properties();
            props.load(r);
        } finally {
            r.close();
        }
    }

    private ExchangeVersion version() {
        return ExchangeVersion.valueOf(props.getProperty("ucern.ews.version", "Exchange2010_SP1"));
    }

    private String userName() {
        return props.getProperty("ucern.ews.user");
    }

    private String password() {
        return props.getProperty("ucern.ews.password");
    }

    private String domain() {
        return props.getProperty("ucern.ews.domain");
    }

    private String mailbox() {
        return props.getProperty("ucern.ews.mailbox", userName() + "@Cerner.com");
    }

    private String prot() {
        return props.getProperty("ucern.ews.protocol", "https");
    }

    private String host() {
        return props.getProperty("ucern.ews.host");
    }

    private int port() {
        return Integer.parseInt(props.getProperty("ucern.ews.port", "443"));
    }

    private String path() {
        return props.getProperty("ucern.ews.path", "EWS/Exchange.asmx");
    }

    private URI uri() {
        final String uri = props.getProperty("ucern.ews.uri");
        if (uri != null) {
            return URI.create(uri);
        }
        final String host = host();
        if (host != null) {
            return URI.create(prot() + "://" + host + ":" + port() + "/" + path());
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExchangeService getService() throws Exception {
        final ExchangeService service = new ExchangeService(version());
        service.setCredentials(new WebCredentials(userName(), password(), domain()));

        final URI uri = uri();
        if (uri == null) {
            service.autodiscoverUrl(mailbox());
        } else {
            service.setUrl(uri);
        }

        return service;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mailbox getMailbox() {
        return new Mailbox(mailbox());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getFolders() throws Exception {
        final String folders = props.getProperty("ucern.ews.folderJson", "[\"uCern\"]");
        return mapper.readValue(folders, new TypeReference<List<String>>() {
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WellKnownFolderName startAt() {
        return WellKnownFolderName.valueOf(props.getProperty("ucern.ews.start", "MsgFolderRoot"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFrom() {
        return props.getProperty("ucern.ews.from", "admin@ucern.com");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPurgeOlderThanDays() {
        return Integer.parseInt(props.getProperty("ucern.ews.purgeDays", "7"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeleteMode getItemDeleteMode() {
        return DeleteMode.valueOf(props.getProperty("ucern.ews.deleteMode", "MoveToDeletedItems"));
    }
}
