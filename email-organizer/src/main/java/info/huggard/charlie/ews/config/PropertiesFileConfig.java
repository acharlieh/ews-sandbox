package info.huggard.charlie.ews.config;

import info.huggard.charlie.ews.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.type.TypeReference;

/**
 * Class for reading configuration out of a properties file.
 * @author Charlie Huggard
 */
@SuppressWarnings({ "nls", "synthetic-access" })
public class PropertiesFileConfig implements Configuration {

    private static ObjectMapper getObjectMapper() {
        final ObjectMapper toReturn = new ObjectMapper();
        toReturn.disable(SerializationConfig.Feature.USE_ANNOTATIONS);
        toReturn.disable(DeserializationConfig.Feature.USE_ANNOTATIONS);
        return toReturn;
    }

    private static Properties readPropertiesFromFile(final String fileName) throws IOException {
        final Properties props = new Properties();
        final Reader r = new InputStreamReader(new FileInputStream(fileName), "UTF-8"); //$NON-NLS-1$
        try {
            props.load(r);
            return props;
        } finally {
            r.close();
        }
    }

    /**
     * @param fileName Properties file to be read.
     * @throws IOException If the file could not be read.
     */
    public PropertiesFileConfig(final String fileName) throws IOException {
        this(readPropertiesFromFile(fileName), getObjectMapper());
    }

    /**
     * DI Constructor
     * @param properties The properties object to be read
     * @param mapper Jackson mapper to be used to parse lists
     */
    PropertiesFileConfig(final Properties properties, final ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;

        this.connectionSection = new PropertiesSection("connection.");
        this.defaultSection = new PropertiesSection("default.", new ImplicitDefaults(connectionSection));
        this.connectionSection.setDefaults(new ConnectionDefaults(defaultSection, connectionSection));
    }

    private final Properties properties;
    private final ObjectMapper mapper;
    private final Values defaultSection;
    private final Section connectionSection;

    /**
     * {@inheritDoc}
     */
    @Override
    public Values getConnectionSettings() {
        return connectionSection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Section> getCleanupMethodSettings() {
        final String[] keys = properties.getProperty("methods").split(",");
        final List<Section> sections = new ArrayList<Section>(keys.length);
        for (final String key : keys) {
            final Section section = new PropertiesSection(String.format("method.%s.", key), defaultSection);
            final String sharedSections = section.getValue("sharedConfig");
            if (sharedSections != null) {
                section.setDefaults(new AugmentedValues(defaultSection, getSharedSections(sharedSections)));
            }

            sections.add(section);
        }
        return sections;
    }

    private List<Section> getSharedSections(final String sharedSections) {
        final String[] keys = sharedSections.split(",");
        final List<Section> sections = new ArrayList<Section>(keys.length);
        for (final String key : keys) {
            final Section section = new PropertiesSection(String.format("section.%s.", key));
            sections.add(section);
        }
        return sections;
    }

    private class PropertiesSection implements Section {
        private final String prefix;
        private Values defaults;

        private List<String> stringToList(final String value) {
            try {
                return mapper.readValue(value, new TypeReference<List<String>>() {
                });
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        public PropertiesSection(final String prefix) {
            this(prefix, NullValues.INSTANCE);
        }

        public PropertiesSection(final String prefix, final Values defaults) {
            this.prefix = prefix;
            this.defaults = defaults;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getValue(final String key) {
            final String value = properties.getProperty(prefix + key);
            if (value != null) {
                return value;
            }
            return defaults.getValue(key);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> getList(final String key) {
            final String value = properties.getProperty(prefix + key);
            if (value != null) {
                return stringToList(value);
            }
            return defaults.getList(key);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Values getDefaults() {
            return defaults;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setDefaults(final Values defaults) {
            this.defaults = defaults;
        }
    }

}
