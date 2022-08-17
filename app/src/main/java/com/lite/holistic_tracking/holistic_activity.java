package com.lite.holistic_tracking;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.google.mediapipe.components.CameraHelper;
import com.google.mediapipe.components.CameraXPreviewHelper;
import com.google.mediapipe.components.ExternalTextureConverter;
import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.components.PermissionHelper;
import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.framework.PacketGetter;
import com.google.mediapipe.glutil.EglManager;
import com.google.protobuf.InvalidProtocolBufferException;

import org.json.JSONArray;
import org.json.JSONException;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class holistic_activity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Flips the camera-preview frames vertically by default, before sending them into FrameProcessor
    // to be processed in a MediaPipe graph, and flips the processed frames back when they are
    // displayed. This maybe needed because OpenGL represents images assuming the image origin is at
    // the bottom-left corner, whereas MediaPipe in general assumes the image origin is at the
    // top-left corner.
    // NOTE: use "flipFramesVertically" in manifest metadata to override this behavior.
    private static final boolean FLIP_FRAMES_VERTICALLY = true;

    private Button backBtn;
    static {
        // Load all native libraries needed by the app.
        System.loadLibrary("mediapipe_jni");
        try {
            System.loadLibrary("opencv_java3");
        } catch (UnsatisfiedLinkError e) {
            // Some example apps (e.g. template matching) require OpenCV 4.
            System.loadLibrary("opencv_java4");
        }
    }

    // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
    // frames onto a {@link Surface}.
    protected FrameProcessor processor;
    // Handles camera access via the {@link CameraX} Jetpack support library.
    protected CameraXPreviewHelper cameraHelper;

    // {@link SurfaceTexture} where the camera-preview frames can be accessed.
    private SurfaceTexture previewFrameTexture;
    // {@link SurfaceView} that displays the camera-preview frames processed by a MediaPipe graph.
    private SurfaceView previewDisplayView;

    // Creates and manages an {@link EGLContext}.
    private EglManager eglManager;
    // Converts the GL_TEXTURE_EXTERNAL_OES texture from Android camera into a regular texture to be
    // consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
    private ExternalTextureConverter converter;

    // ApplicationInfo for retrieving metadata defined in the manifest.
    private ApplicationInfo applicationInfo;

    float[][][] input_data = new float[1][30][432];
    float[][] output_data = new float[1][3];
    int l = 0;

    String[] motion = {"look","train","left"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_holistic_activity);


        HashMap<String, float[][]> LandmarkMap = new HashMap<>();
        LandmarkMap.put("pose",null);
        LandmarkMap.put("leftHand",null);
        LandmarkMap.put("rightHand",null);
        LandmarkMap.put("face",null);

        RetrofitClient retrofitClient = new RetrofitClient();
        retrofitClient.generateClient();

        backBtn = findViewById(R.id.BackBtn);
        backBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                finish();
            }
        });
        try {
            applicationInfo =
                    getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot find application info: " + e);
        }

        previewDisplayView = new SurfaceView(this);
        setupPreviewDisplayView();

        // Initialize asset manager so that MediaPipe native libraries can access the app assets, e.g.,
        // binary graphs.
        AndroidAssetUtil.initializeNativeAssetManager(this);
        eglManager = new EglManager(null);
        processor =
                new FrameProcessor(
                        this,
                        eglManager.getNativeContext(),
                        applicationInfo.metaData.getString("binaryGraphName"),
                        applicationInfo.metaData.getString("inputVideoStreamName"),
                        applicationInfo.metaData.getString("outputVideoStreamName")
                );

        processor
                .addPacketCallback("face_landmarks", (packet) -> {
                    try {
//                        Log.d("ㄱ", "face");
                        byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
                        LandmarkProto.NormalizedLandmarkList poseLandmarks = LandmarkProto.NormalizedLandmarkList.parseFrom(landmarksRaw);
//                        Log.v("AAA", String.valueOf(packet));
//                        LandmarkProto.NormalizedLandmarkList poseLandmarks =
//                                PacketGetter.getProto(packet, LandmarkProto.NormalizedLandmarkList.class);
//                        Log.v(
//                                "AAA_FL",
//                                "[TS:"
//                                        + packet.getTimestamp()
//                                        + "] "
//                                        + getPoseLandmarksDebugString(poseLandmarks));
                        LandmarkMap.put("face",getPoseLandmarksDebugAry(poseLandmarks));

                        Call<JsonElement> callAPI = retrofitClient.getApi().sendLandmark(LandmarkMap);

                        callAPI.enqueue(new Callback<JsonElement>() {
                            @Override
                            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                                // Landmark Map 값 초기화
                                LandmarkMap.put("pose",null);
                                LandmarkMap.put("leftHand",null);
                                LandmarkMap.put("rightHand",null);
                                LandmarkMap.put("face",null);
                                // api로부터 받은 계산된 좌표값을 모델의 input 형태에 맞게 변환 (JsonElement -> JsonArray -> String -> String[])
                                JsonArray DictResponseArray = response.body().getAsJsonArray();
                                Log.e("받아온 값", String.valueOf(DictResponseArray));
                                String StringResponse = String.valueOf(DictResponseArray);
                                StringResponse = StringResponse.replace("[","");
                                StringResponse = StringResponse.replace("]","");
                                String[] strArr = StringResponse.split(",");

                                try {
                                    //1. 배열에 계산된 좌표값을 30개씩 받아와야 함. (String[] -> Float)
                                    //1-(1). 🐌배열은 stack형식으로 받아야 함!!
                                    if(l<30){
                                        for(int i=0; i<432; i++){
                                            input_data[0][l][i] = Float.parseFloat(strArr[i]);
                                        }
                                        l++;

                                    }else{// 2. 30개가 되면 모델에게 보내기
                                        Interpreter lite = getTfliteInterpreter("AAAA4.tflite");
                                        lite.run(input_data, output_data);

                                        Log.e("번역된 값이에요", String.valueOf(output_data[0][0])+" "+String.valueOf(output_data[0][1])+" "+String.valueOf(output_data[0][2]));
                                        l=0;
                                    }
                                    //3. output을 텍스트 뷰에 띄워주기
                                    TextView answerFrame = findViewById(R.id.answerFrame);
                                        // 3-(1). output_data에서 확률이 제일 큰 값을 AND 0.9이상일때만 출력하기
                                    if((output_data[0][0]>=0.7 || output_data[0][1]>=0.7) || output_data[0][2]>=0.7){
                                        // 3-(2). 배열의 max값에 해당하는 motion 데이터 값 출력하기
                                        float maxNum = 0;
                                        int maxLoc = -1;
                                        for(int x=0; x<output_data.length; x++){
                                            if(maxNum<output_data[0][x]){
                                                maxNum = output_data[0][x];
                                                maxLoc = x;
                                            }
                                        }
                                        if(maxLoc!=-1){
                                            Log.e("번역 : ",motion[maxLoc]);
                                            answerFrame.setText(motion[maxLoc]);
                                        }

                                    }else{
                                        answerFrame.setText("뭘까요?");
                                    }

//                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            @Override
                            public void onFailure(Call<JsonElement> call, Throwable t) {
                                Log.e("실패군","실패다");
                            }
                        });
                    } catch (InvalidProtocolBufferException e) {
                        Log.e("AAA", "Failed to get proto.", e);
                    }

                });
        processor
                .addPacketCallback("pose_landmarks", (packet) -> {
                    try {
//                        Log.d("ㄱ", "pose");
                        byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
                        LandmarkProto.NormalizedLandmarkList poseLandmarks = LandmarkProto.NormalizedLandmarkList.parseFrom(landmarksRaw);
//                        Log.v("AAA", String.valueOf(packet));
//                        LandmarkProto.NormalizedLandmarkList poseLandmarks =
//                                PacketGetter.getProto(packet, LandmarkProto.NormalizedLandmarkList.class);
//                        Log.v(
//                                "AAA_PL",
//                                "[TS:"
//                                        + packet.getTimestamp()
//                                        + "] "
//                                        + getPoseLandmarksDebugString(poseLandmarks));
                        LandmarkMap.put("pose",getPoseLandmarksDebugAry(poseLandmarks));
                    } catch (InvalidProtocolBufferException e) {
                        Log.e("AAA", "Failed to get proto.", e);
                    }

                });
        processor
                .addPacketCallback("left_hand_landmarks", (packet) -> {
                    try {
//                        Log.d("ㄱ", "left");
                        byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
                        LandmarkProto.NormalizedLandmarkList poseLandmarks = LandmarkProto.NormalizedLandmarkList.parseFrom(landmarksRaw);
//                        Log.v("AAA", String.valueOf(packet));
//                        LandmarkProto.NormalizedLandmarkList poseLandmarks =
//                                PacketGetter.getProto(packet, LandmarkProto.NormalizedLandmarkList.class);
//                        Log.v(
//                                "AAA_LH",
//                                "[TS:"
//                                        + packet.getTimestamp()
//                                        + "] "
//                                        + getPoseLandmarksDebugString(poseLandmarks));
                        LandmarkMap.put("leftHand",getPoseLandmarksDebugAry(poseLandmarks));
                    } catch (InvalidProtocolBufferException e) {
                        Log.e("AAA", "Failed to get proto.", e);
                    }

                });
        processor
                .addPacketCallback("right_hand_landmarks", (packet) -> {
                    try {
//                        Log.d("ㄱ", "right");
                        byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
                        LandmarkProto.NormalizedLandmarkList poseLandmarks = LandmarkProto.NormalizedLandmarkList.parseFrom(landmarksRaw);
//                        Log.v("AAA", String.valueOf(packet));
//                        LandmarkProto.NormalizedLandmarkList poseLandmarks =
//                                PacketGetter.getProto(packet, LandmarkProto.NormalizedLandmarkList.class);
//                        Log.v(
//                                "AAA_RH",
//                                "[TS:"
//                                        + packet.getTimestamp()
//                                        + "] "
//                                        + getPoseLandmarksDebugString(poseLandmarks));
                        LandmarkMap.put("rightHand",getPoseLandmarksDebugAry(poseLandmarks));
                    } catch (InvalidProtocolBufferException e) {
                        Log.e("AAA", "Failed to get proto.", e);
                    }

                });

        processor
                .getVideoSurfaceOutput()
                .setFlipY(
                        applicationInfo.metaData.getBoolean("flipFramesVertically", FLIP_FRAMES_VERTICALLY));


        PermissionHelper.checkAndRequestCameraPermissions(this);


// Flask의 REST API와의 연결 (목적 : 카메라로 인식한 좌표값 API에게 보내서 계산된 좌표값을 받아오는 코드)




    }


    // 좌표값 숫자 배열로 변환해서 반환하는 코드
    private static float[][] getPoseLandmarksDebugAry(LandmarkProto.NormalizedLandmarkList poseLandmarks){
        float[][] poseLandmarkAry = new float[poseLandmarks.getLandmarkCount()][3];
        int landmarkIndex = 0;
        for (LandmarkProto.NormalizedLandmark landmark : poseLandmarks.getLandmarkList()) {
            poseLandmarkAry[landmarkIndex][0] = landmark.getX();
            poseLandmarkAry[landmarkIndex][1] = landmark.getY();
            poseLandmarkAry[landmarkIndex][2] = landmark.getZ();
            ++landmarkIndex;
        }
        return poseLandmarkAry;
    }

    @Override
    protected void onResume() {
        super.onResume();
        converter = new ExternalTextureConverter(eglManager.getContext());
        converter.setFlipY(
                applicationInfo.metaData.getBoolean("flipFramesVertically", FLIP_FRAMES_VERTICALLY));
        converter.setConsumer(processor);
        if (PermissionHelper.cameraPermissionsGranted(this)) {
            startCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        converter.close();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    protected void onCameraStarted(SurfaceTexture surfaceTexture) {
        previewFrameTexture = surfaceTexture;
        // Make the display view visible to start showing the preview. This triggers the
        // SurfaceHolder.Callback added to (the holder of) previewDisplayView.
        previewDisplayView.setVisibility(View.VISIBLE);
    }

    protected Size cameraTargetResolution() {
        return null; // No preference and let the camera (helper) decide.
    }

    public void startCamera() {
        cameraHelper = new CameraXPreviewHelper();
        cameraHelper.setOnCameraStartedListener(
                surfaceTexture -> {
                    onCameraStarted(surfaceTexture);
                });
        CameraHelper.CameraFacing cameraFacing =
                applicationInfo.metaData.getBoolean("cameraFacingFront", false)
                        ? CameraHelper.CameraFacing.BACK
                        : CameraHelper.CameraFacing.FRONT;
        cameraHelper.startCamera(
                this, cameraFacing, /*surfaceTexture=*/ null, cameraTargetResolution());
    }

    protected Size computeViewSize(int width, int height) {
        return new Size(width, height);
    }

    protected void onPreviewDisplaySurfaceChanged(
            SurfaceHolder holder, int format, int width, int height) {
        // (Re-)Compute the ideal size of the camera-preview display (the area that the
        // camera-preview frames get rendered onto, potentially with scaling and rotation)
        // based on the size of the SurfaceView that contains the display.
        Size viewSize = computeViewSize(width, height);
        Size displaySize = cameraHelper.computeDisplaySizeFromViewSize(viewSize);
        boolean isCameraRotated = cameraHelper.isCameraRotated();

        // Connect the converter to the camera-preview frames as its input (via
        // previewFrameTexture), and configure the output width and height as the computed
        // display size.
        converter.setSurfaceTextureAndAttachToGLContext(
                previewFrameTexture,
                isCameraRotated ? displaySize.getHeight() : displaySize.getWidth(),
                isCameraRotated ? displaySize.getWidth() : displaySize.getHeight());
    }

    private void setupPreviewDisplayView() {
        previewDisplayView.setVisibility(View.GONE);
        ViewGroup viewGroup = findViewById(R.id.preview_display_layout);
        viewGroup.addView(previewDisplayView);

        previewDisplayView
                .getHolder()
                .addCallback(
                        new SurfaceHolder.Callback() {
                            @Override
                            public void surfaceCreated(SurfaceHolder holder) {
                                processor.getVideoSurfaceOutput().setSurface(holder.getSurface());
                            }

                            @Override
                            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                                onPreviewDisplaySurfaceChanged(holder, format, width, height);
                            }

                            @Override
                            public void surfaceDestroyed(SurfaceHolder holder) {
                                processor.getVideoSurfaceOutput().setSurface(null);
                            }
                        });
    }
    
//    tflite 관련 코드
    private Interpreter getTfliteInterpreter(String modelPath){
        try{
            return new Interpreter(loadModelFile(holistic_activity.this, modelPath));
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
    public MappedByteBuffer loadModelFile(Activity activity, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength);
    }
}

