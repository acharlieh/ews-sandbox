package info.huggard.charlie.ews.method;

import info.huggard.charlie.ews.CleanupMethod;
import info.huggard.charlie.ews.Configuration.Section;
import info.huggard.charlie.ews.config.AdditionalDefaults;
import info.huggard.charlie.ews.util.EWSUtil;
import info.huggard.charlie.ews.util.ItemToItemId;

import java.util.Date;
import java.util.Map;

import microsoft.exchange.webservices.data.AcceptMeetingInvitationMessage;
import microsoft.exchange.webservices.data.AwesomeRemoveFromCalendar;
import microsoft.exchange.webservices.data.ComparisonMode;
import microsoft.exchange.webservices.data.ContainmentMode;
import microsoft.exchange.webservices.data.FindItemsResults;
import microsoft.exchange.webservices.data.Folder;
import microsoft.exchange.webservices.data.FolderId;
import microsoft.exchange.webservices.data.Item;
import microsoft.exchange.webservices.data.ItemSchema;
import microsoft.exchange.webservices.data.ItemView;
import microsoft.exchange.webservices.data.Mailbox;
import microsoft.exchange.webservices.data.MeetingCancellation;
import microsoft.exchange.webservices.data.MeetingRequest;
import microsoft.exchange.webservices.data.SearchFilter;
import microsoft.exchange.webservices.data.SortDirection;
import microsoft.exchange.webservices.data.WellKnownFolderName;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

/**
 * A Cleanup Method to accept all requests sent to a shared calendar.
 * @author Charlie Huggard
 */
public class AcceptRequests implements CleanupMethod {
    private static Map<String, String> CONFIG_DEFAULTS = new ImmutableMap.Builder<String, String>() // .
            .put("publish", "true"). //$NON-NLS-1$ //$NON-NLS-2$
            build();

    private Section config;

    private PublishRequests publisher;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConfig(final Section config) {
        config.setDefaults(new AdditionalDefaults(config.getDefaults(), CONFIG_DEFAULTS, null));
        this.config = config;

        if (Boolean.parseBoolean(config.getValue("publish"))) { //$NON-NLS-1$
            publisher = new PublishRequests();
            publisher.setConfig(config);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(final EWSUtil util) throws Exception {
        final Folder folder = util.getStartingFolder(config);

        final ItemView view = new ItemView(50);
        view.getOrderBy().add(ItemSchema.DateTimeReceived, SortDirection.Ascending);

        // Find all meeting related messages
        final SearchFilter filter = new SearchFilter.ContainsSubstring(ItemSchema.ItemClass, "IPM.Schedule.Meeting.", //$NON-NLS-1$
                ContainmentMode.Prefixed, ComparisonMode.Exact);

        final Mailbox mailbox = util.getConfiguredMailbox(config);
        final FolderId calendarFolder = new FolderId(WellKnownFolderName.Calendar, mailbox);
        final FolderId sharedDeletedItems = new FolderId(WellKnownFolderName.DeletedItems, mailbox);

        boolean more = true;
        boolean changed = false;
        while (more) {
            final FindItemsResults<Item> results = folder.findItems(filter, view);
            for (final Item item : results) {
                if (item instanceof MeetingRequest) {
                    final AcceptMeetingInvitationMessage acceptMessage = ((MeetingRequest) item)
                            .createAcceptMessage(false);
                    acceptMessage.calendarSave(calendarFolder);
                    changed = true;
                } else if (item instanceof MeetingCancellation) {
                    // Yes it's strange... but I think I have to specify calendar folder.
                    // Haven't tested without.
                    new AwesomeRemoveFromCalendar(item).internalCreate(calendarFolder, null);
                    changed = true;
                }
            }
            if (results.getTotalCount() != 0) {
                util.getService().moveItems(Iterables.transform(results, ItemToItemId.INSTANCE), sharedDeletedItems);
            }

            more = results.isMoreAvailable();
        }
        if (changed && publisher != null) {
            System.out.printf("%s - Executing Class: %s\n", new Date(), publisher.getClass().getCanonicalName());
            publisher.execute(util);
        }
    }
}
