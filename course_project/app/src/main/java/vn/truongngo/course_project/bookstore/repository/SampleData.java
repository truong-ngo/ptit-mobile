package vn.truongngo.course_project.bookstore.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.truongngo.course_project.bookstore.domain.Book;
import vn.truongngo.course_project.bookstore.domain.BookCondition;
import vn.truongngo.course_project.bookstore.domain.Category;

/**
 * Dữ liệu mẫu để chạy thử, chưa lưu vào DB: sách cần <code>categoryId</code> do SQLite sinh khi lưu thể loại, nên nơi
 * gọi lưu {@link #categories()} trước rồi truyền id vào {@link #books(Map)}.
 *
 * <p>Số sách (23) đủ cho ba trang khi kích thước trang là 10. Có cả sách giá <code>99.000</code>, đúng
 * <code>100.000</code> và <code>120.000</code> tình trạng Tốt, để kiểm tra đúng biên của truy vấn "Tốt, dưới 100k", và
 * một sách chưa phân loại.</p>
 */
public final class SampleData {

    public static final String LITERATURE = "Văn học";
    public static final String SCIENCE = "Khoa học";
    public static final String BIOGRAPHY = "Tiểu sử";

    private SampleData() {
    }

    /** Ba thể loại của đề, chưa có id. */
    public static List<Category> categories() {
        List<Category> categories = new ArrayList<>();
        for (String name : new String[]{LITERATURE, SCIENCE, BIOGRAPHY}) {
            Category category = new Category();
            category.setName(name);
            categories.add(category);
        }
        return categories;
    }

    /**
     * @param categoryIds id đã lưu của mỗi thể loại, khóa là tên thể loại ({@link #LITERATURE}, {@link #SCIENCE},
     *                    {@link #BIOGRAPHY})
     */
    public static List<Book> books(Map<String, Long> categoryIds) {
        Long literature = categoryIds.get(LITERATURE);
        Long science = categoryIds.get(SCIENCE);
        Long biography = categoryIds.get(BIOGRAPHY);

        List<Book> books = new ArrayList<>();

        books.add(book("Dế Mèn phiêu lưu ký", "Tô Hoài", BookCondition.GOOD, 45_000, literature));
        books.add(book("Tắt đèn", "Ngô Tất Tố", BookCondition.FAIR, 30_000, literature));
        books.add(book("Số đỏ", "Vũ Trọng Phụng", BookCondition.GOOD, 62_000, literature));
        books.add(book("Chí Phèo", "Nam Cao", BookCondition.POOR, 18_000, literature));
        books.add(book("Truyện Kiều", "Nguyễn Du", BookCondition.GOOD, 120_000, literature));
        books.add(book("Nhà giả kim", "Paulo Coelho", BookCondition.GOOD, 75_000, literature));
        books.add(book("Mắt biếc", "Nguyễn Nhật Ánh", BookCondition.FAIR, 55_000, literature));
        books.add(book("Cho tôi xin một vé đi tuổi thơ", "Nguyễn Nhật Ánh", BookCondition.GOOD, 68_000, literature));
        books.add(book("Đồi gió hú", "Emily Brontë", BookCondition.FAIR, 85_000, literature));
        books.add(book("Tôi thấy hoa vàng trên cỏ xanh", "Nguyễn Nhật Ánh", BookCondition.GOOD, 99_000, literature));
        books.add(book("Rừng Na Uy", "Haruki Murakami", BookCondition.GOOD, 100_000, literature));

        books.add(book("Lược sử thời gian", "Stephen Hawking", BookCondition.GOOD, 90_000, science));
        books.add(book("Vũ trụ trong vỏ hạt dẻ", "Stephen Hawking", BookCondition.FAIR, 70_000, science));
        books.add(book("Sapiens: Lược sử loài người", "Yuval Noah Harari", BookCondition.GOOD, 150_000, science));
        books.add(book("Nguồn gốc các loài", "Charles Darwin", BookCondition.POOR, 40_000, science));
        books.add(book("Cosmos", "Carl Sagan", BookCondition.FAIR, 95_000, science));
        books.add(book("Tâm lý học đám đông", "Gustave Le Bon", BookCondition.GOOD, 58_000, science));

        books.add(book("Steve Jobs", "Walter Isaacson", BookCondition.GOOD, 135_000, biography));
        books.add(book("Nhật ký trong tù", "Hồ Chí Minh", BookCondition.FAIR, 35_000, biography));
        books.add(book("Hành trình dài đến tự do", "Nelson Mandela", BookCondition.GOOD, 110_000, biography));
        books.add(book("Albert Einstein: Cuộc đời và vũ trụ", "Walter Isaacson", BookCondition.FAIR, 98_000, biography));
        books.add(book("Elon Musk", "Ashlee Vance", BookCondition.POOR, 60_000, biography));

        books.add(book("Đắc nhân tâm", "Dale Carnegie", BookCondition.GOOD, 79_000, null));

        return books;
    }

    /** Dựng một sách chưa có id. */
    private static Book book(String title, String author, BookCondition condition, long price, Long categoryId) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setCondition(condition);
        book.setPrice(price);
        book.setCategoryId(categoryId);
        return book;
    }

//    private void seedIfEmpty() {
//        if (categoryRepository.count(null) > 0) {
//            return;
//        }
//        BookstoreDatabase.getDatabaseHelper(requireContext()).doInTransaction(() -> {
//            Map<String, Long> categoryIds = new HashMap<>();
//            for (Category category : SampleData.categories()) {
//                categoryRepository.insert(category);
//                categoryIds.put(category.getName(), category.getId());
//            }
//
//            for (Book book : SampleData.books(categoryIds)) {
//                bookRepository.insert(book);
//            }
//        });
//    }
}
