package com.example.thermal_camera;

import java.io.IOException;

import androidx.annotation.NonNull;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugins.GeneratedPluginRegistrant;
import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.androidsdk.live.connectivity.UsbPermissionHandler;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.log.ThermalLog;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {

  private static final String CHANNEL = "flirCamera";

  private CameraHandler cameraHandler;

  private Identity connectedIdentity = null;

  private UsbPermissionHandler usbPermissionHandler = new UsbPermissionHandler();

  
  public void connect(Identity identity) {
    cameraHandler.stopDiscovery(discoveryStatusListener);

    if(connectedIdentity != null) {
      return;
    }

    if(identity == null){
      return;
    }

    connectedIdentity = identity;

    if (UsbPermissionHandler.isFlirOne(identity)) {
        usbPermissionHandler.requestFlirOnePermisson(identity, this, permissionListener);
    } else {
        doConnect(identity);
    }
  }

  public void connectFlirOne() {
    connect(cameraHandler.getFlirOne());
  }

  @Override
  public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
    GeneratedPluginRegistrant.registerWith(flutterEngine);

    new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL).setMethodCallHandler(
      (call, result) -> {
        switch(call.method) {
          case "startDiscovery":
            cameraHandler.startDiscovery();
            result.success();
            break;
          case "connect":
            cameraHandler.connect(cameraHandler.getFlirOne());
            break;
        }
      }
    );
  }

  private CameraHandler.DiscoveryStatus discoveryStatusListener = new CameraHandler.DiscoveryStatus() {
      @Override
      public void started() {
          discoveryStatus.setText(getString(R.string.connection_status_text, "discovering"));
      }

      @Override
      public void stopped() {
          discoveryStatus.setText(getString(R.string.connection_status_text, "not discovering"));
      }
  };

  private void doConnect(Identity identity) {
      new Thread(() -> {
          try {
              cameraHandler.connect(identity, connectionStatusListener);
              runOnUiThread(() -> {
                  cameraHandler.startStream(streamDataListener);
              });
          } catch (IOException e) {
              
          }
      }).start();
  };
}
