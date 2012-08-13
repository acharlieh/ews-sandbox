package info.huggard.charlie.ews.util;

import info.huggard.charlie.ews.Configuration.Values;

import java.util.List;

import microsoft.exchange.webservices.data.DeleteMode;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.FindFoldersResults;
import microsoft.exchange.webservices.data.Folder;
import microsoft.exchange.webservices.data.FolderId;
import microsoft.exchange.webservices.data.FolderSchema;
import microsoft.exchange.webservices.data.FolderView;
import microsoft.exchange.webservices.data.Mailbox;
import microsoft.exchange.webservices.data.SearchFilter;
import microsoft.exchange.webservices.data.WellKnownFolderName;

/**
 * Class of various Exchange Web Services utilities.
 * @author Charlie Huggard
 */
@SuppressWarnings("javadoc")
public class EWSUtil {

    private final ExchangeService service;

    public EWSUtil(final ExchangeService service) {
        this.service = service;
    }

    // Common Settings Intrepretation
    public Mailbox getConfiguredMailbox(final Values settings) {
        return new Mailbox(settings.getValue("mailbox"));
    }

    public WellKnownFolderName getParentFolder(final Values settings) {
        return WellKnownFolderName.valueOf(settings.getValue("folderParent"));
    }

    public List<String> getFolderPath(final Values settings) {
        return settings.getList("path");
    }

    public DeleteMode getDeleteMode(final Values settings) {
        return DeleteMode.valueOf(settings.getValue("deleteMode"));
    }

    // EWS Transformations
    public ExchangeService getService() {
        return service;
    }

    public Folder getStartingFolder(final Values settings) throws Exception {
        final Mailbox mailbox = getConfiguredMailbox(settings);
        final WellKnownFolderName startingPoint = getParentFolder(settings);
        final List<String> folderPath = getFolderPath(settings);

        final Folder root = Folder.bind(service, new FolderId(startingPoint, mailbox));
        return traverseOrCreateChildren(root, folderPath);
    }

    public Folder createFolder(final Folder folder, final String name) throws Exception {
        final Folder created = new Folder(folder.getService());
        created.setDisplayName(name);
        created.save(folder.getId());
        return created;
    }

    private Folder findChildFolder(final Folder folder, final String name) throws Exception {
        final SearchFilter filter = new SearchFilter.IsEqualTo(FolderSchema.DisplayName, name);
        final FindFoldersResults results = folder.findFolders(filter, new FolderView(1));
        if (results.getTotalCount() == 0) {
            return null;
        }
        return results.iterator().next();
    }

    public Folder traverseChildren(final Folder root, final List<String> path) throws Exception {
        return traverseChildren(root, path, false);
    }

    public Folder traverseOrCreateChildren(final Folder root, final List<String> path) throws Exception {
        return traverseChildren(root, path, true);
    }

    private Folder traverseChildren(final Folder root, final List<String> paths, final boolean create) throws Exception {
        Folder current = root;
        for (final String path : paths) {
            Folder child = findChildFolder(current, path);
            if (child == null) {
                if (create) {
                    child = createFolder(current, path);
                } else {
                    throw new IllegalStateException(String.format("Folder '%s' does not exist in folder '%s'", path,
                            current.getDisplayName()));
                }
            }
            current = child;
        }
        return current;
    }
}
