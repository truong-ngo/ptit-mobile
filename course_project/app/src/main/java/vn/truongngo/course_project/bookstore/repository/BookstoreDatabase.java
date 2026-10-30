package vn.truongngo.course_project.bookstore.repository;

import android.content.Context;

import vn.truongngo.course_project.bookstore.domain.Book;
import vn.truongngo.course_project.bookstore.domain.Category;
import vn.truongngo.course_project.libs.repository.BaseDbHelper;

public class BookstoreDatabase {
    public static final String DATABASE_NAME = "bookstore.db";
    private static BaseDbHelper helper;
    private static BookRepository bookRepository;
    private static CategoryRepository categoryRepository;

    private BookstoreDatabase() {}

    /** Repository không giữ state đổi được nên dùng chung một thể hiện cho cả tiến trình. */
    public static synchronized BookRepository bookRepository(Context context) {
        if (bookRepository == null) {
            bookRepository = new BookRepository(getDatabaseHelper(context), Book.class);
        }
        return bookRepository;
    }

    public static synchronized CategoryRepository categoryRepository(Context context) {
        if (categoryRepository == null) {
            categoryRepository = new CategoryRepository(getDatabaseHelper(context), Category.class);
        }
        return categoryRepository;
    }

    public static synchronized BaseDbHelper getDatabaseHelper(Context context) {
        if (helper == null) {
            helper = new BaseDbHelper(context, DATABASE_NAME, Category.class, Book.class);
        }
        return helper;
    }
}
