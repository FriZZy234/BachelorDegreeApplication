package com.example.gg_livestream;

import android.accounts.AccountManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentTransaction;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeRequestInitializer;
import com.google.api.services.youtube.model.CdnSettings;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastSnippet;
import com.google.api.services.youtube.model.LiveBroadcastStatus;
import com.google.api.services.youtube.model.LiveStream;
import com.google.api.services.youtube.model.LiveStreamSnippet;
import com.google.api.services.youtube.model.LiveStreamStatus;
import com.pedro.common.ConnectChecker;
import com.pedro.encoder.input.gl.SpriteGestureController;
import com.pedro.encoder.input.gl.render.filters.object.ImageObjectFilterRender;
import com.pedro.encoder.input.video.CameraHelper;
import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.encoder.utils.gl.TranslateTo;
import com.pedro.library.rtmp.RtmpCamera2;
import com.pedro.library.view.OpenGlView;

import java.io.IOException;
import java.util.Collections;

public class LivestreamActivity extends AppCompatActivity implements OnStreamSettingsListener {


    private int selectedBackgroundColor = Color.BLACK; // Default background color

    private String team1Name = "Team 1";
    private String team2Name = "Team 2";
    private Bitmap team1Logo;
    private Bitmap team2Logo;
    private static final int REQUEST_ACCOUNT_PICKER = 1000;
    private static final int REQUEST_AUTHORIZATION = 1001;
    private static final String[] SCOPES = {"https://www.googleapis.com/auth/youtube.force-ssl"};

    private OpenGlView openGlView;
    private RtmpCamera2 rtmpCamera1;
    private GoogleAccountCredential credential;
    private String title, description, visibility;
    private Uri thumbnailUri;
    private String rtmpUrl;

    private TextView team1ScoreView, team2ScoreView, timerTextView;
    private ImageView scoreboardPreview;
    private int team1Score = 0, team2Score = 0;
    private Handler timerHandler = new Handler();
    private long startTime = 0;
    private Bitmap pauseImage;
    private boolean isPaused = false;
    private long elapsedTime = 0;

    private ImageObjectFilterRender imageObjectFilterRender, pauseImageObjectFilterRender;
    private SpriteGestureController spriteGestureController = new SpriteGestureController();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_livestream);

        openGlView = findViewById(R.id.openGlView);

        // Initialize RTMP camera with OpenGLView
        rtmpCamera1 = new RtmpCamera2(openGlView, new ConnectChecker() {
            @Override
            public void onConnectionStarted(@NonNull String rtmpUrl) {
                Log.d("Livestream", "Connection started: " + rtmpUrl);
            }

            @Override
            public void onConnectionSuccess() {
                runOnUiThread(() -> Toast.makeText(LivestreamActivity.this, "Connection success", Toast.LENGTH_SHORT).show());
                Log.d("Livestream", "Connection success");
            }

            @Override
            public void onConnectionFailed(String reason) {
                runOnUiThread(() -> {
                    Toast.makeText(LivestreamActivity.this, "Connection failed. " + reason, Toast.LENGTH_SHORT).show();
                    Log.e("Livestream", "Connection failed: " + reason);
                    rtmpCamera1.stopStream();
                });
            }

            @Override
            public void onNewBitrate(long bitrate) {
                Log.d("Livestream", "New bitrate: " + bitrate);
            }

            @Override
            public void onDisconnect() {
                runOnUiThread(() -> Toast.makeText(LivestreamActivity.this, "Disconnected", Toast.LENGTH_SHORT).show());
                Log.d("Livestream", "Disconnected");
            }

            @Override
            public void onAuthError() {
                runOnUiThread(() -> Toast.makeText(LivestreamActivity.this, "Auth error", Toast.LENGTH_SHORT).show());
                Log.d("Livestream", "Auth error");
            }

            @Override
            public void onAuthSuccess() {
                runOnUiThread(() -> Toast.makeText(LivestreamActivity.this, "Auth success", Toast.LENGTH_SHORT).show());
                Log.d("Livestream", "Auth success");
            }
        });

        // Initialize Google Account Credential
        credential = GoogleAccountCredential.usingOAuth2(this, Collections.singletonList(SCOPES[0]));
        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);

        // Load initial fragment
        if (savedInstanceState == null) {
            navigateToScoreboardFragment();
        }

        // Initialize scoreboard UI elements
        team1ScoreView = findViewById(R.id.team1_score);
        team2ScoreView = findViewById(R.id.team2_score);
        timerTextView = findViewById(R.id.timer_text);
        scoreboardPreview = findViewById(R.id.scoreboard_preview);
        Button team1PlusButton = findViewById(R.id.team1_plus_button);
        Button team2PlusButton = findViewById(R.id.team2_plus_button);
        Button startTimerButton = findViewById(R.id.start_timer_button);
        Button stopTimerButton = findViewById(R.id.stop_timer_button);
        Button pauseButton = findViewById(R.id.pause_button);
        Button resumeButton = findViewById(R.id.resume_button);
        Button resumeTimerButton = findViewById(R.id.resume_timer_button);

        imageObjectFilterRender = new ImageObjectFilterRender();
        pauseImageObjectFilterRender = new ImageObjectFilterRender();
        openGlView.addFilter(imageObjectFilterRender);
        openGlView.addFilter(pauseImageObjectFilterRender);

        team1PlusButton.setOnClickListener(v -> {
            team1Score++;
            team1ScoreView.setText(String.valueOf(team1Score));
            updateScoreboardPreview();
            updateOverlay();
        });

        team2PlusButton.setOnClickListener(v -> {
            team2Score++;
            team2ScoreView.setText(String.valueOf(team2Score));
            updateScoreboardPreview();
            updateOverlay();
        });


        startTimerButton.setOnClickListener(v -> {
            startTimer();
            updateOverlay();
        });

        stopTimerButton.setOnClickListener(v -> {
            stopTimer();
            updateOverlay();
        });
        resumeTimerButton.setOnClickListener(v -> {
            resumeTimer();
            updateOverlay();
        });
        pauseButton.setOnClickListener(v -> {
            pauseStream();
            pauseButton.setVisibility(View.GONE);
            resumeButton.setVisibility(View.VISIBLE);
        });

        resumeButton.setOnClickListener(v -> {
            resumeStream();
            pauseButton.setVisibility(View.VISIBLE);
            resumeButton.setVisibility(View.GONE);
        });


        Spinner colorSpinner = findViewById(R.id.color_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.colors_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(adapter);

        colorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

                switch (position) {
                    case 0: // Negru
                        selectedBackgroundColor = Color.BLACK;
                        break;
                    case 1: // Alb
                        selectedBackgroundColor = Color.WHITE;
                        break;
                    case 2: // Galben
                        selectedBackgroundColor = Color.YELLOW;
                        break;
                    case 3: // Verde
                        selectedBackgroundColor = Color.GREEN;
                        break;
                    case 4: // Roșu
                        selectedBackgroundColor = Color.RED;
                        break;
                    case 5: // Marou
                        selectedBackgroundColor = Color.rgb(139, 69, 19); // Maro
                        break;
                    case 6: // Violet
                        selectedBackgroundColor = Color.rgb(148, 0, 211); // Violet
                        break;
                    case 7: // Portocaliu
                        selectedBackgroundColor = Color.rgb(255, 165, 0); // Portocaliu
                        break;
                    case 8: // Roz
                        selectedBackgroundColor = Color.rgb(255, 192, 203); // Roz
                        break;
                }
                updateOverlay();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing here
            }
        });
    }

    private void startPreview() {
        try {
            rtmpCamera1.startPreview();
            drawOverlay();
        } catch (CameraOpenException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to start camera preview", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawOverlay() {
        updateOverlay();
    }

    private void updateOverlay() {
        runOnUiThread(() -> {
            String timerText = timerTextView.getText().toString();
            Bitmap scoreboardBitmap = createScoreboardBitmap(team1Name, team2Name, team1Logo, team2Logo, team1Score, team2Score, timerText, selectedBackgroundColor);
            imageObjectFilterRender.setImage(scoreboardBitmap);


            int bitmapWidth = scoreboardBitmap.getWidth();
            int bitmapHeight = scoreboardBitmap.getHeight();
            float scaleX = 33.3f;  // Aprox. 1/3 din lățimea ecranului
            float scaleY = (float) bitmapHeight / (float) bitmapWidth * scaleX;

            imageObjectFilterRender.setScale(scaleX, scaleY);
            imageObjectFilterRender.setPosition(TranslateTo.TOP);
        });
    }


    private void startTimer() {
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
        updateOverlay();
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        elapsedTime += System.currentTimeMillis() - startTime;
        updateOverlay();
    }

    private void resumeTimer() {
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
        updateOverlay();
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime + elapsedTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
            timerHandler.postDelayed(this, 500);
            updateOverlay();
        }
    };

    private void updateScoreboardPreview() {
        String timerText = timerTextView.getText().toString();
        Bitmap preview = createScoreboardBitmap(team1Name, team2Name, team1Logo, team2Logo, team1Score, team2Score, timerText, selectedBackgroundColor);
        scoreboardPreview.setImageBitmap(preview);
        updateOverlay();
    }

    private Bitmap createScoreboardBitmap(String team1Name, String team2Name, Bitmap team1Logo, Bitmap team2Logo, int team1Score, int team2Score, String timerText, int backgroundColor) {
        int width = 800;
        int height = 300;
        Bitmap scoreboardBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(scoreboardBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTypeface(ResourcesCompat.getFont(this, R.font.montserrat_extrabold));


        paint.setColor(backgroundColor);
        canvas.drawRect(0, 0, width, height, paint);


        paint.setColor(Color.WHITE);
        paint.setTextSize(40);
        canvas.drawText(team1Name, 50, 50, paint);
        canvas.drawText("VS", width / 2 - 20, 50, paint);
        canvas.drawText(team2Name, width - 200, 50, paint);


        if (team1Logo != null) {
            Rect destRect1 = new Rect(50, 100, 150, 200);
            canvas.drawBitmap(team1Logo, null, destRect1, null);
        }
        if (team2Logo != null) {
            Rect destRect2 = new Rect(width - 200, 100, width - 100, 200);
            canvas.drawBitmap(team2Logo, null, destRect2, null);
        }


        paint.setTextSize(50);
        canvas.drawText(String.valueOf(team1Score), width / 2 - 150, 150, paint); // Sub "VS"
        canvas.drawText(String.valueOf(team2Score), width / 2 + 100, 150, paint); // Sub "VS"


        paint.setTextSize(30);
        canvas.drawText(timerText, width / 2 - 60, height - 20, paint);

        return scoreboardBitmap;
    }



    private void pauseStream() {
        runOnUiThread(() -> {
            if (!isPaused) {

                pauseImage = BitmapFactory.decodeResource(getResources(), R.drawable.half_time);
                imageObjectFilterRender.setImage(pauseImage);
                imageObjectFilterRender.setScale(100f, 100f);
                imageObjectFilterRender.setPosition(TranslateTo.CENTER);
                isPaused = true;
            }
        });
    }

    private void resumeStream() {
        runOnUiThread(() -> {
            if (isPaused) {

                imageObjectFilterRender.setImage(null);
                isPaused = false;
                updateOverlay();
            }
        });
    }

    public void navigateToScoreboardFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new ScoreboardFragment());
        transaction.commit();
    }

    public void navigateToStreamDetailsFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new StreamSettingsFragment());
        transaction.commit();
    }

    public void navigateToFinalizeSetupFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new FinalizeSetupFragment());
        transaction.commit();
    }

    @Override
    public void setScoreboardDetails(String firstPlayerName, String secondPlayerName) {
        this.team1Name = firstPlayerName;
        this.team2Name = secondPlayerName;
        updateScoreboardPreview();
        updateOverlay();
    }

    @Override
    public void setScoreboardDetails(String firstPlayerName, String secondPlayerName, Bitmap team1Logo, Bitmap team2Logo) {
        this.team1Name = firstPlayerName;
        this.team2Name = secondPlayerName;
        this.team1Logo = team1Logo;
        this.team2Logo = team2Logo;
        updateScoreboardPreview();
        updateOverlay();
    }

    @Override
    public void setStreamDetails(String title, String description, String visibility, Uri thumbnailUri) {
        this.title = title;
        this.description = description;
        this.visibility = visibility;
        this.thumbnailUri = thumbnailUri;
    }

    public void startLiveStream() {
        if (title == null || title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (description == null || description.isEmpty()) {
            Toast.makeText(this, "Description is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (visibility == null || visibility.isEmpty()) {
            Toast.makeText(this, "Visibility is required", Toast.LENGTH_SHORT).show();
            return;
        }

        new CreateLiveBroadcastTask().execute();
    }

    private void startStreamingToYouTube() {
        runOnUiThread(() -> {
            if (!rtmpCamera1.isStreaming()) {
                Log.d("RTMP URL", "RTMP URL: " + rtmpUrl);

                if (rtmpUrl == null || !rtmpUrl.startsWith("rtmp://")) {
                    Toast.makeText(LivestreamActivity.this, "Malformed RTMP URL", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean videoPrepared = rtmpCamera1.prepareVideo(1280, 720, 30, 1200 * 1024, CameraHelper.getCameraOrientation(this));
                boolean audioPrepared = rtmpCamera1.prepareAudio(64 * 1024, 44100, true);

                if (videoPrepared && audioPrepared) {
                    rtmpCamera1.startStream(rtmpUrl);
                } else {
                    Toast.makeText(LivestreamActivity.this, "Error preparing stream", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(LivestreamActivity.this, "Already streaming", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    private class CreateLiveBroadcastTask extends AsyncTask<Void, Void, String> {
        private String broadcastId;
        private String errorMessage = null;
        private static final String TAG = "CreateLiveBroadcastTask";

        @Override
        protected String doInBackground(Void... voids) {
            try {
                NetHttpTransport transport = new NetHttpTransport();
                JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
                String apiKey = getString(R.string.youtube_api_key);

                YouTube youtubeService = new YouTube.Builder(transport, jsonFactory, credential)
                        .setApplicationName("GG Livestream")
                        .setYouTubeRequestInitializer(new YouTubeRequestInitializer(apiKey))
                        .build();

                LiveBroadcastSnippet broadcastSnippet = new LiveBroadcastSnippet();
                broadcastSnippet.setTitle(title);
                broadcastSnippet.setDescription(description);
                DateTime startTime = new DateTime(System.currentTimeMillis());
                broadcastSnippet.setScheduledStartTime(startTime);

                LiveBroadcastStatus broadcastStatus = new LiveBroadcastStatus();
                broadcastStatus.setPrivacyStatus(visibility);

                LiveBroadcast liveBroadcast = new LiveBroadcast();
                liveBroadcast.setSnippet(broadcastSnippet);
                liveBroadcast.setStatus(broadcastStatus);

                LiveBroadcast returnedBroadcast = youtubeService.liveBroadcasts()
                        .insert("snippet,status", liveBroadcast)
                        .execute();

                LiveStreamSnippet streamSnippet = new LiveStreamSnippet();
                streamSnippet.setTitle(title);

                CdnSettings cdnSettings = new CdnSettings();
                cdnSettings.setFormat("1080p");
                cdnSettings.setIngestionType("rtmp");
                cdnSettings.setResolution("1080p");
                cdnSettings.setFrameRate("30fps");

                LiveStreamStatus streamStatus = new LiveStreamStatus();
                streamStatus.setStreamStatus("active");

                LiveStream liveStream = new LiveStream();
                liveStream.setSnippet(streamSnippet);
                liveStream.setCdn(cdnSettings);
                liveStream.setStatus(streamStatus);

                LiveStream returnedStream = youtubeService.liveStreams()
                        .insert("snippet,cdn,status", liveStream)
                        .execute();

                youtubeService.liveBroadcasts()
                        .bind(returnedBroadcast.getId(), "id,contentDetails")
                        .setStreamId(returnedStream.getId())
                        .execute();

                broadcastId = returnedBroadcast.getId();
                rtmpUrl = "rtmp://a.rtmp.youtube.com/live2/0pjv-dfsm-r4j3-k9sy-14p1";
                Log.d("RTMP URL", "RTMP URL: " + rtmpUrl);

                return "Live stream created successfully";

            } catch (GooglePlayServicesAvailabilityIOException e) {
                errorMessage = "Google Play Services not available: " + e.getMessage();
                return errorMessage;
            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                errorMessage = "Authorization required.";
                return errorMessage;
            } catch (IOException e) {
                errorMessage = "Error: " + e.getMessage();
                Log.e(TAG, errorMessage);
                return errorMessage;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(LivestreamActivity.this, result, Toast.LENGTH_LONG).show();
            if (!result.contains("Error") && !result.contains("Authorization")) {
                startStreamingToYouTube();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        credential.setSelectedAccountName(accountName);
                        new CreateLiveBroadcastTask().execute();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    new CreateLiveBroadcastTask().execute();
                }
                break;
        }
    }
}
