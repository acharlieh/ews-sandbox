package info.huggard.charlie.ews.util;

import microsoft.exchange.webservices.data.Item;
import microsoft.exchange.webservices.data.ItemId;
import microsoft.exchange.webservices.data.ServiceLocalException;

import com.google.common.base.Function;

/**
 * Transforms an Item to an ItemId.
 * @author Charlie Huggard
 */
public class ItemToItemId implements Function<Item, ItemId> {

    /**
     * An INSTANCE of this function.
     */
    public static final Function<Item, ItemId> INSTANCE = new ItemToItemId();

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemId apply(final Item input) {
        if (input == null) {
            return null;
        }
        try {
            return input.getId();
        } catch (final ServiceLocalException e) {
            throw new RuntimeException(e);
        }
    }

}
