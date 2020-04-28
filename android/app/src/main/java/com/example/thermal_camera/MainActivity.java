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
import com.flir.thermalsdk.image.JavaImageBuffer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Bitmap;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.ArrayList;
import android.util.Base64;
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


  @Override
  public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
    GeneratedPluginRegistrant.registerWith(flutterEngine);

    ThermalSdkAndroid.init(getApplicationContext());

    cameraHandler = new CameraHandler();

    channel = new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL);
    

    setListeners();
  }

  private void setListeners() {
    channel.setMethodCallHandler((call, result) -> {
      if(call.method.equals("cleanAll")){
        if(cameraHandler.isConnected()) cameraHandler.disconnect();
        if(!cameraHandler.getCameraList().isEmpty()) cameraHandler.clear();
      }
      if(call.method.equals("connect")){
        connect(cameraHandler.getFlirOne());
      }
      if(call.method.equals("disconnect")){
        cameraHandler.disconnect();
        result.success(true);
      }
      if(call.method.equals("discover")) {
        startDiscovery();
      }
      if(call.method.equals("startStream")){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
              // Call the desired channel message here.
              try {
                cameraHandler.startStream(streamDataListener);
              } catch (Exception e) {
                Log.d("Fatal porra", "deu ruim na tentaiva de stream: " + e);
              }
            }
          });
      }
      if(call.method.equals("stopStream")){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
              // Call the desired channel message here.
              try {
                cameraHandler.stopStream();
              } catch (Exception e) {
                Log.d("Fatal porra", "deu ruim na tentaiva de stream: " + e);
              }
            }
          });
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

      if(connectedIdentity != null) {
        new Handler(Looper.getMainLooper()).post(new Runnable(){
          @Override
          public void run(){
            channel.invokeMethod("connected", true);
          }
        });
      }

      if(identity == null){
        return;
      }

      connectedIdentity = identity;

      if (UsbPermissionHandler.isFlirOne(identity)) {
          usbPermissionHandler.requestFlirOnePermisson(identity, getApplicationContext(), permissionListener);
      } else {
          doConnect(identity);
      }

  }

  private void doConnect(Identity identity) {
    new Thread(new Runnable(){
      @Override
      public void run(){
        try {
          cameraHandler.connect(identity, connectionStatusListener);

          new Handler(Looper.getMainLooper()).post(new Runnable(){
            @Override
            public void run(){
              channel.invokeMethod("connected", true);
            }
          });
                
        } catch (IOException e) {

          new Handler(Looper.getMainLooper()).post(new Runnable(){
            @Override
            public void run(){
              channel.invokeMethod("connected", false);
            }
          });
        }
      }
    }).start();
  }

  private UsbPermissionHandler.UsbPermissionListener permissionListener = new UsbPermissionHandler.UsbPermissionListener() {
      @Override
      public void permissionGranted(Identity identity) {
          doConnect(identity);
      }

      @Override
      public void permissionDenied(Identity identity) {
        stopDiscovery();
        cameraHandler.clear();
      }

      @Override
      public void error(UsbPermissionHandler.UsbPermissionListener.ErrorType errorType, final Identity identity) {
        
      }
  };

  private final CameraHandler.StreamDataListener streamDataListener = new CameraHandler.StreamDataListener() {
    @Override
    public void temperature(Double temperature) {
      try {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
          @Override
          public void run() {
            channel.invokeMethod("temperature", temperature);
          }
        });
        // msxBitmap.recycle();
      } catch (Exception e) {
      }
    }

    @Override
    public void bytes(Bitmap msxBitmap){
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      msxBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
      byte[] byteArray = stream.toByteArray();
      new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override
        public void run() {
          channel.invokeMethod("streamBytes", byteArray);
        }
      });
      msxBitmap.recycle();
    }

    @Override
    public void onStreamStopped(){
      new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override
        public void run() {
          channel.invokeMethod("streamFinished", true);
        }
      });
    }
  };

  private DiscoveryEventListener discoveryEventListener = new DiscoveryEventListener() {
    @Override
        public void onCameraFound(Identity identity) {
          cameraHandler.add(identity);

           new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
              // Call the desired channel message here.
              channel.invokeMethod("discovered", true);
            }
          });
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
      new Handler(Looper.getMainLooper()).post(new Runnable(){
        @Override
        public void run(){
          channel.invokeMethod("disconnected", true);
        }
      });
    }
  };

  @Override
  protected void onDestroy(){
    super.onDestroy(); 
    cameraHandler.disconnect();
  }

  @Override
  protected void onStop(){
    super.onStop(); 
    cameraHandler.disconnect();
  }
}
