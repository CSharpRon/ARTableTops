package com.ucfknights.artabletops;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

public class MainGameActivity extends AppCompatActivity {

    private static final String TAG = MainGameActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ModelRenderable curRen;
    private ModelRenderable roverRenderable;
    private ModelRenderable carRenderable;
    private ModelRenderable buzzRenderable;
    private Anchor last;
    private Anchor cur;
    private boolean resize = true;

    private LinearLayout imageList;
    private HorizontalScrollView mainImgView;
    private GestureDetector gDetector;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_game);

        imageList = findViewById(R.id.model_images);

        // Set images and add Gesture Detection
        if(imageList != null) {

            LayoutInflater inflater = LayoutInflater.from(this);

            for (int i = 0; i < 10; i++) {

                try {
                    View v = inflater.inflate(R.layout.image, imageList, false);

                    ImageView imgView = v.findViewById(R.id.imageView);
                    imgView.setImageResource(R.drawable.ic_launcher_background);

                    imageList.addView(v);
                } catch (InflateException e) {

                }
            }

            mainImgView = findViewById(R.id.main_horizontal_scroll_view);
            gDetector = new GestureDetector(this, new ImgListGestureDetector(imageList));

            mainImgView.setOnTouchListener(touchListener);
        }

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_main_game);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
                .setSource(this, R.raw.rover3)
                .build()
                .thenAccept(renderable -> roverRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
        ModelRenderable.builder()
                .setSource(this, R.raw.car)
                .build()
                .thenAccept(renderable -> carRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
        ModelRenderable.builder()
                .setSource(this, R.raw.buzz)
                .build()
                .thenAccept(renderable -> buzzRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (curRen == null) {
                        return;
                    }

                    // Create the Anchor.
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    // Create the transformable andy and add it to the anchor.
                    TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                    andy.setParent(anchorNode);
                    andy.getScaleController().setMinScale(0.1f);
                    andy.getScaleController().setMaxScale(2.0f);

                    andy.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));
                    andy.setRenderable(curRen);
                    andy.select();
                    cur=anchor;

                    andy.setOnTapListener(new Node.OnTapListener() {
                        @Override
                        public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
                            last = ((AnchorNode) hitTestResult.getNode().getParent()).getAnchor();
                            //findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
                        }
                    });
                });

//        final Button button = findViewById(R.id.delete_button);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (last != null){
//                    last.detach();
//                    last=null;
//
//                }
//                else if(cur != null){
//                    cur.detach();
//                    cur=null;
//                }
//            }
//        });
//        final Button rover_bttn = findViewById(R.id.rover_bttn);
//        rover_bttn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                curRen=roverRenderable;
//                resize=true;
//            }
//        });
    }

    // Passes everything on to the gesture detector.
    View.OnTouchListener touchListener = (v, event) -> {

        return gDetector.onTouchEvent(event);

    };

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    class ImgListGestureDetector extends GestureDetector.SimpleOnGestureListener {

        private LinearLayout imgList;

        ImgListGestureDetector(LinearLayout list) {
            imgList = list;
        }

        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {

            // Upward Scroll
            if (distanceY > 0 && distanceY > 20) {
                if (imgList.getVisibility() != View.VISIBLE) {
                    imgList.setVisibility(View.VISIBLE);
                }
            } else if (distanceY < 0 && distanceY < -20) {
                if (imgList.getVisibility() != View.INVISIBLE) {
                    imgList.setVisibility(View.INVISIBLE);
                }
            }

            return true;
        }
    }
}
