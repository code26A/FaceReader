package com.example.facereader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    Button button;
    TextView textView;
    ImageView imageView;
    private final static int REQUEST_IMAGE_CAPTURE = 124;
    InputImage firebaseVision;
    FaceDetector visionFaceDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.camera_btn);
        textView = findViewById(R.id.text1);
        imageView = findViewById(R.id.imageView);
        FirebaseApp.initializeApp(this);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenFile();
            }
        });
        Toast.makeText(this, "App is Started", Toast.LENGTH_SHORT).show();
    }

    private void OpenFile() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bundle bundle=data.getExtras();
        Bitmap bitmap=(Bitmap) bundle.get("data");
        FaceDetectionProcess(bitmap);
        Toast.makeText(this, "Success!!", Toast.LENGTH_SHORT).show();
    }

    private void FaceDetectionProcess(Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            return;
        }
        textView.setText("Processing image...");
        final StringBuilder builder = new StringBuilder();
        BitmapDrawable drawable=(BitmapDrawable)imageView.getDrawable();
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .enableTracking()
                        .build();
        FaceDetector detector = FaceDetection.getClient(highAccuracyOpts);
        Task<List<Face>> result = detector.process(image);
        result.addOnSuccessListener(new OnSuccessListener<List<Face>>() {
            @Override
            public void onSuccess(List<Face> faces) {
                if (faces != null && !faces.isEmpty()) {
                    builder.append(faces.size()).append(faces.size() == 1 ? " Face Detected\n\n" : " Faces Detected\n\n");
                    for (Face face : faces) {
                        Integer id = face.getTrackingId();
                        if (id != null) {
                            float rotY = face.getHeadEulerAngleY();
                            float rotZ = face.getHeadEulerAngleZ();
                            builder.append("Face tracking id[").append(id+1).append("]\n");
                            builder.append("1. Head Rotation to Right [").append(String.format("%.2f", rotY)).append("deg.]\n");
                            builder.append("2. Head Tilted Sideways [").append(String.format("%.2f", rotZ)).append("deg.]\n");
                            //smiling probability
                            Float smilingProbability = face.getSmilingProbability();
                            if (smilingProbability != null && smilingProbability > 0) {
                                builder.append("3. Smiling Probability[").append(String.format("%.2f", smilingProbability)).append("]\n");
                            } else {
                                builder.append("3. Smiling Probability: N/A\n");
                            }
                            //Left eye open probability
                            Float leftEyeOpenProbability = face.getLeftEyeOpenProbability();
                            if (leftEyeOpenProbability != null && leftEyeOpenProbability > 0) {
                                builder.append("4. Left Eye Open Probability[").append(String.format("%.2f", leftEyeOpenProbability)).append("]\n");
                            } else {
                                builder.append("4. Left Eye Open Probability: N/A\n");
                            }
                            //Right eye open probability
                            Float rightEyeOpenProbability = face.getRightEyeOpenProbability();
                            if (rightEyeOpenProbability != null && rightEyeOpenProbability > 0) {
                                builder.append("5. Right Eye Open Probability[").append(String.format("%.2f", rightEyeOpenProbability)).append("]\n");
                            } else {
                                builder.append("5. Right Eye Open Probability:\n");
                            }
                            builder.append("\n");
                        }
                    }
                    ShowDetection("Face Detection", builder, true);
                } else {
                    builder.append("No Face Detected\n");
                    ShowDetection("Face Detection", builder, false);
                }
            }
        });
        result.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                builder.append("Sorry!! There is an error!");
                ShowDetection("Face Detection", builder, false);
            }
        });
    }


    public void ShowDetection(final String title, final StringBuilder builder, boolean success) {
        textView.setText(null);
        textView.setMovementMethod(new ScrollingMovementMethod());

        if (success && builder.length() != 0) {
            textView.append(builder);

            String copyTitle = title;
            if (copyTitle == null) {
                // Use a default title when 'title' is null
                copyTitle = "Default Title";
            }

            if (copyTitle.substring(0, copyTitle.indexOf(' ')).equalsIgnoreCase("OCR")) {
                textView.append("\n(Hold the text to copy it!)");
            } else {
                textView.append("(Hold the text to copy!)");
            }

            textView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    String copiedText = builder.toString(); // Convert StringBuilder to String
                    ClipData clip = ClipData.newPlainText(title, copiedText);
                    clipboardManager.setPrimaryClip(clip);
                    return true;
                }
            });
        } else {
            textView.append(title.substring(0, title.indexOf(' ')) + " Failed to read anything");
        }
    }
}
