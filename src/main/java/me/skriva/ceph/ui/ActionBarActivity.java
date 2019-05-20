package me.skriva.ceph.ui;

import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;


public abstract class ActionBarActivity extends AppCompatActivity {
    public static void configureActionBar(ActionBar actionBar) {
        configureActionBar(actionBar, true);
    }

    static void configureActionBar(ActionBar actionBar, boolean upNavigation) {
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(upNavigation);
            actionBar.setDisplayHomeAsUpEnabled(upNavigation);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}