package deep.clone;

import deepy.clone.CopyUtils;
import deepy.clone.models.Book;
import deepy.clone.models.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class CopyUtilsTest {

    @Test
    void testDeepCopyList() {
        ArrayList<String> originalList = new ArrayList<>();
        originalList.add("Hello");
        originalList.add("World");

        ArrayList<String> copiedList = CopyUtils.deepCopy(originalList);

        Assertions.assertNotSame(originalList, copiedList, "The copied list should not be the same instance as the original.");
        Assertions.assertEquals(originalList, copiedList, "The copied list should be equal to the original.");
    }

    @Test
    void testDeepCopySet() {
        HashSet<Integer> originalSet = new HashSet<>();
        originalSet.add(1);
        originalSet.add(2);
        originalSet.add(3);

        HashSet<Integer> copiedSet = CopyUtils.deepCopy(originalSet);

        Assertions.assertNotSame(originalSet, copiedSet, "The copied set should not be the same instance as the original.");
        Assertions.assertEquals(originalSet, copiedSet, "The copied set should be equal to the original.");
    }

    @Test
    void testDeepCopyMap() {
        HashMap<String, String> originalMap = new HashMap<>();
        originalMap.put("key1", "value1");
        originalMap.put("key2", "value2");

        HashMap<String, String> copiedMap = CopyUtils.deepCopy(originalMap);

        Assertions.assertNotSame(originalMap, copiedMap, "The copied map should not be the same instance as the original.");
        Assertions.assertEquals(originalMap, copiedMap, "The copied map should be equal to the original.");
    }

    @Test
    void testDeepCopyUser() {
        User originalUser = getUser();

        User copiedUser = CopyUtils.deepCopy(originalUser);

        Assertions.assertNotSame(originalUser, copiedUser, "The copied user should not be the same instance as the original.");
        Assertions.assertEquals(originalUser, copiedUser, "The copied user should be equal to the original.");
        Assertions.assertNotSame(originalUser.getFavoriteBooks(), copiedUser.getFavoriteBooks(), "The copied user's book list should not be the same instance as the original.");
    }

    @Test
    void testDeepCopyImmutableString() {
        String immutableString = "This is immutable.";
        String copiedString = CopyUtils.deepCopy(immutableString);

        Assertions.assertSame(immutableString, copiedString, "The copied string should be the same instance as the original since it's immutable.");
    }

    private User getUser() {
        ArrayList<Book> books = new ArrayList<>();
        Book bookNeron = new Book();
        bookNeron.setTitle("Neron");
        Book bookMaster = new Book();
        bookMaster.setTitle("Master and Margarita");

        books.add(bookNeron);
        books.add(bookMaster);

        return new User("Denzel Washington ", 68, books);
    }

}
