package quocanh.ntu.appnghenhac;

import android.animation.ObjectAnimator;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class MusicPlayerActivity extends AppCompatActivity {
    ImageView imgMusic, imgBackground;
    TextView txtTitle,
            txtArtist,
            txtLyrics,
            txtCurrentTime,
            txtTotalTime;
    ImageButton btnPlay,
            btnNext,
            btnPrev;
    SeekBar seekBar;
    MediaPlayer mediaPlayer;
    Handler handler = new Handler();
    Runnable runnable;
    ObjectAnimator rotationAnimator;
    // DANH SÁCH NHẠC
    ArrayList<Song> songList;
    // VỊ TRÍ BÀI HÁT
    int position = 0;
    // BÀI HIỆN TẠI
    Song currentSong;
    private boolean isPlayerReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);
        // IMAGE
        imgMusic = findViewById(R.id.imgMusic);
        imgBackground = findViewById(R.id.imgBackground);
        // TEXT
        txtTitle = findViewById(R.id.txtMusicTitle);
        txtArtist = findViewById(R.id.txtMusicArtist);
        txtLyrics = findViewById(R.id.txtLyrics);
        txtCurrentTime = findViewById(R.id.txtCurrentTime);
        txtTotalTime = findViewById(R.id.txtTotalTime);
        // BUTTON
        btnPlay = findViewById(R.id.btnPlay);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        // SEEK BAR
        seekBar = findViewById(R.id.seekBar);

        // NHẬN DANH SÁCH NHẠC
        songList = (ArrayList<Song>) getIntent().getSerializableExtra("songList");
        // NHẬN POSITION
        position = getIntent().getIntExtra("position", 0);

        if (songList != null && !songList.isEmpty()) {
            // BÀI HÁT HIỆN TẠI
            currentSong = songList.get(position);
            // LOAD SONG
            loadSong();
        } else {
            Toast.makeText(this, "Danh sách nhạc trống!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // PLAY / PAUSE
        btnPlay.setOnClickListener(v -> {
            if (mediaPlayer == null || !isPlayerReady) return;

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btnPlay.setImageResource(android.R.drawable.ic_media_play);
                // DỪNG XOAY
                if (rotationAnimator != null) {
                    rotationAnimator.pause();
                }
            } else {
                mediaPlayer.start();
                btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                // XOAY TIẾP
                if (rotationAnimator != null) {
                    rotationAnimator.resume();
                }
                // GỌI HÀM KÍCH HOẠT LẠI VÒNG LẶP SEEKBAR
                startSeekBarUpdate();
            }
        });

        // SEEK BAR
        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser && mediaPlayer != null && isPlayerReady) {
                            mediaPlayer.seekTo(progress);
                            txtCurrentTime.setText(formatTime(progress));
                        }
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });

        // NEXT
        btnNext.setOnClickListener(v -> {
            if (songList == null || songList.isEmpty()) return;
            position++;
            // QUAY VỀ ĐẦU
            if (position >= songList.size()) {
                position = 0;
            }
            currentSong = songList.get(position);
            loadSong();
        });

        // PREVIOUS
        btnPrev.setOnClickListener(v -> {
            if (songList == null || songList.isEmpty()) return;
            position--;
            // QUAY VỀ CUỐI
            if (position < 0) {
                position = songList.size() - 1;
            }
            currentSong = songList.get(position);
            loadSong();
        });
    }

    // LOAD SONG
    private void loadSong() {
        isPlayerReady = false;
        btnPlay.setImageResource(android.R.drawable.ic_media_play);
        // DỪNG HANDLER CŨ
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        // DỪNG XOAY CŨ
        if (rotationAnimator != null) {
            rotationAnimator.cancel();
        }
        imgMusic.setRotation(0f);
        // GIẢI PHÓNG PLAYER CŨ
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        // THÔNG TIN BÀI HÁT
        txtTitle.setText(currentSong.getTitle());
        txtArtist.setText(currentSong.getArtist());

        if (currentSong.getLyrics() != null) {
            txtLyrics.setText(currentSong.getLyrics());
        } else {
            txtLyrics.setText("Lời bài hát đang được cập nhật...");
        }

        txtCurrentTime.setText("0:00");
        txtTotalTime.setText("0:00");
        seekBar.setProgress(0);

        // FADE EFFECT
        imgMusic.setAlpha(0f);
        imgMusic.animate().alpha(1f).setDuration(800);

        // LOAD ẢNH CẮT THEO HÌNH TRÒN
        Glide.with(this)
                .load(currentSong.getImageURL())
                .apply(RequestOptions.circleCropTransform())
                .into(imgMusic);

        // LOAD BACKGROUND
        Glide.with(this)
                .load(currentSong.getImageURL())
                .into(imgBackground);

        // XOAY ĐĨA NHẠC
        rotationAnimator = ObjectAnimator.ofFloat(imgMusic, "rotation", 0f, 360f);
        rotationAnimator.setDuration(12000); // 12 giây xoay 1 vòng cho mượt mắt
        rotationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        rotationAnimator.setInterpolator(new LinearInterpolator());

        // RESET GÓC XOAY
        imgMusic.setRotation(0f);

        // TẠO MEDIA PLAYER
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );
            mediaPlayer.setDataSource(currentSong.getSongURL());
            // CHUẨN BỊ XONG
            mediaPlayer.setOnPreparedListener(mp -> {
                isPlayerReady = true;
                mp.start();
                // KÍCH HOẠT XOAY ĐĨA NHẠC KHI HÁT
                rotationAnimator.start();
                btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                // THỜI GIAN TỔNG
                seekBar.setMax(mp.getDuration());
                txtTotalTime.setText(formatTime(mp.getDuration()));
                // Bắt đầu cập nhật SeekBar
                startSeekBarUpdate();
            });
            // KHI HẾT NHẠC
            mediaPlayer.setOnCompletionListener(mp -> btnNext.performClick());

            mediaPlayer.prepareAsync();
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tải tệp âm thanh!", Toast.LENGTH_SHORT).show();
        }
    }


    //PHƯƠNG THỨC CẬP NHẬT SEEKBAR
    private void startSeekBarUpdate() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        runnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlayerReady && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    txtCurrentTime.setText(formatTime(currentPosition));
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(runnable);
    }
    // FORMAT TIME
    private String formatTime(int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
    // DESTROY
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // DỪNG HANDLER
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        // DỪNG XOAY
        if (rotationAnimator != null) {
            rotationAnimator.cancel();
        }
        // GIẢI PHÓNG PLAYER
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}