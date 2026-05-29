package quocanh.ntu.appnghenhac;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Đợi 3 giây rồi tự động chuyển màn hình
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Tạo Intent để chuyển sang MainActivity hiện tại
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                // Đóng SplashActivity hoàn toàn để không bị quay lại khi nhấn Back
                finish();
            }
        }, 3000);
    }
}