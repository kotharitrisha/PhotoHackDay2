
package com.iqengines.demo;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Size;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

public class Preview extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final String TAG = Preview.class.getSimpleName();
    
    private static long AUTO_FOCUS_INTERVAL = 1500;

    private SurfaceHolder mHolder;
    
    private Handler mHandler;
    
    private Boolean mAutoFocus;

    Camera mCamera;

    Size mPreviewSize;

    private int mPreviewFormat;

    private boolean mThreadRun;

    private byte[] mLastFrameCopy;

    private byte[] mLastFrameCopyOut;

    private FrameReceiver mFrameReceiver;

    private Size mFramePreviewSize;

    public interface FrameReceiver {
        public void onFrameReceived(byte[] frameBuffer, Size framePreviewSize);
    }

    public Preview(Context context) {
        this(context, null);
    }

    public Preview(Context context, AttributeSet attrs) {
        super(context, attrs);

        mHandler = new Handler();
        
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    // @Override
    // protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // // We purposely disregard child measurements because act as a
    // // wrapper to a SurfaceView that centers the camera preview instead
    // // of stretching it.
    // final int width = resolveSize(getSuggestedMinimumWidth(),
    // widthMeasureSpec);
    // final int height = resolveSize(getSuggestedMinimumHeight(),
    // heightMeasureSpec);
    // setMeasuredDimension(width, height);
    // 
    // if (mSupportedPreviewSizes != null) {
    // mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width,
    // height);
    // }
    // }

    private Size getOptimalSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.2;
        double targetRatio = (double) w / h;
        Log.d(TAG, "target view size: " + w + "x" + h + ", target ratio=" + targetRatio);
        
        if (sizes == null)
            return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;
        int targetWidth = w;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            boolean fitToView = size.width <= w && size.height <= h;
            Log.d(TAG, "Supported preview size: " + size.width + "x" + size.height + ", ratio="
                    + ratio + ", fitToView=" + fitToView);
            if (!fitToView) {
                // we can not use preview size bigger than surface dimensions
                // skipping
                continue;
            }
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }

            double hypot = Math.hypot(size.height - targetHeight, size.width - targetWidth);
            if (hypot < minDiff) {
                optimalSize = size;
                minDiff = hypot;
            }
        }

        if (optimalSize == null) {
            Log.d(TAG,
                    "Cannot find preview that matchs the aspect ratio, ignore the aspect ratio requirement");

            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (size.width > w || size.height > h) {
                    // we can not use preview size bigger than surface
                    // dimensions
                    continue;
                }

                double hypot = Math.hypot(size.height - targetHeight, size.width - targetWidth);
                if (hypot < minDiff) {
                    optimalSize = size;
                    minDiff = hypot;
                }
            }
        }

        if (optimalSize == null) {
            throw new RuntimeException("Unable to determine optimal preview size");
        }
        Log.d(TAG, "optimalSize.width=" + optimalSize.width + ", optimalSize.height="
                + optimalSize.height);

        return optimalSize;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mCamera == null) {
            Log.e(TAG, "mCamera == null !");        
            return;
        }

        Camera.Parameters params = mCamera.getParameters();
        mPreviewFormat = params.getPreviewFormat();
        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        int angle;
        switch (display.getRotation()) {
            case Surface.ROTATION_0:
                angle = 90;
                break;
            case Surface.ROTATION_90:
                angle = 0;
                break;
            case Surface.ROTATION_180:
                angle = 270;
                break;
            case Surface.ROTATION_270:
                angle = 180;
                break;
            default:
                throw new AssertionError("Wrong surface rotation value");
        }
        setDisplayOrientation(params, angle);
        
        if (mPreviewSize == null) {
            // h and w get inverted on purpose
            mPreviewSize = getOptimalSize(params.getSupportedPreviewSizes(), width > height ? width
                    : height, width > height ? height : width);
        }
        params.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        
        mCamera.setParameters(params);
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            Log.e(TAG, "Can't set preview display", e);
        }

        startPreview();

        if (mFrameReceiver != null)
            setFrameReceiver(mFrameReceiver);
    }

    
    
    class AutoFocusRunnable implements Runnable {
    	
    	
    	@Override
    	public void run() {
        	
        	if (mAutoFocus){
        	
        		if (mCamera != null) {
        			try {
               	
        				mCamera.autoFocus(new AutoFocusCallback() {
        					@Override
        					public void onAutoFocus(boolean success, Camera camera) {
        						mHandler.postDelayed(AutoFocusRunnable.this, AUTO_FOCUS_INTERVAL);
        					}
        				});
                		} catch (Exception e) {
                		Log.w(TAG, "Unable to auto-focus", e);
                    	mHandler.postDelayed(AutoFocusRunnable.this, AUTO_FOCUS_INTERVAL);
                	}
            	}
        	}
        }

		
    };
    
    void startPreview() {
    	mAutoFocus=true;
        mCamera.startPreview();
        Log.i(TAG,"AUTOFOCUS REUP");
        new AutoFocusRunnable().run();
    }
    
    void stopPreview() {
    	mAutoFocus=false;
    	mHandler.removeCallbacks(this);
    	mCamera.cancelAutoFocus();
    	Log.i(TAG, "start stoppreview");
    	mCamera.stopPreview();
    	Log.i(TAG,"stoppreview finished");
    }
    

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
        	mAutoFocus = true;
            mCamera = Camera.open();
        } catch (RuntimeException e) {
            Toast.makeText(getContext(),
                    "Unable to connect to camera. " + "Perhaps it's being used by another app.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    	
    	mThreadRun = false;
    	
        if (mCamera != null) {
        	
            synchronized (this) {
            	
            	mCamera.setPreviewCallback(null);
            	Log.i(TAG, "Start synchronized !!!!!!!!!!!!!!!!!!!!!");            	
                mCamera.stopPreview();            	
            	Log.i(TAG,"Preview Stopped");
                mCamera.release();
                Log.i(TAG, "Camera Release !!!!!!!!!!!!!!!!!!!!!");
                mCamera = null;
                Log.i(TAG, "Camera Detroyed !!!!!!!!!!!!!!!!!!!!!");
                
            }
        }
    }
    
    public boolean isSurfaceViewNull(){
    	
    	if(this.mHolder==null)
    		return true;
    	else
    		return false;
    	
    }

    private void setDisplayOrientation(Camera.Parameters params, int angle) {
        try {
            Method method = mCamera.getClass().getMethod("setDisplayOrientation", new Class[] {
                int.class
            });
            if (method != null)
                method.invoke(mCamera, new Object[] {
                    angle
                });
        } catch (Exception e) {
            Log.d(TAG, "Can't call Camera.setDisplayOrientation on this device, trying another way");
            if (angle == 90 || angle == 270)
                params.set("orientation", "portrait");
            else if (angle == 0 || angle == 180)
                params.set("orientation", "landscape");
        }
        params.setRotation(angle);
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
     
        mThreadRun = true;
        while (mThreadRun) {
            try {
                byte[] frameCopy;
                synchronized (this) {
                    this.wait();

                }
                
                if (mPreviewFormat != ImageFormat.NV21) {
                    Log.e(TAG, "Unknown preview format: " + mPreviewFormat);
                    continue;
                }
                
                frameCopy = getLastFrameCopy();
                
                if (mFrameReceiver != null) {
                	
                	
                    mFrameReceiver.onFrameReceived(frameCopy, mFramePreviewSize);
                    
                    
                }
                
                Thread.sleep(50); // let other threads time to run
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            
        }
    }

    public void setFrameReceiver(FrameReceiver receiver) {
        if (DemoActivity.DEBUG) {
            Log.d(TAG, "setFrameReceiver");
        }

        mFrameReceiver = receiver;

        if (mCamera != null) {
            if (mFrameReceiver != null) {
                (new Thread(this)).start();

                // wait until thread is in main loop
                while (!mThreadRun) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                mThreadRun = false;
            }

            int bitsPerPixel = 12;
            byte[] previewBuffer = new byte[mCamera.getParameters().getPreviewSize().height
                    * mCamera.getParameters().getPreviewSize().width * bitsPerPixel / 8];
            mLastFrameCopy = new byte[previewBuffer.length];
            mLastFrameCopyOut = new byte[previewBuffer.length];
            mCamera.addCallbackBuffer(previewBuffer);

            mCamera.setPreviewCallbackWithBuffer(mFrameReceiver == null ? null
                    : new Camera.PreviewCallback() {

                        @Override
                        public void onPreviewFrame(byte[] data, Camera camera) {
                  

                            if (data == null) {
                              
                                Log.w(TAG, "Skiping empty frame");
                                return;
                            }

                            copyLastFrame(data);

                            synchronized (Preview.this) {
                                mFramePreviewSize = mCamera.getParameters().getPreviewSize();
                                Preview.this.notifyAll();
                                
                            }
                            
                            mCamera.addCallbackBuffer(data);
                        }

                    });
        }
    }

    private Object mLastFrameCopyLock = new Object();
    private void copyLastFrame(byte[] frame) {
        synchronized (mLastFrameCopyLock) {
        	
            System.arraycopy(frame, 0, mLastFrameCopy, 0, frame.length);
            
        }
    }

    public byte[] getLastFrameCopy() {
        synchronized (mLastFrameCopyLock) {
            System.arraycopy(mLastFrameCopy, 0, mLastFrameCopyOut, 0, mLastFrameCopy.length);
            return mLastFrameCopyOut;
        }
    }
    
    

    static Bitmap convertFrameToBmp(byte[] frame, Size framePreviewSize) {
        Log.d(TAG, "frame.length=" + frame.length + ", framePreviewSize.width="
                + framePreviewSize.width + ", framePreviewSize.height=" + framePreviewSize.height);

        final int frameWidth = framePreviewSize.width;
        final int frameHeight = framePreviewSize.height;
        final int frameSize = frameWidth * frameHeight;
        int[] rgba = new int[frameSize];
        for (int i = 0; i < frameHeight; ++i)
            for (int j = 0; j < frameWidth; ++j) {
                int y = (0xff & ((int) frame[i * frameWidth + j]));
                int u = (0xff & ((int) frame[frameSize + (i >> 1) * frameWidth + (j & ~1) + 0]));
                int v = (0xff & ((int) frame[frameSize + (i >> 1) * frameWidth + (j & ~1) + 1]));
                y = y < 16 ? 16 : y;

                int r = Math.round(1.164f * (y - 16) + 1.596f * (v - 128));
                int g = Math.round(1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = Math.round(1.164f * (y - 16) + 2.018f * (u - 128));

                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);

                rgba[i * frameWidth + j] = 0xff000000 + (b << 16) + (g << 8) + r;
            }

        Bitmap bmp = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0, frameWidth, 0, 0, frameWidth, frameHeight);
        return bmp;
    }
   

}
