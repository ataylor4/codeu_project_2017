package codeu.chat.util.store;

import java.util.Comparator;

/**
 * Generates iterator for traversing the BTreeStore
 * Includes a bound on the highest element (dynamically calculates the next element)
 */
@SuppressWarnings("unchecked")
public class BTreeIterable<KEY, VALUE> implements Iterable<VALUE> {
    private BTreeIterator<KEY, VALUE> iterator;

    protected BTreeIterable(BTreeStore<KEY, VALUE> curr, int index, KEY maxKey, Comparator<? super KEY> comparator) {
        iterator = new BTreeIterator<>(curr, index, maxKey, comparator);
    }

    protected BTreeIterable(BTreeIterator<KEY, VALUE> iterator) {
        this.iterator = iterator;
    }

    public BTreeIterable<KEY, VALUE> setExclusive() {
        iterator.setExclusive();
        return this;
    }

    @Override
    public BTreeIterator<KEY, VALUE> iterator() {
        return iterator;
    }
}
