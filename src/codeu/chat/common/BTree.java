// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package codeu.chat.common;

import java.util.Comparator;

/**
 * Created by ataylor on 3/12/17.
 */
@SuppressWarnings("unchecked")
public class BTree<T> {
    private BTree<T> parent;
    private BTree<T>[] children;
    private Object[] elems;
    private BTreeInformation<T> treeInformation;

    /**
     * Constructor for a B-Tree
     * @param b: The maximum amount of pointers any node of the tree can have.
     * @param comparator: The compare function that should be use to order elements
     *                  within the tree. See
     *                  https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html.
     */
    public BTree(int b, Comparator<? super T> comparator) {
        this(new BTreeInformation<>(b, comparator));
        if (b < 2) {
            throw new IllegalArgumentException(
                "Must have a minimimum of two pointers per node (when full)");
        }
    }

    private BTree(BTreeInformation<T> treeInformation) {
        this.treeInformation = treeInformation;
        parent = null;

        children = new BTree[treeInformation.maxNumPointers];
        elems = new Object[treeInformation.maxNumPointers - 1];
    }

    /**
     * Search the tree for the given element. Returns the element if found.
     * @param elem: The element to search for.
     *            Note that only the field(s) used by the comparator must be initialized
     * @return the element that is sought after, or null if it is not within the tree
     */
    public T contains(T elem) {
        BTree<T> curr = this;
        while (curr != null) {
            int pointerIndex = getNext((T[]) curr.elems, elem);
            if (pointerIndex < curr.elems.length &&
                curr.elems[pointerIndex] != null &&
                treeInformation.comparator.compare(elem, (T) curr.elems[pointerIndex]) == 0) {
              return (T) curr.elems[pointerIndex];
            }
            curr = curr.children[pointerIndex];
        }
        return null;
    }

    /**
     * Inserts the given element into the tree, assuming it is not a duplicate.
     * @param elem: The element to add into the tree
     * @return The root of the tree after the insertion.
     */
    public BTree<T> add(T elem) {
        BTree<T> curr = this;
        while (true) {
            int index = getNext((T[]) curr.elems, elem);
            if (index < elems.length && elem.equals(curr.elems[index])) {
                return this;
            }
            if (curr.children[index] == null) {
                //first call, prev's value doesn't matter
                BTree<T> newRoot = insertIntoNode(curr, elem, null, curr);
                return newRoot == null ? this : newRoot;
            }
            curr = curr.children[index];
        }
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
        return builder.deleteCharAt(builder.length() - 1).toString();
    }

    private BTree<T> insertIntoNode(BTree<T> toInsert, T elem, BTree<T> child, BTree<T> prev) {
        if (toInsert == null) {
            return editRoot(prev, child, elem);
        }
        int indexToInsert = getNext((T[]) toInsert.elems, elem);
        if (toInsert.elems[toInsert.elems.length - 1] == null) {
            insertIntoArray(toInsert.elems, elem, indexToInsert);
            insertIntoArray(toInsert.children, child, indexToInsert + 1);
        } else {
            int centerIndex = (treeInformation.maxNumPointers + 1) / 2;;
            if (centerIndex > indexToInsert) {
                // account for the case of inserting into the left child
                centerIndex--;
            }

            T center = (T) toInsert.elems[centerIndex];
            BTree<T> newPointer = new BTree<>(treeInformation);
            newPointer.parent = toInsert.parent;

            for (int i = centerIndex + 1; i < elems.length; i++) {
                newPointer.elems[i - (centerIndex + 1)] = toInsert.elems[i];
                toInsert.elems[i] = null;
            }
            toInsert.elems[centerIndex] = null;

            for (int i = centerIndex + 1; i < toInsert.children.length; i++) {
                newPointer.children[i - (centerIndex + 1)] = toInsert.children[i];
                toInsert.children[i] = null;
            }

            if (centerIndex == indexToInsert) {
                insertIntoArray(newPointer.elems, center, 0);
                center = elem;
                insertIntoArray(newPointer.children, child, 0);
            } else {
                if ((treeInformation.maxNumPointers + 1) / 2 >= indexToInsert) {
                    insertIntoArray(toInsert.elems, elem, indexToInsert);
                    insertIntoArray(toInsert.children, child, indexToInsert + 1);
                } else {
                    insertIntoArray(newPointer.elems, elem, indexToInsert - (centerIndex + 1));
                    insertIntoArray(newPointer.children, child, indexToInsert - centerIndex);
                }
            }
            return insertIntoNode(toInsert.parent, center, newPointer, toInsert);
        }
        return null;
    }

    // handles the case of splitting the root node and creating a new root
    private BTree<T> editRoot(BTree<T> childOne, BTree<T> childTwo, T elem) {
        BTree<T> newRoot = new BTree<>(treeInformation);
        newRoot.elems[0] = elem;
        newRoot.children[0] = childOne;
        childOne.parent = newRoot;
        newRoot.children[1] = childTwo;
        childTwo.parent = newRoot;
        return newRoot;
    }

    // index is the index of the element immediately after elem
    private <V> void insertIntoArray(V[] arr, V elem, int index) {
        for (int i = arr.length - 2; i >= index; i--) {
            arr[i + 1] = arr[i];
        }
        arr[index] = elem;
    }

    // returns the index of the first elem in elems >= the elem
    // if no such index exits, returns the number of non-null elements in the array
    private int getNext(T[] elems, T elem) {
        for (int i = 0; i < elems.length; i++) {
            if (elems[i] == null) {
                // ran out of children to compare to
                return i;
            }
            if (treeInformation.comparator.compare(elem, elems[i]) <= 0) {
                // elem comes before the next pointer
                return i;
            }
        }
        return elems.length;
    }

    private StringBuilder printTree(BTree<T> curr, StringBuilder builder) {
        if (curr == null) {
            return builder;
        }
        int elemIndex;
        for (elemIndex = 0; elemIndex < curr.elems.length; elemIndex++) {
            printTree(curr.children[elemIndex], builder);
            if (curr.elems[elemIndex] == null) {
                elemIndex++;
                break;
            }
            builder.append(curr.elems[elemIndex]);
            builder.append(" ");
        }
        printTree(curr.children[elemIndex], builder);
        return builder;
    }

    private static class BTreeInformation<T> {
        private final int maxNumPointers;
        private final Comparator<? super T> comparator;

        public BTreeInformation(int maxNumPointers, Comparator<? super T> comparator) {
            this.maxNumPointers = maxNumPointers;
            this.comparator = comparator;
        }

    }
}
