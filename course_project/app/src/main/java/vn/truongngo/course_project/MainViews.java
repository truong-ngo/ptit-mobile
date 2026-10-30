package vn.truongngo.course_project;

import androidx.fragment.app.FragmentContainerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import vn.truongngo.course_project.libs.util.ViewId;

/** Các view của {@link MainActivity} (<code>activity_main.xml</code>). */
public class MainViews {

    /** Chỗ chứa các Fragment: hai tab và các màn con (form) khi mở chồng lên. */
    @ViewId
    public FragmentContainerView container;

    /** Thanh chọn tab Sách / Thể loại; ẩn khi đang ở màn con. */
    @ViewId
    public BottomNavigationView bottomNav;
}
