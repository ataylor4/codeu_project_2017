package codeu.chat.util.store;

import codeu.chat.util.Serializers;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;

import static org.junit.Assert.*;

public class BTreeStoreTest {
    private static final String FILENAME = "test.log";
    @Test
    public void testInsert() {
        BTreeStore<Integer, Integer> test = new BTreeStore<>(2, Integer::compareTo, Serializers.INTEGER,
            Serializers.INTEGER, FILENAME);
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
        BTreeStore<Integer, Integer> test = new BTreeStore<>(2, Integer::compareTo, Serializers.INTEGER,
            Serializers.INTEGER, FILENAME);
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
        BTreeStore<Integer, Integer> test = new BTreeStore<>(2, Integer::compareTo, Serializers.INTEGER,
            Serializers.INTEGER, FILENAME);
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
        BTreeStore<Integer, String> test = new BTreeStore<>(2, Integer::compareTo,
            Serializers.INTEGER, Serializers.STRING, FILENAME);
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
        BTreeStore<Integer, String> test = new BTreeStore<>(2, Integer::compareTo,
            Serializers.INTEGER, Serializers.STRING, FILENAME);
        String[] stringsToTest = "abc def ghi jklmn abe mno".split(" ");
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < stringsToTest.length; i++) {
                test = test.insert(i, stringsToTest[i], true);
            }
        }
        for (int i = 0; i < stringsToTest.length; i++) {
            BTreeIterator<Integer, String> iterator = test.at(i).iterator();
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
        BTreeStore<String, String> test = new BTreeStore<>(2, String::compareTo,
            Serializers.STRING, Serializers.STRING, FILENAME);
        String[] stringsToTest = "abc abe def ghi jklmn mno".split(" ");
        for (int i = 0; i < stringsToTest.length; i++) {
            test = test.insert(stringsToTest[i], stringsToTest[i], false);
        }

        BTreeIterator<String, String> result = test.range("abc", "mno").iterator();
        for (int i = 0; i < stringsToTest.length; i++) {
            assertTrue(result.hasNext());
            assertEquals(stringsToTest[i], result.next());
        }
        assertFalse(result.hasNext());
    }

    @Test
    public void testContainsWithBoundsThatDontExist() {
        BTreeStore<Integer, String> test = new BTreeStore<>(2, Integer::compareTo,
            Serializers.INTEGER, Serializers.STRING, FILENAME);
        String[] stringsToTest = "abc abe def ghi jklmn mno".split(" ");
        for (int i = 0; i < stringsToTest.length; i++) {
            test = test.insert(i, stringsToTest[i], false);
        }

        BTreeIterator<Integer, String> result = test.range(-1, 8).iterator();
        for (int i = 0; i < stringsToTest.length; i++) {
            assertTrue(result.hasNext());
            assertEquals(stringsToTest[i], result.next());
        }
        assertFalse(result.hasNext());
    }

    @Test
    public void testContainsWithPartialBounds() {
        BTreeStore<String, String> test = new BTreeStore<>(2, String::compareTo,
            Serializers.STRING, Serializers.STRING, FILENAME);
        String[] stringsToTest = "abc abe def ghi jklmn mno".split(" ");
        for (int i = 0; i < stringsToTest.length; i++) {
            test = test.insert(stringsToTest[i], stringsToTest[i], false);
        }

        BTreeIterator<String, String> result = test.range("d", "k").iterator();
        for (int i = 2; i < stringsToTest.length - 1; i++) {
            assertTrue(result.hasNext());
            assertEquals(stringsToTest[i], result.next());
        }
        assertFalse(result.hasNext());
    }

    @Test
    public void testAll() {
        BTreeStore<String, String> test = new BTreeStore<>(2, String::compareTo,
            Serializers.STRING, Serializers.STRING, FILENAME);
        String[] stringsToTest = "abc abe def ghi jklmn mno".split(" ");
        for (int i = 0; i < stringsToTest.length; i++) {
            test = test.insert(stringsToTest[i], stringsToTest[i], false);
        }

        BTreeIterator<String, String> result = test.all().iterator();
        for (int i = 0; i < stringsToTest.length; i++) {
            assertTrue(result.hasNext());
            assertEquals(stringsToTest[i], result.next());
        }
        assertFalse(result.hasNext());
    }

    @Test
    public void testAfterWithoutBound() {
        BTreeStore<String, String> test = new BTreeStore<>(2, String::compareTo,
            Serializers.STRING, Serializers.STRING, FILENAME);
        String[] stringsToTest = "abc abe def ghi jklmn mno".split(" ");
        for (int i = 0; i < stringsToTest.length; i++) {
            test = test.insert(stringsToTest[i], stringsToTest[i], false);
        }

        BTreeIterator<String, String> result = test.after("d").iterator();
        for (int i = 2; i < stringsToTest.length; i++) {
            assertTrue(result.hasNext());
            assertEquals(stringsToTest[i], result.next());
        }
        assertFalse(result.hasNext());
    }

    @Test
    public void testAfterWithBound() {
        BTreeStore<String, String> test = new BTreeStore<>(2, String::compareTo,
            Serializers.STRING, Serializers.STRING, FILENAME);
        String[] stringsToTest = "abc abe def def def ghi jklmn mno".split(" ");
        for (int i = 0; i < stringsToTest.length; i++) {
            test = test.insert(stringsToTest[i], stringsToTest[i], true);
        }

        BTreeIterator<String, String> result = test.after("def").iterator();
        for (int i = 5; i < stringsToTest.length; i++) {
            assertTrue(result.hasNext());
            assertEquals(stringsToTest[i], result.next());
        }
        assertFalse(result.hasNext());
    }

    @Test
    public void testBeforeWithoutBound() {
        BTreeStore<String, String> test = new BTreeStore<>(2, String::compareTo,
            Serializers.STRING, Serializers.STRING, FILENAME);
        String[] stringsToTest = "abc abe def ghi jklmn mno".split(" ");
        for (int i = 0; i < stringsToTest.length; i++) {
            test = test.insert(stringsToTest[i], stringsToTest[i], true);
        }

        BTreeIterator<String, String> result = test.before("g").iterator();
        for (int i = 0; i < 3; i++) {
            assertTrue(result.hasNext());
            assertEquals(stringsToTest[i], result.next());
        }
        assertFalse(result.hasNext());
    }

    @Test
    public void testBeforeWithBound() {
        BTreeStore<String, String> test = new BTreeStore<>(2, String::compareTo,
            Serializers.STRING, Serializers.STRING, FILENAME);
        String[] stringsToTest = "abc abe def def def ghi jklmn mno".split(" ");
        for (int i = 0; i < stringsToTest.length; i++) {
            test = test.insert(stringsToTest[i], stringsToTest[i], true);
        }

        BTreeIterator<String, String> result = test.before("def").iterator();
        for (int i = 0; i < 2; i++) {
            assertTrue(result.hasNext());
            assertEquals(stringsToTest[i], result.next());
        }
        assertFalse(result.hasNext());
    }

    @Test
    public void testDeleteFromTreeNoDuplicates() {
        BTreeStore<Integer, Integer> test = new BTreeStore<>(2, Integer::compareTo, Serializers.INTEGER,
            Serializers.INTEGER, FILENAME);
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
        BTreeStore<Integer, Integer> test = new BTreeStore<>(2, Integer::compareTo, Serializers.INTEGER,
            Serializers.INTEGER, FILENAME);
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

    @Test
    public void testAllEmpty() {
        BTreeStore<Integer, Integer> test = new BTreeStore<>(2, Integer::compareTo, Serializers.INTEGER,
            Serializers.INTEGER, FILENAME);
        assertFalse(test.all().iterator().hasNext());
    }

    @Test
    public void testLoadingFromFileSimple() {
        BTreeStore<Integer, String> test = new BTreeStore<>(2, Integer::compareTo, Serializers.INTEGER,
            Serializers.STRING, FILENAME);
        test = test.insert(3, "Ashley", false);
        test = test.insert(18, "Pod", false);
        test = test.insert(7, "Ryan", false);
        test = test.delete(7);

        BTreeStore<Integer, String> copy = new BTreeStore<>(2, Integer::compareTo, Serializers.INTEGER,
            Serializers.STRING, FILENAME);
        assertEquals("Ashley Pod", copy.toString());
    }

    @Test
    public void testLoadingFromFile() {
        BTreeStore<Integer, Integer> test = new BTreeStore<>(2, Integer::compareTo, Serializers.INTEGER,
            Serializers.INTEGER, FILENAME);
        int[] numsToTest = new int[]{
            5, 33, 29, 7, 18, 81, 44, 12, 93, 27, 63, 19, 31, -5, 99
        };
        for (int i = 0; i < numsToTest.length; i++) {
            test = test.insert(numsToTest[i], numsToTest[i], true);
        }
        for (int i = 8; i < numsToTest.length; i++) {
            test = test.delete(numsToTest[i]);
        }
        for (int i = 8; i < numsToTest.length; i++) {
            test = test.insert(numsToTest[i], numsToTest[i], true);
        }

        BTreeStore<Integer, Integer> copy = new BTreeStore<>(2, Integer::compareTo, Serializers.INTEGER,
            Serializers.INTEGER, FILENAME);
        assertEquals("-5 5 7 12 18 19 27 29 31 33 44 63 81 93 99", copy.toString());
    }

    @Test
    public void testUpdate() {
        BTreeStore<Integer, Integer> test = new BTreeStore<>(2, Integer::compareTo, Serializers.INTEGER,
            Serializers.INTEGER, FILENAME);
        int[] numsToTest = new int[]{
            5, 33, 29, 7, 18, 81, 44, 12, 93, 27, 63, 19, 31, -5, 99
        };
        for (int i = 0; i < numsToTest.length; i++) {
            test = test.insert(numsToTest[i], numsToTest[i], true);
        }
        for (int i = 8; i < numsToTest.length; i++) {
            test = test.delete(numsToTest[i]);
        }
        for (int i = 8; i < numsToTest.length; i++) {
            test = test.insert(numsToTest[i], numsToTest[i], true);
        }

        for (int i = 0; i < numsToTest.length; i++) {
            test.update(numsToTest[i], numsToTest[i] + 1);
        }

        BTreeStore<Integer, Integer> copy = new BTreeStore<>(2, Integer::compareTo, Serializers.INTEGER,
            Serializers.INTEGER, FILENAME);
        assertEquals("-4 6 8 13 19 20 28 30 32 34 45 64 82 94 100", copy.toString());
    }

    @Test
    public void badUpdate() {
        BTreeStore<Integer, Integer> test = new BTreeStore<>(2, Integer::compareTo, Serializers.INTEGER,
            Serializers.INTEGER, FILENAME);
        int[] numsToTest = new int[]{
            5, 33, 29, 7, 18, 81, 44, 12, 93, 27, 63, 19, 31, -5, 99
        };
        for (int i = 0; i < numsToTest.length; i++) {
            test = test.insert(numsToTest[i], numsToTest[i], true);
        }
        assertFalse(test.update(301, 12));
    }


    @After
    public void cleanup() {
        File file = new File(FILENAME);
        file.delete();
    }
}

