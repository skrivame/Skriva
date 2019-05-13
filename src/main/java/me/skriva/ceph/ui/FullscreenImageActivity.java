package me.skriva.ceph.ui;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

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
