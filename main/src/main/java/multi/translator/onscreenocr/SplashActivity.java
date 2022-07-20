package multi.translator.onscreenocr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import multi.translator.onscreenocr.pages.launch.LaunchActivity;

public class SplashActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run(){
                startActivity(new Intent(SplashActivity.this, LaunchActivity.class));
                finish();
            }
        },  3000 );

    }
}
