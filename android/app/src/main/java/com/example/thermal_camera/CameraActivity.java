package com.example.thermal_camera;

import androidx.annotation.NonNull;
import java.io.IOException;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.util.Log;

import io.flutter.embedding.android.FlutterActivity;

import com.flir.thermalsdk.androidsdk.image.BitmapAndroid;
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.image.ThermalImage;
import com.flir.thermalsdk.image.fusion.FusionMode;
import com.flir.thermalsdk.live.Camera;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.live.discovery.DiscoveryFactory;
import com.flir.thermalsdk.live.streaming.ThermalImageStreamListener;
import com.flir.thermalsdk.androidsdk.live.connectivity.UsbPermissionHandler;
import com.flir.thermalsdk.ErrorCode;


public class CameraActivity extends FlutterActivity {

    private static final String TAG = "CameraActivity";

    private UsbPermissionHandler usbPermissionHandler = new UsbPermissionHandler(); 

    private Camera camera;

    private ImageView msxImage;

    private ConnectionStatusListener connectionStatusListener = new ConnectionStatusListener(){
      @Override
      public void onDisconnected(ErrorCode errorCode) {
        Log.d(TAG, "Disconnected");
      }
    };

    private DiscoveryEventListener discoveryListener = new DiscoveryEventListener(){
       @Override
        public void onCameraFound(Identity identity) {
          Log.d(TAG, "Camera found");
          DiscoveryFactory.getInstance().stop(CommunicationInterface.USB);
          usbPermissionHandler.requestFlirOnePermisson(identity, getApplicationContext(), permissionListener);
        }

        @Override
        public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode errorCode) {
          Log.d(TAG, "Discovery Error");
        }
    };

    private UsbPermissionHandler.UsbPermissionListener permissionListener = new UsbPermissionHandler.UsbPermissionListener() {
        @Override
        public void permissionGranted(Identity identity) {

          try {
            camera.connect(identity, connectionStatusListener);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  camera.subscribeStream(thermalImageStreamListener);
                }
            });
          } catch (IOException e){

          }
        }

        @Override
        public void permissionDenied(Identity identity) {
            
        }

        @Override
        public void error(UsbPermissionHandler.UsbPermissionListener.ErrorType errorType, final Identity identity) {
            
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState){
      super.onCreate(savedInstanceState);
      ThermalSdkAndroid.init(getApplicationContext());

      camera = new Camera();

      DiscoveryFactory.getInstance().scan(discoveryListener, CommunicationInterface.USB);
    };

    @Override
    protected void onStop(){
      super.onStop();
      camera.unsubscribeAllStreams();
    }

    @Override
    protected void onStart() {
      super.onStart();
    }

    private final ThermalImageStreamListener thermalImageStreamListener = new ThermalImageStreamListener() {
      @Override
      public void onImageReceived() {

        runOnUiThread(() -> {
          camera.withImage(handleIncomingImage);
        });
      }
    };

    private final Camera.Consumer<ThermalImage> handleIncomingImage = new Camera.Consumer<ThermalImage>() {
      @Override
      public void accept(ThermalImage thermalImage) {
          //Get a bitmap with only IR data
          Bitmap msxBitmap;
          {
              thermalImage.getFusion().setFusionMode(FusionMode.THERMAL_ONLY);
              msxBitmap = BitmapAndroid.createBitmap(thermalImage.getImage()).getBitMap();
          }

          runOnUiThread(() -> {
              msxImage.setImageBitmap(msxBitmap);
            }
          );
      }
    };
}