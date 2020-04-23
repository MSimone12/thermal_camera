package com.example.thermal_camera;

import androidx.annotation.NonNull;
import java.io.IOException;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.util.Log;
import android.app.Activity;

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


public class CameraActivity extends Activity {

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
          Log.d(TAG, "Camera found "+ identity.deviceId);
         
          DiscoveryFactory.getInstance().stop(CommunicationInterface.USB);
          if (UsbPermissionHandler.isFlirOne(identity)) {
            usbPermissionHandler.requestFlirOnePermisson(identity, getApplicationContext(), permissionListener);
          } else {
              connect(identity);
          }
          
        }

        @Override
        public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode errorCode) {
          Log.d(TAG, "Discovery Error");
          DiscoveryFactory.getInstance().stop(CommunicationInterface.USB);
        }
    };

    private void connect(Identity identity) {
      try {
            camera.connect(identity, connectionStatusListener);
            runOnUiThread(() -> {
                camera.subscribeStream(thermalImageStreamListener);
              }
            );
          } catch (IOException e){
           Log.d(TAG, "deu ruim!!");
          }
    }

    private UsbPermissionHandler.UsbPermissionListener permissionListener = new UsbPermissionHandler.UsbPermissionListener() {
        @Override
        public void permissionGranted(Identity identity) {
          Log.d(TAG, "permissionGranted");
          connect(identity);
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
      setContentView(R.layout.activity_main);

      ThermalSdkAndroid.init(getApplicationContext());

      camera = new Camera();

      setupViews();

      DiscoveryFactory.getInstance().scan(discoveryListener, CommunicationInterface.USB);
    };

    @Override
    protected void onStop(){
      super.onStop();
      if (camera == null) {
            return;
        }
        if (camera.isGrabbing()) {
            camera.unsubscribeAllStreams();
        }
        camera.disconnect();
    }

    @Override
    protected void onStart() {
      super.onStart();
    }

    private final ThermalImageStreamListener thermalImageStreamListener = new ThermalImageStreamListener() {
      @Override
      public void onImageReceived() {
        Log.d(TAG, "IMAGE RECEIVED");
        runOnUiThread(() -> {
          camera.withImage(handleIncomingImage);
        });
      }
    };

    private final Camera.Consumer<ThermalImage> handleIncomingImage = new Camera.Consumer<ThermalImage>() {
      @Override
      public void accept(ThermalImage thermalImage) {

          Log.d(TAG, "THERMAL IMAGE RECEIVED");
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

    private void setupViews() {
        msxImage = findViewById(R.id.msx_image);
    };
}