package codeu.chat.util.store;

import java.util.Comparator;
import java.util.Iterator;

public class BTreeIterator<KEY, VALUE> implements Iterator<VALUE> {
    protected BTreeStore<KEY, VALUE> curr;
    protected int index;

    private final KEY maxKey;
    private final Comparator<? super KEY> comparator;
    private boolean inclusiveBound;

    public BTreeIterator(BTreeStore<KEY, VALUE> curr, int index, KEY maxKey, Comparator<? super KEY> comparator) {
        this.curr = curr;
        this.index = index;
        this.maxKey = maxKey;
        this.comparator = comparator;
        this.inclusiveBound = true;
    }

    @Override
    public boolean hasNext() {
        return curr != null && ((inclusiveBound && comparator.compare(curr.getKey(index), maxKey) <= 0)
            || (!inclusiveBound && comparator.compare(curr.getKey(index), maxKey) < 0));
    }

    @Override
    public VALUE next() {
        VALUE nextElem = curr.getValue(index);
        calculateNext();
        return nextElem;
    }

    public KEY getKey() {
        return curr != null ? curr.getKey(index) : null;
    }

    protected void setExclusive() {
        inclusiveBound = false;
    }

    private void calculateNext() {
        if (curr.getChild(index + 1) != null) {
            // go to smallest child
            curr = curr.minimumTree(curr.getChild(index + 1));
            index = 0;
        } else if (index + 1 < curr.getNumElems()) {
            // go to next elem on same node
            index++;
        } else {
            // go to parent
            BTreeStore<KEY, VALUE> next = curr.getParent();
            BTreeStore<KEY, VALUE> prev = curr;
            int nextIndex;
            KEY prevKey = curr.getKey(index);
            curr = next;
            while (next != null) {
                nextIndex = next.getNext(prevKey, next.getNumElems());
                while (nextIndex < next.getNumElems() + 1 &&
                    next.getChild(nextIndex) != prev) { //equality check to find where we left off
                    nextIndex++;
                }
                if (nextIndex < next.getNumElems()) {
                    curr = next;
                    index = nextIndex;
                    break;
                }
                prev = next;
                next = next.getParent();
                curr = next;
            }
        }
    }
}
