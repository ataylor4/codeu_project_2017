package codeu.chat.util.store;

import codeu.chat.util.Logger;
import codeu.chat.util.Serializer;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

@SuppressWarnings("unchecked")
public class BTreeStore<KEY, VALUE> implements StoreAccessor<KEY, VALUE> {
    public static final int NUM_POINTERS = 2;

    private static final Logger.Log LOG = Logger.newLog(BTreeStore.class);
    private static final int INSERTION = 1;
    private static final int DELETION = 2;
    private static final int UPDATE = 3;

    private BTreeStore<KEY, VALUE> parent;
    private Object[] children;
    private Object[] keys;
    private Object[] values;
    private BTreeInformation<KEY, VALUE> treeInformation;
    private int numElems;

    /**
     * Constructor for a B-Tree
     * @param minNumPointers: The maximum amount of pointers any node of the tree can have.
     * @param comparator: The compare function that should be use to order elements
     *                  within the tree. See
     *                  https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html.
     */
    public BTreeStore(int minNumPointers, Comparator<? super KEY> comparator, Serializer<KEY> keySerializer,
                      Serializer<VALUE> valueSerializer, String filename) {
        this(new BTreeInformation<>(minNumPointers, comparator, keySerializer, valueSerializer, filename));
        if (minNumPointers < 2) { // a BTree is only well defined if the min num pointers/node >= 2
            throw new IllegalArgumentException(
                "Must have a minimimum of two pointers per node (when full)");
        }
        numElems = 0;
        try {
            boolean fileExists = treeInformation.file.exists();
            if (fileExists) {
                createTreeFromFile(treeInformation.file);
            } else {
                treeInformation.file.createNewFile();
                LOG.warning("Creating the file... could not recover the tree");
            }
        } catch (IOException e) {
            LOG.error(e, "Error creating or reading the file");
        }
    }

    private BTreeStore(BTreeInformation<KEY, VALUE> treeInformation) {
        this.treeInformation = treeInformation;
        parent = null;

        children = new Object[treeInformation.maxNumPointers];
        keys = new Object[treeInformation.maxNumPointers - 1];
        values = new Object[treeInformation.maxNumPointers - 1];
        numElems = 0;
    }

    private void createTreeFromFile(File file) throws IOException {
        try(InputStream inputStream = new FileInputStream(file)) {
            BTreeStore<KEY, VALUE> root = new BTreeStore<>(treeInformation);
            while (true) {
                int read = inputStream.read();
                if (read == -1) {
                    break;
                }
                if (read == INSERTION) {
                    KEY key = treeInformation.keySerializer.read(inputStream);
                    VALUE value = treeInformation.valueSerializer.read(inputStream);
                    root = root.insert(key, value, false, false);
                } else if (read == DELETION) {
                    KEY key = treeInformation.keySerializer.read(inputStream);
                    root = root.delete(key, false);
                } else if (read == UPDATE) {
                    KEY key = treeInformation.keySerializer.read(inputStream);
                    VALUE value = treeInformation.valueSerializer.read(inputStream);
                    root.modify(key, value);
                }
            }
            deepCopy(root);
        }
    }

    private void deepCopy(BTreeStore<KEY, VALUE> root) {
        this.parent = root.parent;
        this.children = root.children;
        this.keys = root.keys;
        this.values = root.values;
        this.treeInformation = root.treeInformation;
        this.numElems = root.numElems;
        for (int i = 0; i <= numElems; i++) {
            if (children[i] == null) {
                continue;
            }
            ((BTreeStore<KEY, VALUE>) children[i]).parent = this;
        }
    }

    public void clear(String filename) {
        parent = null;
        children = new Object[treeInformation.maxNumPointers];
        keys = new Object[treeInformation.maxNumPointers - 1];
        values = new Object[treeInformation.maxNumPointers - 1];
        numElems = 0;
        treeInformation.file.delete();
        treeInformation.file = new File(filename);
        try {
            treeInformation.file.createNewFile();
        } catch (IOException e) {
            LOG.error(e, "Error creating new file on clearing tree");
        }

    }

    /**
     * To be used whenever the value associated with a key is changed
     * (current use case: a message is added to a conversation, so we need to update the conversation object)
     * Note: needs to be called EVEN IF the memory address of value is unchanged
     * Key must be in the tree, or an IllegalArgumentException is thrown
     * @param key - the key associated with the updated value
     * @param value - the updated value
     * @return true if the key was successfully updated, false otherwise
     */
    @Override
    public boolean update(KEY key, VALUE value) {
        if (modify(key, value)) {
            try (FileOutputStream outputStream = new FileOutputStream(treeInformation.file, true)) {
                outputStream.write(UPDATE);
                treeInformation.keySerializer.write(outputStream, key);
                treeInformation.valueSerializer.write(outputStream, value);
                outputStream.flush();
            } catch (IOException e) {
                LOG.error(e, "Error updating key and value");
            }
            return true;
        }
        return false;
    }

    private boolean modify(KEY key, VALUE value) {
        BTreeIterator<KEY, VALUE> toChange = at(key).iterator();
        if (toChange.hasNext()) {
            toChange.curr.values[toChange.index] = value;
        }
        return toChange.hasNext();
    }

    public VALUE first(KEY elem) {
        BTreeIterator<KEY, VALUE> result = at(elem).iterator();
        return result.hasNext() ? result.next() : null;
    }

    public BTreeIterable<KEY, VALUE> at(KEY elem) {
        return range(elem, elem);
    }

    public BTreeIterable<KEY, VALUE> range(KEY min, KEY max) {
        if (this.numElems == 0) {
            return new BTreeIterable<>(null, 0, null, treeInformation.comparator);
        }
        BTreeStore<KEY, VALUE> curr = this;
        BTreeIterable<KEY, VALUE> currentCeiling = new BTreeIterable<>(null, 0, max, treeInformation.comparator);
        while (curr != null) {
            int pointerIndex = curr.getNext(min, curr.numElems);
            if (pointerIndex < curr.numElems
                && (treeInformation.comparator.compare(min, (KEY) curr.keys[pointerIndex]) == 0
                    || (curr.children[pointerIndex] == null
                        && treeInformation.comparator.compare(max, (KEY) curr.keys[pointerIndex]) > 0))) {
                BTreeStore<KEY, VALUE> relevantChild = (BTreeStore<KEY, VALUE>) curr.children[pointerIndex];
                if (relevantChild != null) {
                    BTreeStore<KEY, VALUE> maximumTree = maximumTree(relevantChild);
                    if (treeInformation.comparator
                        .compare(min, (KEY) maximumTree.keys[maximumTree.numElems - 1]) != 0) {
                        return new BTreeIterable<>(curr, pointerIndex, max, treeInformation.comparator);
                    }
                } else {
                    return new BTreeIterable<>(curr, pointerIndex, max, treeInformation.comparator);
                }
            }
            if (pointerIndex < curr.numElems) {
                currentCeiling = new BTreeIterable<>(curr, pointerIndex, max, treeInformation.comparator);
            }
            curr = (BTreeStore<KEY, VALUE>) curr.children[pointerIndex];
        }
        return currentCeiling;
    }

    public BTreeIterable<KEY, VALUE> all() {
        if (this.numElems == 0) {
            return new BTreeIterable<>(null, 0, null, treeInformation.comparator);
        }
        BTreeStore<KEY, VALUE> min = minimumTree(this);
        BTreeStore<KEY, VALUE> max = maximumTree(this);
        return range((KEY) min.keys[0], (KEY) max.keys[max.numElems - 1]);
    }

    public BTreeIterable<KEY, VALUE> after(KEY start) {
        if (this.numElems == 0) {
            return new BTreeIterable<>(null, 0, null, treeInformation.comparator);
        }
        BTreeStore<KEY, VALUE> max = maximumTree(this);
        BTreeIterator<KEY, VALUE> result = range(start, (KEY) max.keys[max.numElems - 1]).iterator();
        while (result.getKey() != null &&
            treeInformation.comparator.compare(result.getKey(), start) == 0) {
            result.next();
        }
        return new BTreeIterable<>(result);
    }

    public BTreeIterable<KEY, VALUE> before(KEY end) {
        if (this.numElems == 0) {
            return new BTreeIterable<>(null, 0, null, treeInformation.comparator);
        }
        BTreeStore<KEY, VALUE> min = minimumTree(this);
        return range((KEY) min.keys[0], end).setExclusive();
    }

    private BTreeStore<KEY, VALUE> insert(KEY key, VALUE value, boolean allowDuplicates, boolean writeToFile) {
        BTreeStore<KEY, VALUE> curr = this;
        while (true) {
            int index = curr.getNext(key, curr.numElems);
            if (!allowDuplicates && index < curr.numElems && key.equals(curr.keys[index])) {
                return this;
            }
            if (curr.children[index] == null) {
                //first call, prev's value doesn't matter
                BTreeStore<KEY, VALUE> newRoot = insertIntoNode(curr, key, value, null, curr);
                if (writeToFile) {
                    try (FileOutputStream outputStream = new FileOutputStream(treeInformation.file, true)) {
                        outputStream.write(INSERTION);
                        treeInformation.keySerializer.write(outputStream, key);
                        treeInformation.valueSerializer.write(outputStream, value);
                        outputStream.flush();
                    } catch (IOException e) {
                        LOG.error(e, "Error writing to file on insertion.", key, treeInformation.file);
                    }
                }
                return newRoot == null ? this : newRoot;
            }
            curr = (BTreeStore<KEY, VALUE>) curr.children[index];
        }
    }

    /**
     * Inserts the given element into the tree.
     * @param key: the element to add the tree under
     * @param value: The element to add into the tree
     * @param allowDuplicates: true if allowDuplicates elements are allowed
     * @return The root of the tree after the insertion.
     */
    public BTreeStore<KEY, VALUE> insert(KEY key, VALUE value, boolean allowDuplicates) {
        //always write to file upon real insert
        return insert(key, value, allowDuplicates, true);
    }

    private BTreeStore<KEY, VALUE> delete(KEY elem, boolean writeToFile) {
        BTreeIterator<KEY, VALUE> toDelete = at(elem).iterator();
        BTreeStore<KEY, VALUE> result = this;
        if (toDelete.hasNext()) {
            result = removeFromTree(toDelete.curr, toDelete.index);
            if (writeToFile) {
                try (FileOutputStream outputStream = new FileOutputStream(treeInformation.file, true)) {
                    outputStream.write(DELETION);
                    treeInformation.keySerializer.write(outputStream, elem);
                    outputStream.flush();
                } catch (IOException e) {
                    LOG.error(e, "Error writing to file on deletion.", elem, treeInformation.file);
                }
            }
            if (result == null) {
                result = this;
            }
        }
        return result;
    }

    /**
     * Deletes the given element into the tree (or does nothing, if no element with the given key exists)
     * If multiple elements with the same key to be deleted exists, only one will be deleted
     * @param elem: the key of the element to be deleted
     * @return the root of the tree after the deletion
     */
    public BTreeStore<KEY, VALUE> delete(KEY elem) {
        return delete(elem, true);
    }

    /**
     * Returns a string representation of the tree.
     * @return A string with all elements in the tree in sorted order,
     * with spaces between the elements
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        printTree(this, builder);
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    protected KEY getKey(int index) {
        return (KEY) keys[index];
    }

    protected VALUE getValue(int index) {
        return (VALUE) values[index];
    }

    protected BTreeStore<KEY, VALUE> getChild(int index) {
        return (BTreeStore<KEY, VALUE>) children[index];
    }

    protected int getNumElems() {
        return numElems;
    }

    protected BTreeStore<KEY, VALUE> getParent() {
        return parent;
    }

    // A helper method that will insert the given key and associated value into the node given by toInsert
    // child is the child associated with that key, and prev is the previous node we were inserting
    // returns null, if the root is unchanged, or the new root
    private BTreeStore<KEY, VALUE> insertIntoNode(BTreeStore<KEY, VALUE> toInsert, KEY key, VALUE value,
                                                  BTreeStore<KEY, VALUE> child, BTreeStore<KEY, VALUE> prev) {
        if (toInsert == null) {
            return editRoot(prev, child, key, value);
        }
        int indexToInsert = toInsert.getNext(key, toInsert.numElems);

        // insert directly into nodes with space
        if (toInsert.numElems < toInsert.treeInformation.maxNumPointers - 1) {
            toInsert.addElem(key, value, indexToInsert, child);
            toInsert.numElems++;
        } else {
            int centerIndex = (treeInformation.maxNumPointers + 1) / 2;
            int adjustedCenter = centerIndex > indexToInsert ? centerIndex - 1 : centerIndex;

            KEY centerKey = (KEY) toInsert.keys[adjustedCenter];
            VALUE centerValue = (VALUE) toInsert.values[adjustedCenter];
            BTreeStore<KEY, VALUE> newPointer = new BTreeStore<>(treeInformation);
            newPointer.parent = toInsert.parent;

            for (int i = adjustedCenter + 1; i < keys.length; i++) {
                newPointer.keys[i - (adjustedCenter + 1)] = toInsert.keys[i];
                newPointer.values[i - (adjustedCenter + 1)] = toInsert.values[i];
                toInsert.keys[i] = null;
                toInsert.values[i] = null;
            }
            toInsert.keys[adjustedCenter] = null;
            toInsert.values[adjustedCenter] = null;

            // need to deal with child pointers in non-leaf case
            if (toInsert.children[0] != null) {
                for (int i = adjustedCenter + 1; i < toInsert.children.length; i++) {
                    newPointer.children[i - (adjustedCenter + 1)] = toInsert.children[i];
                    ((BTreeStore<KEY, VALUE>)toInsert.children[i]).parent = newPointer;
                    toInsert.children[i] = null;
                }
            }

            if (centerIndex == indexToInsert) {
                insertIntoArray(newPointer.keys, centerKey, 0);
                insertIntoArray(newPointer.values, centerValue, 0);
                centerKey = key;
                centerValue = value;
                if (child != null) {
                    insertIntoArray(newPointer.children, child, 0);
                    child.parent = newPointer;
                }
            } else {
                if ((treeInformation.maxNumPointers + 1) / 2 >= indexToInsert) {
                    toInsert.addElem(key, value, indexToInsert, child);
                } else {
                    newPointer.addElem(key, value, indexToInsert - (adjustedCenter + 1), child);
                }
            }
            newPointer.numElems = newPointer.keys.length - centerIndex;
            toInsert.numElems = centerIndex;
            return insertIntoNode(toInsert.parent, centerKey, centerValue, newPointer, toInsert);
        }
        return null;
    }

    // adds an element into the given node of the BTree
    private void addElem(KEY key, VALUE value, int indexToInsert, BTreeStore<KEY, VALUE> child) {
        insertIntoArray(keys, key, indexToInsert);
        insertIntoArray(values, value, indexToInsert);
        if (child != null) {
            insertIntoArray(children, child, indexToInsert + 1);
            child.parent = this;
        }
    }

    // handles the case of splitting the root node and creating a new root
    private BTreeStore<KEY, VALUE> editRoot(BTreeStore<KEY, VALUE> childOne, BTreeStore<KEY, VALUE> childTwo,
                                            KEY key, VALUE value) {
        BTreeStore<KEY, VALUE> newRoot = new BTreeStore<>(treeInformation);
        newRoot.keys[0] = key;
        newRoot.values[0] = value;
        newRoot.children[0] = childOne;
        childOne.parent = newRoot;
        newRoot.children[1] = childTwo;
        childTwo.parent = newRoot;
        newRoot.numElems = 1; //one element
        return newRoot;
    }

    // index is the index of the element immediately after elem
    private <V> void insertIntoArray(V[] arr, V elem, int index) {
        for (int i = arr.length - 2; i >= index; i--) {
            arr[i + 1] = arr[i];
        }
        arr[index] = elem;
    }

    private BTreeStore<KEY, VALUE> removeFromTree(BTreeStore<KEY, VALUE> deletingFrom, int indexToDelete) {
        // deleting from an interior node involves finding the predecessor element and moving it into the interior node
        // then recursing on the modified leaf node
        if (deletingFrom.children[indexToDelete] != null) {
            BTreeStore<KEY, VALUE> replacement
                = maximumTree((BTreeStore<KEY, VALUE>) deletingFrom.children[indexToDelete]);
            deletingFrom.keys[indexToDelete] = replacement.keys[replacement.numElems - 1];
            deletingFrom.values[indexToDelete] = replacement.values[replacement.numElems - 1];
            return removeFromTree(replacement, replacement.numElems - 1);
        }
        if (deletingFrom.numElems >= treeInformation.maxNumPointers / 2) {
            // don't need to rebalance the tree if we have enough keys
            deleteFromArray(deletingFrom.keys, indexToDelete);
            deleteFromArray(deletingFrom.values, indexToDelete);
            deletingFrom.numElems--;
            return null;
        }
        return deleteUpTheTree(deletingFrom, indexToDelete);
    }

    // deletes recursively traveling up the tree
    private BTreeStore<KEY, VALUE> deleteUpTheTree(BTreeStore<KEY, VALUE> deletingFrom, int indexToDelete) {
        KEY deletedKey = (KEY) deletingFrom.keys[indexToDelete];
        deleteFromArray(deletingFrom.keys, indexToDelete);
        deleteFromArray(deletingFrom.values, indexToDelete);
        deleteFromArray(deletingFrom.children, indexToDelete + 1);
        deletingFrom.numElems--;
        if (deletingFrom.numElems + 1 >= treeInformation.maxNumPointers / 2 || deletingFrom.parent == null) {
            return null;
        }

        BTreeStore<KEY, VALUE> parent = deletingFrom.parent;

        int parentIndex = parent.getNext(deletedKey, parent.numElems);
        while (parentIndex < parent.numElems + 1
            && parent.children[parentIndex] != deletingFrom) { //equality check to find where we left off
            parentIndex++;
        }

        //right sibling exists and has enough pointers
        if (parentIndex < parent.numElems
            && ((BTreeStore<KEY, VALUE>) parent.children[parentIndex + 1]).numElems
                >= treeInformation.maxNumPointers / 2) {
            BTreeStore<KEY, VALUE> rightSibling = (BTreeStore<KEY, VALUE>) parent.children[parentIndex + 1];
            deletingFrom.keys[deletingFrom.numElems] = parent.keys[parentIndex];
            deletingFrom.values[deletingFrom.numElems] = parent.values[parentIndex];
            deletingFrom.children[deletingFrom.numElems + 1] = rightSibling.children[0];
            deletingFrom.numElems++;

            parent.keys[parentIndex] = rightSibling.keys[0];
            parent.values[parentIndex] = rightSibling.values[0];

            deleteFromArray(rightSibling.keys, 0);
            deleteFromArray(rightSibling.values, 0);
            deleteFromArray(rightSibling.children, 0);
            rightSibling.numElems--;
            return null;
        }
        //left sibling exists and has enough pointers
        if (parentIndex > 0 &&
            ((BTreeStore<KEY, VALUE>)parent.children[parentIndex - 1]).numElems >= treeInformation.maxNumPointers / 2) {
            BTreeStore<KEY, VALUE> leftSibling = (BTreeStore<KEY, VALUE>) parent.children[parentIndex - 1];
            insertIntoArray(deletingFrom.keys, parent.keys[parentIndex - 1], 0);
            insertIntoArray(deletingFrom.values, parent.values[parentIndex - 1], 0);
            insertIntoArray(deletingFrom.children, leftSibling.children[leftSibling.numElems], 0);
            deletingFrom.numElems++;

            parent.keys[parentIndex - 1] = leftSibling.keys[leftSibling.numElems - 1];
            parent.values[parentIndex - 1] = leftSibling.values[leftSibling.numElems - 1];

            leftSibling.keys[leftSibling.numElems - 1] = null;
            leftSibling.values[leftSibling.numElems - 1] = null;
            leftSibling.children[leftSibling.numElems] = null;
            leftSibling.numElems--;
            return null;
        }
        //neither has enough and we merge with left sibling
        if (parentIndex > 0) {
            deletingFrom = mergeNodes((BTreeStore<KEY, VALUE>) parent.children[parentIndex - 1],
                (KEY) parent.keys[parentIndex - 1], (VALUE) parent.values[parentIndex - 1], deletingFrom);
            if (parent.parent == null && parent.numElems == 1) {
                deletingFrom.parent = null;
                return deletingFrom;
            }
            return deleteUpTheTree(parent, parentIndex - 1);
        }
        //neither has enough and we merge with the right sibling
        deletingFrom = mergeNodes(deletingFrom, (KEY) parent.keys[parentIndex],
            (VALUE) parent.values[parentIndex], (BTreeStore<KEY, VALUE>) parent.children[parentIndex + 1]);
        if (parent.parent == null && parent.numElems == 1) {
            deletingFrom.parent = null;
            return deletingFrom;
        }
        return deleteUpTheTree(parent, parentIndex);
    }

    // merges two close to empty nodes
    private BTreeStore<KEY, VALUE> mergeNodes(BTreeStore<KEY, VALUE> left, KEY separatingKey,
                                              VALUE separatingValue, BTreeStore<KEY, VALUE> right) {
        left.keys[left.numElems] = separatingKey;
        left.values[left.numElems] = separatingValue;

        for (int i = 0; i < right.numElems; i++) {
            left.keys[left.numElems + 1 + i] = right.keys[i];
            left.values[left.numElems + 1 + i] = right.values[i];
            left.children[left.numElems + 1 + i] = right.children[i];
        }

        left.children[left.numElems + 1 + right.numElems] = right.children[right.numElems];
        left.numElems = left.numElems + 1 + right.numElems;
        return left;
    }

    // deletes the element at index from arr
    private <V> void deleteFromArray(V[] arr, int index) {
        for (int i = index; i < arr.length - 1; i++) {
            arr[i] = arr[i + 1];
        }
        arr[arr.length - 1] = null;
    }

    // returns the index of the first elem in elems >= the elem (if includeEqualElems is true)
    // if includeEqualElems is false, then it returns the first elem in elems > the elem in question
    // if no such index exits, returns the number of non-null elements in the array
    protected int getNext(KEY elem, int numElems) {
        int insertionIndex = Arrays.binarySearch((KEY[]) keys, 0, numElems, elem, treeInformation.comparator);
        if (insertionIndex < 0) {
            insertionIndex = -1 * (insertionIndex + 1);
        }
        while (insertionIndex < numElems &&
            insertionIndex - 1 >= 0 &&
            treeInformation.comparator.compare((KEY) keys[insertionIndex - 1], elem) == 0) {
            insertionIndex--;
        }
        return insertionIndex;
    }

    // returns the tree that is the farthest left element in the specified subtree, @param tree
    protected BTreeStore<KEY, VALUE> minimumTree(BTreeStore<KEY, VALUE> tree) {
        BTreeStore<KEY, VALUE> result = tree;
        while (result.children[0] != null) {
            result = (BTreeStore<KEY, VALUE>) result.children[0];
        }
        return result;
    }

    // returns the tree that contains the farthest right (greatest) element in the specified subtree, @param tree
    protected BTreeStore<KEY, VALUE> maximumTree(BTreeStore<KEY, VALUE> tree) {
        BTreeStore<KEY, VALUE> result = tree;
        while (result.children[result.numElems] != null) {
            result = (BTreeStore<KEY, VALUE>) result.children[result.numElems];
        }
        return result;
    }

    // recursive helper to print the tree
    private StringBuilder printTree(BTreeStore<KEY, VALUE> curr, StringBuilder builder) {
        if (curr == null) {
            return builder;
        }
        int elemIndex;
        for (elemIndex = 0; elemIndex < curr.keys.length; elemIndex++) {
            printTree((BTreeStore<KEY, VALUE>) curr.children[elemIndex], builder);
            if (curr.keys[elemIndex] == null) {
                elemIndex++;
                break;
            }
            builder.append(curr.values[elemIndex]);
            builder.append(" ");
        }
        printTree((BTreeStore<KEY, VALUE>) curr.children[elemIndex], builder);
        return builder;
    }

    // class that stores invariant information that is constant for the BTree
    private static class BTreeInformation<KEY, VALUE> {
        private final int maxNumPointers;
        private final Comparator<? super KEY> comparator;
        private final Serializer<KEY> keySerializer;
        private final Serializer<VALUE> valueSerializer;
        private File file;

        public BTreeInformation(int minNumPointers, Comparator<? super KEY> comparator,
                                Serializer<KEY> keySerializer, Serializer<VALUE> valueSerializer, File file) {
            this.maxNumPointers = 2 * minNumPointers;
            this.comparator = comparator;
            this.keySerializer = keySerializer;
            this.valueSerializer = valueSerializer;
            this.file = file;
        }

        public BTreeInformation(int minNumPointers, Comparator<? super KEY> comparator,
                                Serializer<KEY> keySerializer, Serializer<VALUE> valueSerializer, String filename) {
            this(minNumPointers, comparator, keySerializer, valueSerializer, new File(filename));
        }
    }
}

