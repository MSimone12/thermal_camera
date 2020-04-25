package com.example.thermal_camera;

import android.content.Intent;
import android.util.Log;


import androidx.annotation.NonNull;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugins.GeneratedPluginRegistrant;
import io.flutter.plugin.common.MethodChannel;

import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.androidsdk.live.connectivity.UsbPermissionHandler;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.log.ThermalLog;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Bitmap;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.lang.Exception;

public class MainActivity extends FlutterActivity {
  
  private static final String CHANNEL = "flirCamera";

  //Handles network camera operations
  private CameraHandler cameraHandler;

  private Identity connectedIdentity = null;

  private LinkedBlockingQueue<FrameDataHolder> framesBuffer = new LinkedBlockingQueue(21);
  private UsbPermissionHandler usbPermissionHandler = new UsbPermissionHandler();

  private MethodChannel channel;

  private ArrayList discoveryList = new ArrayList<String>();

  private MethodChannel.Result methodResult;


  @Override
  public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
    GeneratedPluginRegistrant.registerWith(flutterEngine);

    ThermalLog.LogLevel enableLoggingInDebug = ThermalLog.LogLevel.NONE;

    //ThermalSdkAndroid has to be initiated from a Activity with the Application Context to prevent leaking Context,
    // and before ANY using any ThermalSdkAndroid functions
    //ThermalLog will show log from the Thermal SDK in standards android log framework
    ThermalSdkAndroid.init(getApplicationContext(), enableLoggingInDebug);

    cameraHandler = new CameraHandler();

    channel = new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL);
    startDiscovery();

    setListeners();
  }

  private void setListeners() {
    channel.setMethodCallHandler((call, result) -> {
      methodResult = result;
      if(call.method.equals("connect")){
        connect(cameraHandler.getFlirOne());
        result.success(true);
      }
      if(call.method.equals("disconnect")){
        cameraHandler.disconnect();
        result.success(true);
      }
      if(call.method.equals("discover")) {
        result.success(discoveryList);
      }
      if(call.method.equals("startStream")){
        try {
          cameraHandler.startStream(streamDataListener);
        } catch (Exception e) {
          Log.d("Fatal porra", "deu ruim na tentaiva de stream: " + e);
        }
      }
    });
  }

  private void startDiscovery() {
    cameraHandler.startDiscovery(discoveryEventListener, discoveryStatusListener);
  }

  private void stopDiscovery(){
    cameraHandler.stopDiscovery(discoveryStatusListener);
  }

  private void connect(Identity identity) {
      //We don't have to stop a discovery but it's nice to do if we have found the camera that we are looking for
      stopDiscovery();

      if (UsbPermissionHandler.isFlirOne(identity)) {
          usbPermissionHandler.requestFlirOnePermisson(identity, getApplicationContext(), permissionListener);
      } else {
          doConnect(identity);
      }

  }

  private void doConnect(Identity identity) {
    Log.d("Connect", "Trying to connect to: " + identity.deviceId);
    try {
      cameraHandler.connect(identity, connectionStatusListener);

       Log.d("Connect", "Connected to: " + identity.deviceId);
        
      // cameraHandler.startStream(streamDataListener);
            
    } catch (IOException e) {
      Log.d("Fatal porra", "deu ruim na tentaiva de conexão");
    }
  }

  private UsbPermissionHandler.UsbPermissionListener permissionListener = new UsbPermissionHandler.UsbPermissionListener() {
      @Override
      public void permissionGranted(Identity identity) {
          doConnect(identity);
      }

      @Override
      public void permissionDenied(Identity identity) {
      }

      @Override
      public void error(UsbPermissionHandler.UsbPermissionListener.ErrorType errorType, final Identity identity) {
        
      }
  };

  private final CameraHandler.StreamDataListener streamDataListener = new CameraHandler.StreamDataListener() {
    @Override
    public void image(FrameDataHolder dataHolder) {
      Log.d("DataHolder", "dataHolder received");
    }

    @Override
    public void image(Bitmap msxBitmap) {
      Log.d("StreamListener", "Bitmap received");
      try {
          ByteArrayOutputStream stream = new ByteArrayOutputStream();
          msxBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
          byte[] biteArray = stream.toByteArray();

          
          new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
              // Call the desired channel message here.
              channel.invokeMethod("stream", biteArray);
            }
          });
          
      } catch (Exception e) {
        Log.d("StreamListener", "UNABLE TO PUT IMAGES INTO FRAMES BUFFER "+e);
      }
    }
  };

  private DiscoveryEventListener discoveryEventListener = new DiscoveryEventListener() {
    @Override
        public void onCameraFound(Identity identity) {
          discoveryList.add(identity.deviceId);
          cameraHandler.add(identity);
        }

        @Override
        public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode errorCode) {
          
        }
  };

   private CameraHandler.DiscoveryStatus discoveryStatusListener = new CameraHandler.DiscoveryStatus() {
      @Override
      public void started(){};

      @Override
      public void stopped(){};
  };

  private ConnectionStatusListener connectionStatusListener = new ConnectionStatusListener(){
    @Override
    public void onDisconnected(ErrorCode errorCode){
      Log.d("Conn", "Desconectou ");
    }
  };
}
