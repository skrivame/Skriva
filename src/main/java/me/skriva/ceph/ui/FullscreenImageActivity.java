package me.skriva.ceph.ui;

import android.content.Context;
import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.github.piasy.biv.BigImageViewer;
import com.github.piasy.biv.indicator.progresspie.ProgressPieIndicator;
import com.github.piasy.biv.view.BigImageView;

import me.skriva.ceph.R;

public class FullscreenImageActivity extends AppCompatActivity {

    public static void start(Context context, String file) {
        Intent intent = new Intent(context, FullscreenImageActivity.class);
        intent.putExtra("imageuri", file);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DataBindingUtil.setContentView(this, R.layout.activity_big_image);

        Intent intent = getIntent();
        String image_path= intent.getStringExtra("imageuri");

        BigImageView bigImageView = findViewById(R.id.mBigImage);
        bigImageView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);

        bigImageView.setProgressIndicator(new ProgressPieIndicator());
        bigImageView.showImage(Uri.parse(image_path));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BigImageViewer.imageLoader().cancelAll();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
