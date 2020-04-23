package com.example.thermal_camera;

import android.content.Intent;

import androidx.annotation.NonNull;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugins.GeneratedPluginRegistrant;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodCall;

import android.os.Bundle;

public class MainActivity extends FlutterActivity {
  
  private static final String CHANNEL = "flirCamera";

  private void startFlirOneActivity(Bundle bundle) {
    Intent intent = new Intent(this, CameraActivity.class);
    startActivity(intent, bundle);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    startFlirOneActivity(savedInstanceState);
  }

  @Override
  public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
    GeneratedPluginRegistrant.registerWith(flutterEngine);
  }

   @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
  }
}
