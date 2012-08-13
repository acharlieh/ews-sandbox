package info.huggard.charlie.ews.config;

import info.huggard.charlie.ews.Configuration.Values;

import java.util.ArrayList;
import java.util.List;

/**
 * AugmentedValues attempts to get values from an arbitrary set of value objects. This object cannot be used to block
 * the resolution of a value, but rather only augment the set of available default values.
 * @author Charlie Huggard
 */
public class AugmentedValues implements Values {

    private final Values defaults;
    private final List<Values> values;

    public AugmentedValues(final Values defaults, final List<? extends Values> values) {
        this.defaults = defaults;
        this.values = new ArrayList<Values>(values);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getValue(final String key) {
        for (final Values valueSet : values) {
            final String value = valueSet.getValue(key);
            if (value != null) {
                return value;
            }
        }
        return defaults.getValue(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getList(final String key) {
        for (final Values valueSet : values) {
            final List<String> value = valueSet.getList(key);
            if (value != null) {
                return value;
            }
        }
        return defaults.getList(key);
    }

}
