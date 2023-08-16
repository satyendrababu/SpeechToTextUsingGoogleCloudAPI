package com.example.demospeechtotext;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeRequest;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.protobuf.ByteString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private Button startButton;
    private TextView resultTextView;
    private ProgressBar progressBar;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    private Thread recordingThread;
    private byte[] byteArray;
    private VoiceRecorder mVoiceRecorder;
    Handler handler = new Handler();
    private MediaPlayer mediaPlayer;

    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {

        }

        @Override
        public void onVoice(byte[] data, int size) {

            byteArray = appendByteArrays(byteArray, data);

        }

        @Override
        public void onVoiceEnd() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.VISIBLE);

                }
            });
            Log.e("kya",""+byteArray);
            transcribeRecording(byteArray);


        }

    };
    private SpeechClient speechClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.start_button);
        resultTextView = findViewById(R.id.result_text_view);
        progressBar = findViewById(R.id.progress_bar);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (permissionToRecordAccepted) {

                    if (startButton.getText().toString().equals("Start")) {
                        startButton.setText("Stop");
                        startVoiceRecorder();

                    } else {
                        stopVoiceRecorder();
                    }
                }

            }
        });


        initializeSpeechClient();
    }
    private void initializeSpeechClient() {
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(getResources().openRawResource(R.raw.credentials));
            FixedCredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);
            speechClient = SpeechClient.create(SpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build());
        } catch (IOException e) {

            Log.e("kya", "InitException" + e.getMessage());
        }
    }
    private void transcribeRecording(byte[] data) {
        try {

            Log.e("API_CALL", "API CALL STARTED...");

            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        RecognizeResponse response = speechClient.recognize(createRecognizeRequestFromVoice(data));

                        for (SpeechRecognitionResult result : response.getResultsList()) {
                            String transcript = result.getAlternativesList().get(0).getTranscript();
                            updateResult(transcript);
                        }
                    } catch (Exception e) {
                        Log.e("SEECOLE", "" + e.getMessage());
                    }

                }
            });
            recordingThread.start();
        } catch (Exception e) {
            Log.e("SEECOLE", "" + e.getMessage());
        }
    }
    private RecognizeRequest createRecognizeRequestFromVoice(byte[] audioData) {
        RecognitionAudio audioBytes = RecognitionAudio.newBuilder().setContent(ByteString.copyFrom(audioData)).build();
        RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(16000)
                .setLanguageCode("en-US")
                .build();
        return RecognizeRequest.newBuilder()
                .setConfig(config)
                .setAudio(audioBytes)
                .build();
    }
    private void updateResult(final String transcript) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                playSound();
                resultTextView.setText(transcript);
                clearByteArray(byteArray);
                startButton.setText("Start");
                stopVoiceRecorder();

            }
        });
    }
    private void startVoiceRecorder() {

        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
        }
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.start();
    }

    private void stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
        }

        if (recordingThread != null) {
            try {
                recordingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            recordingThread = null;
        }

    }

    private byte[] appendByteArrays(byte[] array1, byte[] array2) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            outputStream.write(array1);
            outputStream.write(array2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return outputStream.toByteArray();
    }
    private void clearByteArray(byte[] array) {
        // Set each element to zero
        Arrays.fill(array, (byte) 0);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionToRecordAccepted) {
            startButton.setEnabled(false);
        }
    }
    private void playSound(){
        mediaPlayer = MediaPlayer.create(this, R.raw.transcribe_voice);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.release();
            }
        });
        mediaPlayer.start();
    }
}