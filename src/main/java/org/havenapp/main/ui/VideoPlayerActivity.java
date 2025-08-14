package org.havenapp.main.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.StyledPlayerView;

import org.havenapp.main.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class VideoPlayerActivity extends AppCompatActivity implements Player.Listener {

    private ExoPlayer player;
    private StyledPlayerView playerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Video Event"); // todo add to strings
        }

        // Get reference to the player view
        playerView = findViewById(R.id.player);

        initializePlayer();
    }


    private void initializePlayer() {
        // Create ExoPlayer instance
        player = new ExoPlayer.Builder(this).build();

        // Bind player to the view
        playerView.setPlayer(player);

        // Set player listener
        player.addListener(this);

        // Get the video URI from intent
        Uri videoUri = getIntent().getData();
        if (videoUri != null) {
            // Create media item and set to player
            MediaItem mediaItem = MediaItem.fromUri(videoUri);
            player.setMediaItem(mediaItem);

            // Prepare and start playback
            player.prepare();
            player.setPlayWhenReady(true); // Auto-play equivalent
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause the player when activity is paused
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume playback when activity resumes
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release the player when activity is destroyed
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ExoPlayer.Listener methods

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        switch (playbackState) {
            case Player.STATE_IDLE:
                // Player is idle
                break;
            case Player.STATE_BUFFERING:
                // Player is buffering
                onBuffering();
                break;
            case Player.STATE_READY:
                // Player is ready to play
                onPrepared();
                break;
            case Player.STATE_ENDED:
                // Playback completed
                onCompletion();
                break;
        }
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        if (isPlaying) {
            onStarted();
        } else {
            onPaused();
        }
    }

    @Override
    public void onPlayerError(com.google.android.exoplayer2.PlaybackException error) {
        onError(error);
    }

    // Equivalent methods to the original BetterVideoCallback methods

    private void onStarted() {
        //Log.i(TAG, "Started");
        // Show controls when playback starts
        playerView.showController();
    }

    private void onPaused() {
        //Log.i(TAG, "Paused");
    }

    private void onPrepared() {
        //Log.i(TAG, "Prepared");
    }

    private void onBuffering() {
        //Log.i(TAG, "Buffering");
    }

    private void onError(Exception e) {
        //Log.i(TAG, "Error " + e.getMessage());
    }

    private void onCompletion() {
        //Log.i(TAG, "Completed");
    }
}