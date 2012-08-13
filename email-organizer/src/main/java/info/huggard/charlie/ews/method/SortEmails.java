package info.huggard.charlie.ews.method;

import info.huggard.charlie.ews.CleanupMethod;
import info.huggard.charlie.ews.Configuration.Section;
import info.huggard.charlie.ews.util.EWSUtil;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import microsoft.exchange.webservices.data.DeleteMode;
import microsoft.exchange.webservices.data.EmailMessageSchema;
import microsoft.exchange.webservices.data.FindItemsResults;
import microsoft.exchange.webservices.data.Folder;
import microsoft.exchange.webservices.data.Item;
import microsoft.exchange.webservices.data.ItemView;
import microsoft.exchange.webservices.data.SearchFilter;

/**
 * A Cleanup Method to sort emails.
 * @author Charlie Huggard
 */
public class SortEmails implements CleanupMethod {

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

        final String from = config.getValue("sender");
        final SearchFilter filter;
        if (from == null) {
            filter = new SearchFilter.Exists(EmailMessageSchema.From);
        } else {
            filter = new SearchFilter.IsEqualTo(EmailMessageSchema.From, from);
        }

        final Pattern pattern = Pattern.compile(config.getValue("subjectPattern"));

        while (sort(util, folder, filter, pattern)) {
        }
    }

    private boolean sort(final EWSUtil util, final Folder folder, final SearchFilter filter,
            final Pattern subjectPattern) throws Exception {
        final ItemView view = new ItemView(50);
        final FindItemsResults<Item> items = folder.findItems(filter, view);
        for (final Item item : items) {
            final String subject = item.getSubject();

            if (subject == null) {
                item.delete(DeleteMode.MoveToDeletedItems);
                break;
            }
            final Matcher matcher = subjectPattern.matcher(subject);

            final Folder childFolder = util.traverseOrCreateChildren(folder, Collections
                    .singletonList(getGroup(matcher)));

            item.move(childFolder.getId());
        }
        return items.isMoreAvailable();
    }

    private String getGroup(final Matcher matcher) {
        if (matcher.matches()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                final String group = matcher.group(i);
                if (group != null) {
                    return group;
                }
            }
        }
        return "___UNKNOWN___"; //$NON-NLS-1$
    }
}
