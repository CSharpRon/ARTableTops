package com.ucfknights.artabletops;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
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

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        // region Generate Renderables
        // Here, we are manually loading and rendering the objects into memory so that they are readily available to the user
        ModelRenderable.builder()
                .setSource(this, R.raw.rover3)
                .build()
                .thenAccept(renderable -> roverRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load rover resource", Toast.LENGTH_LONG);
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
                                    Toast.makeText(this, "Unable to load car resource", Toast.LENGTH_LONG);
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
                                    Toast.makeText(this, "Unable to load buzz lightyear resource", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
        // endregion

        // region Add Images to Scroll View
        imageList = findViewById(R.id.model_images);

        // Set images and add Gesture Detection
        if(imageList != null) {

            LayoutInflater inflater = LayoutInflater.from(this);
            String[] models = getResources().getStringArray(R.array.models);

            // Dynamically add the images and the logic for the buttons behind them
            for (int i = 0; i < models.length; i++) {

                try {
                    View v = inflater.inflate(R.layout.image, imageList, false);

                    ImageView imgView = v.findViewById(R.id.imageView);
                    imgView.setImageResource(GetImageResource(models[i]));
                    imgView.setTag(models[i]);
                    imgView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ClearHighlights(imageList);
                            ((ImageView)v).getDrawable().setColorFilter(0x77000000,PorterDuff.Mode.SRC_ATOP);

                            if( !((String)v.getTag()).equals("delete")) {
                                curRen = GetRenderable((String) v.getTag());
                                resize = (String) v.getTag() == "buzz_lightyear" ? false : true;
                            } else {
                                if (last != null){
                                    last.detach();
                                    last=null;
                                }
                                else if(cur != null){
                                    cur.detach();
                                    cur=null;
                                }
                            }
                        }
                    });

                    imageList.addView(v);
                } catch (InflateException e) {

                }
            }

            mainImgView = findViewById(R.id.main_horizontal_scroll_view);
            gDetector = new GestureDetector(this, new ImgListGestureDetector(imageList));

            mainImgView.setOnTouchListener(touchListener);
        }

        // endregion

        // region Tap To AR Functionality
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
        // endregion

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

    // Remove any highlights on images that would show them as selected
    private void ClearHighlights(LinearLayout images) {
        int imgCount = images.getChildCount();
        for (int j = 0; j < imgCount; j++) {

            View v = images.getChildAt(j);

            // Each Linear Layout in that view should have only one child, the ImageView
            if(((LinearLayout)v).getChildCount() == 1) {
                ((ImageView)((LinearLayout) v).getChildAt(0)).getDrawable().clearColorFilter();
            }
        }
    }

    // When passed in the name of a particular model, return the corresponding resource image
    private @DrawableRes int GetImageResource(String modelName) {

        switch(modelName) {
            case "buzz_lightyear":
                return R.drawable.buzz_lightyear;
            case "car":
                return R.drawable.car;
            case "discovery_rover":
                return R.drawable.discovery_rover;
            case "delete":
                return R.drawable.delete;
            default:
                break;
        }

       return R.drawable.buzz_lightyear;
    }

    // When passed in the name of a particular model, return the rendered object
    private ModelRenderable GetRenderable(String modelName) {

        switch(modelName) {
            case "buzz_lightyear":
                return buzzRenderable;
            case "car":
                return carRenderable;
            case "discovery_rover":
                return roverRenderable;
            default:
                break;
        }

        return buzzRenderable;
    }

    // Unimplemented as the flick gestures kept interfering with the horizontal scroll
    class ImgListGestureDetector extends GestureDetector.SimpleOnGestureListener {

        private LinearLayout imgList;
        private int epsilon = 120;

        ImgListGestureDetector(LinearLayout list) {
            imgList = list;
        }

//        @Override
//        public boolean onFling(MotionEvent e1, MotionEvent e2,
//                               float velocityX, float velocityY) {
//
//            if(e1.getY() - e2.getY() > epsilon && Math.abs(velocityY) > 200) {
////
////                if (imgList.getVisibility() != View.VISIBLE) {
////                    imgList.setVisibility(View.VISIBLE);
////                }
////                return false; // Swipe Up
//            }  else if (e2.getY() - e1.getY() > epsilon && Math.abs(velocityY) > 200) {
////                if (imgList.getVisibility() != View.INVISIBLE) {
////                    imgList.setVisibility(View.INVISIBLE);
////                }
////                return false; // Top to bottom
//            }
//            return false;
//        }

//        @Override
//        public boolean onScroll(MotionEvent e1, MotionEvent e2,
//                                float distanceX, float distanceY) {
//
//            int epsilon = 60;
//
//            // Upward Scroll
//            if (distanceY > 0 && distanceY > epsilon) {
//                if (imgList.getVisibility() != View.VISIBLE) {
//                    imgList.setVisibility(View.VISIBLE);
//                }
//           } //else if (distanceY < 0 && distanceY < -epsilon) {
////                if (imgList.getVisibility() != View.INVISIBLE) {
////                    imgList.setVisibility(View.INVISIBLE);
////                }
////            }
//
//            return true;
//        }
    }
}
