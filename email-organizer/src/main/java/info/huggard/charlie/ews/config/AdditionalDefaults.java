package info.huggard.charlie.ews.config;

import info.huggard.charlie.ews.Configuration.Values;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class to add simple defaults before another defaults class.
 * @author Charlie Huggard
 */
public class AdditionalDefaults implements Values {

    public AdditionalDefaults(final Values defaults, final Map<String, String> values,
            final Map<String, List<String>> lists) {
        this.defaults = defaults;
        if (values == null) {
            this.values = Collections.emptyMap();
        } else {
            this.values = new HashMap<String, String>(values);
        }
        if (lists == null) {
            this.lists = Collections.emptyMap();
        } else {
            this.lists = new HashMap<String, List<String>>(lists);
        }
    }

    private final Values defaults;
    private final Map<String, String> values;
    private final Map<String, List<String>> lists;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getValue(final String key) {
        if (values.containsKey(key)) {
            return values.get(key);
        }
        return defaults.getValue(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getList(final String key) {
        if (lists.containsKey(key)) {
            return lists.get(key);
        }
        return defaults.getList(key);
    }

}
