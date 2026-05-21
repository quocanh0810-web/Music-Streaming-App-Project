package quocanh.ntu.appnghenhac;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import java.io.IOException;

public class MusicPlayerActivity extends AppCompatActivity {
    ImageView imgMusic;
    TextView txtTitle, txtArtist;
    Button btnPlay;
    MediaPlayer mediaPlayer;
    boolean isPlaying = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        imgMusic = findViewById(R.id.imgMusic);
        txtTitle = findViewById(R.id.txtMusicTitle);
        txtArtist = findViewById(R.id.txtMusicArtist);
        btnPlay = findViewById(R.id.btnPlay);

        String title = getIntent().getStringExtra("title");
        String artist = getIntent().getStringExtra("artist");
        String image = getIntent().getStringExtra("image");
        String song = getIntent().getStringExtra("song");

        txtTitle.setText(title);
        txtArtist.setText(artist);

        Glide.with(this)
                .load(image)
                .into(imgMusic);

        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );

            mediaPlayer.setDataSource(song);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        btnPlay.setOnClickListener(v -> {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btnPlay.setText("Play");
            } else {
                mediaPlayer.start();
                btnPlay.setText("Pause");
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}
