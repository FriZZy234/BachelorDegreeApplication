//package com.example.gg_livestream;
//
//import android.os.Bundle;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.camera.core.CameraSelector;
//import androidx.camera.core.Preview;
//import androidx.camera.lifecycle.ProcessCameraProvider;
//import androidx.camera.view.PreviewView;
//import androidx.core.content.ContextCompat;
//
//import com.arthenica.ffmpegkit.FFmpegKit;
//import com.arthenica.ffmpegkit.ReturnCode;
//import com.google.common.util.concurrent.ListenableFuture;
//
//import java.util.concurrent.ExecutionException;
//
//public class CameraPreviewActivity extends AppCompatActivity {
//
//    private PreviewView previewView;
//    private String rtmpUrl;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_camera_preview);
//
//        previewView = findViewById(R.id.previewView);
//
//        rtmpUrl = getIntent().getStringExtra("rtmpUrl");
//
//        startCamera();
//        startStreamingToYouTube(rtmpUrl);
//    }
//
//    private void startCamera() {
//        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
//
//        cameraProviderFuture.addListener(() -> {
//            try {
//                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
//                bindPreview(cameraProvider);
//            } catch (ExecutionException | InterruptedException e) {
//                e.printStackTrace();
//            }
//        }, ContextCompat.getMainExecutor(this));
//    }
//
//    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
//        Preview preview = new Preview.Builder().build();
//        CameraSelector cameraSelector = new CameraSelector.Builder()
//                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
//                .build();
//
//        preview.setSurfaceProvider(previewView.getSurfaceProvider());
//
//        cameraProvider.bindToLifecycle(this, cameraSelector, preview);
//    }
//
//    private void startStreamingToYouTube(String rtmpUrl) {
//        String[] command = {
//                "-re", "-f", "v4l2", "-i", "/dev/video0",
//                "-f", "lavfi", "-i", "anullsrc",
//                "-vcodec", "libx264", "-pix_fmt", "yuv420p",
//                "-preset", "ultrafast", "-r", "30",
//                "-g", "60", "-b:v", "2500k",
//                "-acodec", "aac", "-ar", "44100",
//                "-b:a", "128k", "-f", "flv", rtmpUrl
//        };
//
//        FFmpegKit.executeAsync(command, session -> {
//            final ReturnCode returnCode = session.getReturnCode();
//            if (ReturnCode.isSuccess(returnCode)) {
//                runOnUiThread(() -> Toast.makeText(CameraPreviewActivity.this, "Stream started successfully", Toast.LENGTH_SHORT).show());
//            } else {
//                runOnUiThread(() -> Toast.makeText(CameraPreviewActivity.this, "Failed to start stream", Toast.LENGTH_SHORT).show());
//            }
//        });
//    }
//}
