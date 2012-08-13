package info.huggard.charlie.ews.config;

import info.huggard.charlie.ews.Configuration.Values;

import java.util.List;

/**
 * An object holding no values
 * @author Charlie Huggard
 */
public class NullValues implements Values {

    public static final Values INSTANCE = new NullValues();

    /**
     * {@inheritDoc}
     */
    @Override
    public String getValue(final String key) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getList(final String key) {
        return null;
    }

}
