package info.huggard.charlie.ews.method;

import info.huggard.charlie.ews.CleanupMethod;
import info.huggard.charlie.ews.Configuration.Section;
import info.huggard.charlie.ews.util.EWSUtil;
import microsoft.exchange.webservices.data.DeleteMode;
import microsoft.exchange.webservices.data.FindFoldersResults;
import microsoft.exchange.webservices.data.FindItemsResults;
import microsoft.exchange.webservices.data.Folder;
import microsoft.exchange.webservices.data.FolderView;
import microsoft.exchange.webservices.data.Item;
import microsoft.exchange.webservices.data.ItemView;

/**
 * The opposite of SortEmails, this takes all items in subfolders and merges them back to the parent.
 * @author Charlie Huggard
 */
public class UnsortEmails implements CleanupMethod {

    private Section config;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConfig(final Section config) {
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(final EWSUtil util) throws Exception {
        final Folder folder = util.getStartingFolder(config);

        final int num = folder.getChildFolderCount();
        if (num == 0) {
            return;
        }
        final FolderView view = new FolderView(num);
        final FindFoldersResults results = folder.findFolders(view);

        for (final Folder child : results) {
            while (unsort(folder, child)) {
                // NOOP
            }
            child.delete(DeleteMode.HardDelete);
        }
    }

    private boolean unsort(final Folder parent, final Folder child) throws Exception {
        final ItemView view = new ItemView(50);
        final FindItemsResults<Item> items = child.findItems(view);
        for (final Item item : items) {
            item.move(parent.getId());
        }
        return items.isMoreAvailable();
    }
}
