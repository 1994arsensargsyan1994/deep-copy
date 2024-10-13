package deepy.clone;

import deepy.clone.models.Book;
import deepy.clone.models.User;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {

        final User originalUser = getUser();
        final User copiedUser = CopyUtils.deepCopy(originalUser);
        System.out.println("Original user: " + originalUser);
        System.out.println("Copied user: " + copiedUser);

        // for all test cases refer CopyUtilsTest class
    }

    private static User getUser() {
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
