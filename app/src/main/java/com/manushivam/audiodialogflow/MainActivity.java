package com.manushivam.audiodialogflow;

import ai.api.android.AIConfiguration;
import ai.api.android.GsonFactory;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Metadata;
import ai.api.model.Result;
import ai.api.model.Status;
import ai.api.ui.AIButton;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends BaseActivity
    implements AdapterView.OnItemSelectedListener, View.OnClickListener, AIButton.AIButtonListener {

  public static final String TAG = MainActivity.class.getName();

  private AIButton aiButton;
  private static final int REQUEST_AUDIO_PERMISSIONS_ID = 33;
  private TextView resultTextView;
  public static final String ClientAccessToken = "1ce15173da1a4e35838d20dde8eb4b0d";
  TTS tts;

  private Gson gson = GsonFactory.getGson();

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    resultTextView = (TextView) findViewById(R.id.resultTextView);
    aiButton = (AIButton) findViewById(R.id.micButton);
    tts = new TTS();
    TTS.init(getApplicationContext());

    final AIConfiguration config = new AIConfiguration(ClientAccessToken,
        AIConfiguration.SupportedLanguages.English,
        AIConfiguration.RecognitionEngine.System);

    config.setRecognizerStartSound(getResources().openRawResourceFd(R.raw.test_start));
    config.setRecognizerStopSound(getResources().openRawResourceFd(R.raw.test_stop));
    config.setRecognizerCancelSound(getResources().openRawResourceFd(R.raw.test_cancel));

    aiButton.initialize(config);
    aiButton.setResultsListener(this);
    if (checkPermissions()) {

    } else {
      aiButton.setEnabled(false);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();

    // use this method to disconnect from speech recognition service
    // Not destroying the SpeechRecognition object in onPause method would block other apps from using SpeechRecognition service
    aiButton.pause();
  }

  @Override
  protected void onResume() {
    super.onResume();

    // use this method to reinit connection to recognition service
    aiButton.resume();
  }

  @Override
  public void onResult(final AIResponse response) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "onResult");
        JsonObject object = gson.fromJson(gson.toJson(response), JsonObject.class);

        Log.i(TAG, "Received success response");
        Log.d("response", object.toString());

        // this is example how to get different parts of result object
        final Status status = response.getStatus();
        Log.i(TAG, "Status code: " + status.getCode());
        Log.i(TAG, "Status type: " + status.getErrorType());

        final Result result = response.getResult();
        Log.i(TAG, "Resolved query: " + result.getResolvedQuery());

        Log.i(TAG, "Action: " + result.getAction());

        // final String speech = result.getFulfillment().getSpeech();

        final String speech = result.getFulfillment().getSpeech();
        Log.d("messages", result.getFulfillment().getMessages().toString());
        String talkSpeech = "";

        if(object.get("result")
            .getAsJsonObject()
            .get("parameters").getAsJsonObject().size()!=0
            && !object.get("result")
            .getAsJsonObject()
            .get("parameters").getAsJsonObject().get("time").getAsString().equals(""))
        {
          talkSpeech = "According to me Cases since yesterday are 1264";
        }
        else{
          JsonArray messages = object.get("result")
              .getAsJsonObject()
              .get("fulfillment")
              .getAsJsonObject()
              .get("messages")
              .getAsJsonArray();
          for (int i = 0; i < result.getFulfillment().getMessages().size(); i++) {
            Log.d("speech", messages.get(i)
                .getAsJsonObject()
                .get("speech")
                .getAsJsonArray()
                .get(0)
                .getAsString());
            talkSpeech = talkSpeech + " " + messages.get(i)
                .getAsJsonObject()
                .get("speech")
                .getAsJsonArray()
                .get(0)
                .getAsString();
          }
        }

        Log.i(TAG, "Speech: " + talkSpeech);
        resultTextView.setText(talkSpeech);
        TTS.speak(talkSpeech);

        final Metadata metadata = result.getMetadata();
        if (metadata != null) {
          Log.i(TAG, "Intent id: " + metadata.getIntentId());
          Log.i(TAG, "Intent name: " + metadata.getIntentName());
        }

        final HashMap<String, JsonElement> params = result.getParameters();
        if (params != null && !params.isEmpty()) {
          Log.i(TAG, "Parameters: ");
          for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
            Log.i(TAG, String.format("%s: %s", entry.getKey(), entry.getValue().toString()));
          }
        }
      }
    });
  }

  @Override
  public void onError(final AIError error) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "onError");
        resultTextView.setText(error.toString());
      }
    });
  }

  @Override
  public void onCancelled() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "onCancelled");
        resultTextView.setText("");
      }
    });
  }

  private void startActivity(Class<?> cls) {
    final Intent intent = new Intent(this, cls);
    startActivity(intent);
  }

  @Override
  public void onClick(View v) {

  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {

  }

  @Override
  protected void onStart() {
    super.onStart();
    checkPermissions();
  }

  private boolean checkPermissions() {
    //Check permission
    String recordPermission = Manifest.permission.RECORD_AUDIO;
    if (ActivityCompat.checkSelfPermission(this, recordPermission)
        == PackageManager.PERMISSION_GRANTED) {
      //Permission Granted
      Log.d("PER", "granted");
      aiButton.setEnabled(true);
      return true;
    } else {
      //Permission not granted, ask for permission
      Log.d("PER", "denied");
      ActivityCompat.requestPermissions(this, new String[] { recordPermission },
          REQUEST_AUDIO_PERMISSIONS_ID);
      return false;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == REQUEST_AUDIO_PERMISSIONS_ID) {
      if (grantResults.length > 0
          && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        // Showing the toast message
        aiButton.setEnabled(true);
      } else {
        aiButton.setEnabled(false);
      }
    }
  }
}


