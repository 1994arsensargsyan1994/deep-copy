package deepy.clone.models;

import java.util.List;
import java.util.Objects;

public class User {
    private final String name;
    private final int age;
    private final List<Book> favoriteBooks;

    public User(final String name, final int age, final List<Book> favoriteBooks) {
        this.name = name;
        this.age = age;
        this.favoriteBooks = favoriteBooks;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public List<Book> getFavoriteBooks() {
        return favoriteBooks;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return age == user.age &&
                Objects.equals(name, user.name)
                && Objects.equals(favoriteBooks, user.favoriteBooks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age, favoriteBooks);
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", favoriteBooks=" + favoriteBooks +
                '}';
    }
}