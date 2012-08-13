package info.huggard.charlie.ews.config;

import info.huggard.charlie.ews.Configuration.Values;

import java.util.Collections;
import java.util.List;

import microsoft.exchange.webservices.data.DeleteMode;
import microsoft.exchange.webservices.data.WellKnownFolderName;

/**
 * Last layer of default settings. Included are those implied by other default values / defined settings.
 * @author Charlie Huggard
 */
public class ImplicitDefaults implements Values {
    private final Values connectionConfig;

    public ImplicitDefaults(final Values connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getValue(final String key) {
        if (key.equals("mailbox")) {
            return String.format("%s@Cerner.com", connectionConfig.getValue("user"));
        }
        if (key.equals("folderParent")) {
            return WellKnownFolderName.MsgFolderRoot.name();
        }
        if (key.equals("deleteMode")) {
            return DeleteMode.MoveToDeletedItems.name();
        }
        if (key.equals("deleteMode")) {
            return DeleteMode.MoveToDeletedItems.name();
        }
        if (key.equals("subfolders")) {
            return "true";
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getList(final String key) {
        if (key.equals("path")) {
            return Collections.emptyList();
        }
        return null;
    }

}
