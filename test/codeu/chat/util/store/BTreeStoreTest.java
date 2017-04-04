package codeu.chat.util.store;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class BTreeStoreTest {
    @Test
    public void testInsert() {
        BTreeStore<Integer, Integer> test = new BTreeStore<>(2, Integer::compareTo);
        int[] numsToTest = new int[]{
            5, 33, 29, 7, 18, 81, 44, 12, 93, 27, 63, 19, 31, -5, 99
        };
        for (int i = 0; i < numsToTest.length; i++) {
            test = test.insert(numsToTest[i], numsToTest[i], true);
        }
        assertEquals("-5 5 7 12 18 19 27 29 31 33 44 63 81 93 99", test.toString());
    }

    @Test
    public void testDuplicatesNotAllowed() {
        BTreeStore<Integer, Integer> test = new BTreeStore<>(2, Integer::compareTo);
        int[] numsToTest = new int[]{
            5, 33, 29, 7, 18, 81, 44, 12, 93, 27, 63, 19, 31, -5, 99
        };
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < numsToTest.length; i++) {
                test = test.insert(numsToTest[i], numsToTest[i], false);
            }
        }
        assertEquals("-5 5 7 12 18 19 27 29 31 33 44 63 81 93 99", test.toString());
    }

    @Test
    public void testDuplicatesAllowed() {
        BTreeStore<Integer, Integer> test = new BTreeStore<>(2, Integer::compareTo);
        int[] numsToTest = new int[]{
            5, 33, 29, 7, 18, 81, 44, 12, 93, 27, 63, 19, 31, -5, 99
        };
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < numsToTest.length; i++) {
                test = test.insert(numsToTest[i], numsToTest[i], true);
            }
        }
        assertEquals("-5 -5 -5 5 5 5 7 7 7 12 12 12 18 18 18 19 19 19 27 27 27 29 29 29 31 31 31 " +
            "33 33 33 44 44 44 63 63 63 81 81 81 93 93 93 99 99 99", test.toString());
    }

    @Test
    public void testAtNoDuplicates() {
        BTreeStore<Integer, String> test = new BTreeStore<>(2, Integer::compareTo);
        String[] stringsToTest = "abc def ghi jklmn abe mno".split(" ");
        for (int i = 0; i < stringsToTest.length; i++) {
            test = test.insert(i, stringsToTest[i], true);
        }
        for (int i = 0; i < stringsToTest.length; i++) {
            assert(test.at(i).iterator().next() == stringsToTest[i]);
        }
        assertFalse(test.at(-3).iterator().hasNext());
        assertFalse(test.at(6).iterator().hasNext());
    }

    @Test
    public void testAtWithDuplicates() {
        BTreeStore<Integer, String> test = new BTreeStore<>(2, Integer::compareTo);
        String[] stringsToTest = "abc def ghi jklmn abe mno".split(" ");
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < stringsToTest.length; i++) {
                test = test.insert(i, stringsToTest[i], true);
            }
        }
        for (int i = 0; i < stringsToTest.length; i++) {
            Iterator iterator = test.at(i).iterator();
            for (int j = 0; j < 3; j++) {
                assert (iterator.next() == stringsToTest[i]);
            }
            assertFalse(iterator.hasNext());
        }
        assertFalse(test.at(-3).iterator().hasNext());
        assertFalse(test.at(6).iterator().hasNext());
    }

    @Test
    public void testContainsWithBoundsThatExist() {
        BTreeStore<String, String> test = new BTreeStore<>(2, String::compareTo);
        String[] stringsToTest = "abc abe def ghi jklmn mno".split(" ");
        for (int i = 0; i < stringsToTest.length; i++) {
            test = test.insert(stringsToTest[i], stringsToTest[i], false);
        }

        BTreeIterator result = test.range("abc", "mno").iterator();
        for (int i = 0; i < stringsToTest.length; i++) {
            assertTrue(result.hasNext());
            assertEquals(stringsToTest[i], result.next());
        }
        assertFalse(result.hasNext());
    }

    @Test
    public void testContainsWithBoundsThatDontExist() {
        BTreeStore<Integer, String> test = new BTreeStore<>(2, Integer::compareTo);
        String[] stringsToTest = "abc abe def ghi jklmn mno".split(" ");
        for (int i = 0; i < stringsToTest.length; i++) {
            test = test.insert(i, stringsToTest[i], false);
        }

        Iterator result = test.range(-1, 8).iterator();
        for (int i = 0; i < stringsToTest.length; i++) {
            assertTrue(result.hasNext());
            assertEquals(stringsToTest[i], result.next());
        }
        assertFalse(result.hasNext());
    }

    @Test
    public void testContainsWithPartialBounds() {
        BTreeStore<String, String> test = new BTreeStore<>(2, String::compareTo);
        String[] stringsToTest = "abc abe def ghi jklmn mno".split(" ");
        for (int i = 0; i < stringsToTest.length; i++) {
            test = test.insert(stringsToTest[i], stringsToTest[i], false);
        }

        BTreeIterator result = test.range("d", "k").iterator();
        for (int i = 2; i < stringsToTest.length - 1; i++) {
            assertTrue(result.hasNext());
            assertEquals(stringsToTest[i], result.next());
        }
        assertFalse(result.hasNext());
    }

    @Test
    public void testAll() {
        BTreeStore<String, String> test = new BTreeStore<>(2, String::compareTo);
        String[] stringsToTest = "abc abe def ghi jklmn mno".split(" ");
        for (int i = 0; i < stringsToTest.length; i++) {
            test = test.insert(stringsToTest[i], stringsToTest[i], false);
        }

        BTreeIterator result = test.all().iterator();
        for (int i = 0; i < stringsToTest.length; i++) {
            assertTrue(result.hasNext());
            assertEquals(stringsToTest[i], result.next());
        }
        assertFalse(result.hasNext());
    }

    @Test
    public void testAfterWithoutBound() {
        BTreeStore<String, String> test = new BTreeStore<>(2, String::compareTo);
        String[] stringsToTest = "abc abe def ghi jklmn mno".split(" ");
        for (int i = 0; i < stringsToTest.length; i++) {
            test = test.insert(stringsToTest[i], stringsToTest[i], false);
        }

        BTreeIterator result = test.after("d").iterator();
        for (int i = 2; i < stringsToTest.length; i++) {
            assertTrue(result.hasNext());
            assertEquals(stringsToTest[i], result.next());
        }
        assertFalse(result.hasNext());
    }

    @Test
    public void testAfterWithBound() {
        BTreeStore<String, String> test = new BTreeStore<>(2, String::compareTo);
        String[] stringsToTest = "abc abe def def def ghi jklmn mno".split(" ");
        for (int i = 0; i < stringsToTest.length; i++) {
            test = test.insert(stringsToTest[i], stringsToTest[i], true);
        }

        BTreeIterator result = test.after("def").iterator();
        for (int i = 5; i < stringsToTest.length; i++) {
            assertTrue(result.hasNext());
            assertEquals(stringsToTest[i], result.next());
        }
        assertFalse(result.hasNext());
    }

    @Test
    public void testBeforeWithoutBound() {
        BTreeStore<String, String> test = new BTreeStore<>(2, String::compareTo);
        String[] stringsToTest = "abc abe def ghi jklmn mno".split(" ");
        for (int i = 0; i < stringsToTest.length; i++) {
            test = test.insert(stringsToTest[i], stringsToTest[i], true);
        }

        BTreeIterator result = test.before("g").iterator();
        for (int i = 0; i < 3; i++) {
            assertTrue(result.hasNext());
            assertEquals(stringsToTest[i], result.next());
        }
        assertFalse(result.hasNext());
    }

    @Test
    public void testBeforeWithBound() {
        BTreeStore<String, String> test = new BTreeStore<>(2, String::compareTo);
        String[] stringsToTest = "abc abe def def def ghi jklmn mno".split(" ");
        for (int i = 0; i < stringsToTest.length; i++) {
            test = test.insert(stringsToTest[i], stringsToTest[i], true);
        }

        BTreeIterator result = test.before("def").iterator();
        for (int i = 0; i < 2; i++) {
            assertTrue(result.hasNext());
            assertEquals(stringsToTest[i], result.next());
        }
        assertFalse(result.hasNext());
    }

    @Test
    public void testDeleteFromTreeNoDuplicates() {
        BTreeStore<Integer, Integer> test = new BTreeStore<>(2, Integer::compareTo);
        int[] numsToTest = new int[]{
            5, 33, 29, 7, 18, 81, 44, 12, 93, 27, 63, 19, 31, -5, 99
        };
        for (int i = 0; i < numsToTest.length; i++) {
            test = test.insert(numsToTest[i], numsToTest[i], true);
        }
        for (int i = 0; i < numsToTest.length; i++) {
            test = test.delete(numsToTest[i]);
            for (int j = i + 1; j < numsToTest.length; j++) {
                assertTrue(test.at(numsToTest[j]).iterator().hasNext());
            }
            assertFalse(test.at(numsToTest[i]).iterator().hasNext());
        }
    }

    @Test
    public void testAddAndDeleteFromTree() {
        BTreeStore<Integer, Integer> test = new BTreeStore<>(2, Integer::compareTo);
        int[] numsToTest = new int[]{
            5, 33, 29, 7, 18, 81, 44, 12, 93, 27, 63, 19, 31, -5, 99
        };
        for (int i = 0; i < numsToTest.length; i++) {
            test = test.insert(numsToTest[i], numsToTest[i], true);
        }
        for (int i = 8; i < numsToTest.length; i++) {
            test = test.delete(numsToTest[i]);
        }
        for (int i = 0; i < 8; i++) {
            assertTrue(test.at(numsToTest[i]).iterator().hasNext());
        }
        for (int i = 8; i < numsToTest.length; i++) {
            assertFalse(test.at(numsToTest[i]).iterator().hasNext());
        }
        for (int i = 8; i < numsToTest.length; i++) {
            test = test.insert(numsToTest[i], numsToTest[i], true);
        }
        assertEquals("-5 5 7 12 18 19 27 29 31 33 44 63 81 93 99", test.toString());
    }
}

