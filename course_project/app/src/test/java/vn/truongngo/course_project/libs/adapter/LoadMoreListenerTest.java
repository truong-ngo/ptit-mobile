package vn.truongngo.course_project.libs.adapter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Kiểm thử hàm quyết định của {@link LoadMoreListener} trên JVM. Phần gắn vào RecyclerView và gọi
 * {@code onLoadMore} cần thiết bị nên chưa được kiểm thử ở đây.
 */
public class LoadMoreListenerTest {

    @Test
    public void loadsWhenWithinThresholdOfTheEnd() {
        // 20 dòng (0..19), threshold 3: từ dòng 16 trở đi thì tải
        assertFalse(LoadMoreListener.shouldLoad(15, 20, 3, false, true));
        assertTrue(LoadMoreListener.shouldLoad(16, 20, 3, false, true));
        assertTrue(LoadMoreListener.shouldLoad(19, 20, 3, false, true));
    }

    @Test
    public void thresholdZeroOnlyLoadsWhenLastItemIsVisible() {
        assertFalse(LoadMoreListener.shouldLoad(18, 20, 0, false, true));
        assertTrue(LoadMoreListener.shouldLoad(19, 20, 0, false, true));
    }

    @Test
    public void doesNotLoadWhileLoadingOrWhenNoMorePages() {
        assertFalse(LoadMoreListener.shouldLoad(19, 20, 3, true, true));
        assertFalse(LoadMoreListener.shouldLoad(19, 20, 3, false, false));
    }

    @Test
    public void doesNotLoadWithoutItems() {
        assertFalse(LoadMoreListener.shouldLoad(-1, 0, 3, false, true));
        assertFalse(LoadMoreListener.shouldLoad(-1, 20, 3, false, true));
    }

    @Test
    public void shortListThatFitsOnScreenLoadsImmediately() {
        // 5 dòng, màn hình hiện cả 5 (dòng cuối là 4): không có gì để cuộn nên phải tải ngay
        assertTrue(LoadMoreListener.shouldLoad(4, 5, 3, false, true));
    }
}
