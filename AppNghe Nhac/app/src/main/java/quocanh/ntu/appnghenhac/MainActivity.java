package quocanh.ntu.appnghenhac;

import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    // Khai báo các thành phần giao diện (UI Components)
    RecyclerView recyclerViewSongs;      // Danh sách Popular Songs
    RecyclerView recyclerViewNewSongs;   // Danh sách New Songs
    RecyclerView recyclerViewRecent;     // Danh sách Nghe gần đây
    RecyclerView recyclerViewFavorite;   // Danh sách Bài hát yêu thích
    ViewPager2 viewPagerBanner;          // Slider Banner ảnh trượt đầu trang

    TextView txtRecentHeader;
    TextView txtFavoriteHeader;
    EditText edtSearch;

    // Khai báo các mảng chứa dữ liệu bài hát
    ArrayList<Song> songList = new ArrayList<>();
    ArrayList<Song> filteredPopularList = new ArrayList<>();
    ArrayList<Song> filteredNewList = new ArrayList<>();
    ArrayList<Song> recentList = new ArrayList<>();
    ArrayList<Song> favoriteList = new ArrayList<>();

    // Khai báo các bộ điều phối hiển thị (Adapters)
    SongAdapter popularAdapter;
    SongAdapter newSongsAdapter;
    SongAdapter recentAdapter;
    SongAdapter favoriteAdapter;
    BannerAdapter bannerAdapter;

    FirebaseDatabase database;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Ánh xạ các View từ Layout XML sang Code Java
        recyclerViewSongs = findViewById(R.id.recyclerViewSongs);
        recyclerViewNewSongs = findViewById(R.id.recyclerViewNewSongs);
        recyclerViewRecent = findViewById(R.id.recyclerViewRecent);
        recyclerViewFavorite = findViewById(R.id.recyclerViewFavorite);
        viewPagerBanner = findViewById(R.id.viewPagerBanner);
        txtRecentHeader = findViewById(R.id.txtRecentHeader);
        txtFavoriteHeader = findViewById(R.id.txtFavoriteHeader);
        edtSearch = findViewById(R.id.edtSearch);

        // 2. Cấu hình hiển thị Lưới ô vuông 2 CỘT cho Popular Songs
        recyclerViewSongs.setLayoutManager(new GridLayoutManager(this, 2));
        popularAdapter = new SongAdapter(this, filteredPopularList);
        recyclerViewSongs.setAdapter(popularAdapter);

        // 3. Cấu hình hiển thị Lưới ô vuông 2 CỘT cho New Songs
        recyclerViewNewSongs.setLayoutManager(new GridLayoutManager(this, 2));
        newSongsAdapter = new SongAdapter(this, filteredNewList);
        recyclerViewNewSongs.setAdapter(newSongsAdapter);

        // 4. Cấu hình hiển thị Cuộn ngang cho danh sách Nghe gần đây
        recyclerViewRecent.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recentAdapter = new SongAdapter(this, recentList, true);
        recyclerViewRecent.setAdapter(recentAdapter);
        txtRecentHeader.setVisibility(View.GONE);
        recyclerViewRecent.setVisibility(View.GONE);

        // 5. Cấu hình hiển thị Cuộn ngang cho danh sách Bài hát yêu thích
        recyclerViewFavorite.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        favoriteAdapter = new SongAdapter(this, favoriteList, true);
        recyclerViewFavorite.setAdapter(favoriteAdapter);
        txtFavoriteHeader.setVisibility(View.GONE);
        recyclerViewFavorite.setVisibility(View.GONE);

        // 6. Khởi tạo Slider Banner chuyển động ở đầu trang
        bannerAdapter = new BannerAdapter(this, songList);
        viewPagerBanner.setAdapter(bannerAdapter);

        // 7. Cấu hình kết nối tới Firebase Realtime Database
        database = FirebaseDatabase.getInstance("https://appnghenhac-8e5b8-default-rtdb.firebaseio.com/");
        reference = database.getReference("Songs");

        // 8. Đồng bộ và xử lý dữ liệu thời gian thực từ Cloud Firebase xuống thiết bị
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                songList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Song song = dataSnapshot.getValue(Song.class);
                    if (song != null) {
                        // FIX LỖI: Lấy mã nút khóa cha (song4, song7...) gán trực tiếp làm ID cho bài hát
                        song.setId(dataSnapshot.getKey());
                        songList.add(song);
                    }
                }

                // Sắp xếp thứ tự danh sách bài hát theo bảng chữ cái ABC danh mục Title
                Collections.sort(songList, new Comparator<Song>() {
                    @Override
                    public int compare(Song s1, Song s2) {
                        String t1 = s1.getTitle() != null ? s1.getTitle() : "";
                        String t2 = s2.getTitle() != null ? s2.getTitle() : "";
                        return t1.compareToIgnoreCase(t2);
                    }
                });

                // Cập nhật làm mới giao diện thanh trượt ảnh Banner đầu trang
                bannerAdapter.notifyDataSetChanged();

                // Phân tách danh sách bài hát và kích hoạt bộ lọc tìm kiếm
                filter(edtSearch.getText().toString());

                // 9. Đồng bộ danh sách Thả tim (Yêu thích) dựa trên mã nhận diện thiết bị ANDROID_ID
                String userId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                DatabaseReference favRef = database.getReference("Users").child(userId).child("favorites");

                favRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot favSnapshot) {
                        favoriteList.clear();

                        for (DataSnapshot ds : favSnapshot.getChildren()) {
                            String favSongId = ds.getKey(); // Nhận mã key bài hát đã yêu thích (Ví dụ: "song18")

                            for (Song song : songList) {
                                // So khớp chính xác ID để đưa bài hát tương ứng vào mục yêu thích
                                if (song.getId() != null && song.getId().equals(favSongId)) {
                                    favoriteList.add(song);
                                }
                            }
                        }

                        // Điều khiển tự động ẩn/hiện Layout yêu thích dựa trên dữ liệu thật
                        if (favoriteList.isEmpty()) {
                            txtFavoriteHeader.setVisibility(View.GONE);
                            recyclerViewFavorite.setVisibility(View.GONE);
                        } else {
                            txtFavoriteHeader.setVisibility(View.VISIBLE);
                            recyclerViewFavorite.setVisibility(View.VISIBLE);
                        }

                        // Thông báo cập nhật danh sách yêu thích cuộn ngang
                        favoriteAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 10. Lắng nghe sự kiện người dùng nhập văn bản vào ô tìm kiếm (Search Bar)
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // Hàm chuyển đổi chuỗi chữ có dấu Tiếng Việt thành chữ không dấu viết thường
    private String removeAccent(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replaceAll("Đ", "D").replaceAll("đ", "d");
    }

    // Hàm xử lý logic tìm kiếm thông minh và phân phối dữ liệu đều vào 2 lưới hiển thị
    private void filter(String text) {
        filteredPopularList.clear();
        filteredNewList.clear();

        ArrayList<Song> temporaryList = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            temporaryList.addAll(songList);
        } else {
            String query = removeAccent(text.toLowerCase().trim());
            for (Song song : songList) {
                String title = song.getTitle() != null ? song.getTitle() : "";
                String artist = song.getArtist() != null ? song.getArtist() : "";

                if (removeAccent(title.toLowerCase()).contains(query) ||
                        removeAccent(artist.toLowerCase()).contains(query)) {
                    temporaryList.add(song);
                }
            }
        }

        // Thực hiện chia đôi mảng dữ liệu tìm được để đẩy đều sang hai ô lưới song song
        int halfSize = temporaryList.size() / 2;
        for (int i = 0; i < temporaryList.size(); i++) {
            if (i < halfSize) {
                filteredPopularList.add(temporaryList.get(i));
            } else {
                filteredNewList.add(temporaryList.get(i));
            }
        }

        // Cập nhật làm mới hiển thị tại cả 2 vùng hiển thị lưới ô vuông chính
        popularAdapter.notifyDataSetChanged();
        newSongsAdapter.notifyDataSetChanged();
    }

    // Hàm lưu vết danh sách lịch sử nghe gần đây (Mức trần lưu giữ tối đa 5 bài hát mới nhất)
    public void addToRecent(Song song) {
        if (song == null) return;
        for (int i = 0; i < recentList.size(); i++) {
            if (recentList.get(i).getTitle() != null && recentList.get(i).getTitle().equals(song.getTitle())) {
                recentList.remove(i);
                break;
            }
        }
        recentList.add(0, song);
        if (recentList.size() > 5) {
            recentList.remove(recentList.size() - 1);
        }
        txtRecentHeader.setVisibility(View.VISIBLE);
        recyclerViewRecent.setVisibility(View.VISIBLE);
        recentAdapter.notifyDataSetChanged();
    }
}