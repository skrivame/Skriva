package me.skriva.ceph.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import me.skriva.ceph.R;
import me.skriva.ceph.utils.ThemeHelper;

import static me.skriva.ceph.ui.XmppActivity.configureActionBar;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(ThemeHelper.find(this));

        setContentView(R.layout.activity_about);
        setSupportActionBar(findViewById(R.id.toolbar));
        configureActionBar(getSupportActionBar());
        setTitle(getString(R.string.title_activity_about_x, getString(R.string.app_name
        )));
    }
}
