package info.huggard.charlie.ews;

import java.util.List;

/**
 * Configuration for the email cleanup script.
 * @author Charlie Huggard
 */
public interface Configuration {

    Values getConnectionSettings();

    List<Section> getCleanupMethodSettings();

    public interface Section extends Values {
        Values getDefaults();

        void setDefaults(Values defaults);
    }

    public interface Values {
        String getValue(String key);

        List<String> getList(String key);
    }

}
