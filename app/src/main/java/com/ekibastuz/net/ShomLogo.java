package com.ekibastuz.net;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public class ShomLogo extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.black));
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shomlogo);

        int delayMillis = 2000;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                navigateToMainActivity();
            }
        }, delayMillis);
    }

    private void navigateToMainActivity() {
        Intent mainActivityIntent = new Intent(ShomLogo.this, ActivityShomla.class);
        startActivity(mainActivityIntent);
        finish();
    }
}
