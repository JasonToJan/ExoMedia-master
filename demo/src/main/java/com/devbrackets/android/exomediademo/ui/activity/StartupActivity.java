package com.devbrackets.android.exomediademo.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.devbrackets.android.exomediademo.R;
import com.devbrackets.android.exomediademo.adapter.StartupListAdapter;

public class StartupActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    // change code---
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        Toast.makeText(activity.getApplicationContext(), permission + " : permission", Toast.LENGTH_LONG);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startup_activity);

        ListView exampleList = (ListView) findViewById(R.id.startup_activity_list);
        exampleList.setAdapter(new StartupListAdapter(this));
        exampleList.setOnItemClickListener(this);
        verifyStoragePermissions(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case StartupListAdapter.INDEX_AUDIO_PLAYBACK:
                showAudioSelectionActivity();
                break;

            case StartupListAdapter.INDEX_VIDEO_PLAYBACK:
                showVideoSelectionActivity();
                break;

            default:
        }
    }

    private void showVideoSelectionActivity() {
        Intent intent = new Intent(this, VideoSelectionActivity.class);
        startActivity(intent);
    }

    private void showAudioSelectionActivity() {
        Intent intent = new Intent(this, AudioSelectionActivity.class);
        startActivity(intent);
    }
}
