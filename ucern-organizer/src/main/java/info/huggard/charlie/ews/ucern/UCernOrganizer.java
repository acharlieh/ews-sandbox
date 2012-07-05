package info.huggard.charlie.ews.ucern;

import java.util.Date;
import java.util.List;

import microsoft.exchange.webservices.data.ComparisonMode;
import microsoft.exchange.webservices.data.ContainmentMode;
import microsoft.exchange.webservices.data.DeleteMode;
import microsoft.exchange.webservices.data.EmailMessageSchema;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.FindFoldersResults;
import microsoft.exchange.webservices.data.FindItemsResults;
import microsoft.exchange.webservices.data.Folder;
import microsoft.exchange.webservices.data.FolderId;
import microsoft.exchange.webservices.data.FolderSchema;
import microsoft.exchange.webservices.data.FolderView;
import microsoft.exchange.webservices.data.Item;
import microsoft.exchange.webservices.data.ItemSchema;
import microsoft.exchange.webservices.data.ItemView;
import microsoft.exchange.webservices.data.LogicalOperator;
import microsoft.exchange.webservices.data.Mailbox;
import microsoft.exchange.webservices.data.PostItem;
import microsoft.exchange.webservices.data.SearchFilter;
import microsoft.exchange.webservices.data.WellKnownFolderName;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.joda.time.Days;
import org.joda.time.LocalDate;

/**
 * @author Charlie Huggard
 */
@SuppressWarnings("all")
public class UCernOrganizer {

    private static String getFileName(final String... args) {
        return (args.length == 1) ? args[0] : "ucern-ews.properties";
    }

    private static ObjectMapper getObjectMapper() {
        final ObjectMapper toReturn = new ObjectMapper();
        toReturn.disable(SerializationConfig.Feature.USE_ANNOTATIONS);
        toReturn.disable(DeserializationConfig.Feature.USE_ANNOTATIONS);
        return toReturn;
    }

    public static interface Configuration {
        ExchangeService getService() throws Exception;

        Mailbox getMailbox();

        String getFrom();

        int getPurgeOlderThanDays();

        WellKnownFolderName startAt();

        DeleteMode getItemDeleteMode();

        List<String> getFolders() throws Exception;
    }

    public static void main(final String... args) throws Exception {
        final Configuration config = new FileConfiguration(getFileName(args), getObjectMapper());
        final ExchangeService service = config.getService();

        final Folder root = Folder.bind(service, new FolderId(config.startAt(), config.getMailbox()));
        final Folder uCern = traverseChildren(root, config.getFolders());

        while (sort(uCern, config.getFrom())) {
            ;
        }

        final int days = config.getPurgeOlderThanDays();

        if (days > 0) {
            final Item marker = makeMarker(uCern);
            final Date aWeekAgo = daysAgo(days);
            final SearchFilter toPurge = new SearchFilter.IsLessThan(ItemSchema.DateTimeReceived, aWeekAgo);

            purge(uCern, toPurge, config.getItemDeleteMode());
            marker.delete(DeleteMode.HardDelete);
        }
    }

    public static Date daysAgo(final int days) {
        return new LocalDate().minus(Days.days(days)).toDate();
    }

    public static Item makeMarker(final Folder folder) throws Exception {
        final Item post = new PostItem(folder.getService());
        post.save(folder.getId());
        return post;
    }

    public static void purge(final Folder folder, final SearchFilter filter, final DeleteMode mode) throws Exception {
        final int num = folder.getChildFolderCount();
        if (num > 0) {
            final FolderView folderView = new FolderView(num);
            final FindFoldersResults children = folder.findFolders(folderView);
            for (final Folder child : children) {
                purge(child, filter, mode);
            }
        }
        while (deleteItems(folder, filter, mode)) {
            ;
        }
        final ItemView view = new ItemView(1);
        if (folder.findItems(view).getTotalCount() == 0) {
            folder.delete(DeleteMode.HardDelete);
        }
    }

    public static boolean deleteItems(final Folder folder, final SearchFilter filter, final DeleteMode mode)
            throws Exception {
        final ItemView view = new ItemView(50);
        final FindItemsResults<Item> items = folder.findItems(filter, view);
        for (final Item item : items) {
            item.delete(mode);
        }
        return items.isMoreAvailable();
    }

    public static boolean sort(final Folder folder, final String fromEmail) throws Exception {
        final ItemView view = new ItemView(50);
        final SearchFilter filter = new SearchFilter.SearchFilterCollection(LogicalOperator.And,
                new SearchFilter.IsEqualTo(EmailMessageSchema.From, fromEmail), new SearchFilter.ContainsSubstring(
                        ItemSchema.Subject, "[", ContainmentMode.Prefixed, ComparisonMode.Exact));
        final FindItemsResults<Item> items = folder.findItems(filter, view);
        for (final Item item : items) {
            final String subject = item.getSubject();
            final String group = subject.split("]")[0].substring(1);
            Folder groupFolder = findChildFolder(folder, group);
            if (groupFolder == null) {
                groupFolder = createFolder(folder, group);
            }
            item.move(groupFolder.getId());
        }
        return items.isMoreAvailable();
    }

    public static Folder createFolder(final Folder folder, final String name) throws Exception {
        final Folder created = new Folder(folder.getService());
        created.setDisplayName(name);
        created.save(folder.getId());
        return created;
    }

    public static Folder findChildFolder(final Folder folder, final String name) throws Exception {
        final SearchFilter filter = new SearchFilter.IsEqualTo(FolderSchema.DisplayName, name);
        final FindFoldersResults results = folder.findFolders(filter, new FolderView(1));
        if (results.getTotalCount() == 0) {
            return null;
        }
        return results.iterator().next();
    }

    public static Folder traverseChildren(final Folder root, final List<String> paths) throws Exception {
        Folder current = root;
        for (final String path : paths) {
            final Folder child = findChildFolder(current, path);
            if (child == null) {
                throw new IllegalStateException("Folder '" + path + "' does not exist in folder '"
                        + current.getDisplayName() + "'");
            }
            current = child;
        }
        return current;
    }

    private static void printFolder(final Folder folder) throws Exception {
        System.out.printf("%s - %s\n", folder.getDisplayName(), folder.getId());
    }
}
