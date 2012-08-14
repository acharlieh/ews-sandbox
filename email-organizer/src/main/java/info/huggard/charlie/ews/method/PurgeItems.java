package info.huggard.charlie.ews.method;

import info.huggard.charlie.ews.CleanupMethod;
import info.huggard.charlie.ews.Configuration.Section;
import info.huggard.charlie.ews.util.EWSUtil;
import info.huggard.charlie.ews.util.ItemToItemId;

import java.util.Date;

import microsoft.exchange.webservices.data.AffectedTaskOccurrence;
import microsoft.exchange.webservices.data.DeleteMode;
import microsoft.exchange.webservices.data.FindFoldersResults;
import microsoft.exchange.webservices.data.FindItemsResults;
import microsoft.exchange.webservices.data.Folder;
import microsoft.exchange.webservices.data.FolderView;
import microsoft.exchange.webservices.data.Item;
import microsoft.exchange.webservices.data.ItemId;
import microsoft.exchange.webservices.data.ItemSchema;
import microsoft.exchange.webservices.data.ItemView;
import microsoft.exchange.webservices.data.SearchFilter;
import microsoft.exchange.webservices.data.SendCancellationsMode;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import com.google.common.collect.Iterables;

/**
 * Deletes items that are older than a set number of days.
 * @author Charlie Huggard
 */
public class PurgeItems implements CleanupMethod {

    private Section config;

    private Integer purgeDays;

    private boolean includeSubfolders;

    private DeleteMode deleteMode;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConfig(final Section config) {
        this.config = config;

        final String purgeString = config.getValue("purgeDays");
        if (purgeString != null) {
            purgeDays = Integer.parseInt(purgeString);
            if (purgeDays <= 0) {
                purgeDays = null;
            }
        } else {
            purgeDays = 7;
        }

        final String subfolders = config.getValue("subfolders");

        if (subfolders != null) {
            includeSubfolders = Boolean.parseBoolean(subfolders);
        } else {
            includeSubfolders = true;
        }

        deleteMode = DeleteMode.valueOf(config.getValue("deleteMode"));
    }

    private Date daysAgo(final int days) {
        return new LocalDate().minus(Days.days(days)).toDate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(final EWSUtil util) throws Exception {
        if (purgeDays == null) {
            return;
        }
        final Folder folder = util.getStartingFolder(config);

        final SearchFilter filter = new SearchFilter.IsLessThan(ItemSchema.DateTimeReceived, daysAgo(purgeDays));

        purge(folder, filter);
    }

    private void purge(final Folder folder, final SearchFilter filter) throws Exception {
        final int num = folder.getChildFolderCount();
        if (includeSubfolders && num > 0) {
            final FolderView folderView = new FolderView(num);
            final FindFoldersResults children = folder.findFolders(folderView);
            for (final Folder child : children) {
                purge(child, filter);
                final ItemView view = new ItemView(1);
                if (child.findItems(view).getTotalCount() == 0) {
                    child.delete(DeleteMode.HardDelete);
                }
            }
        }

        while (deleteItems(folder, filter)) {
        }
    }

    private boolean deleteItems(final Folder folder, final SearchFilter filter) throws Exception {
        final ItemView view = new ItemView(500);
        final FindItemsResults<Item> items = folder.findItems(filter, view);
        if (items.getTotalCount() > 0) {
            final Iterable<ItemId> itemIds = Iterables.transform(items, ItemToItemId.INSTANCE);
            folder.getService().deleteItems(itemIds, deleteMode, SendCancellationsMode.SendToNone,
                    AffectedTaskOccurrence.SpecifiedOccurrenceOnly);
        }
        return items.isMoreAvailable();
    }
}
