package com.iqengines.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.iqengines.sdk.IQE;
import com.iqengines.sdk.IQE.OnResultCallback;
import com.iqengines.sdk.Utils;

public class DemoActivity extends Activity implements OnInitListener {
	TextToSpeech tts;
	private int DATA_CHECKING = 0;  
	String text;
	/**
	 * Account settings. You can obtain the required keys after you've signed up for visionIQ.
	 */

	// Insert your API key here (find it at iengines.com --> developer center --> settings).
    static final String KEY = "93882b5d16e940c0a0c2439fbcb761cb";
    // Insert your secret key here (find it at iengines.com --> developer center --> settings).
    static final String SECRET = "45f8f2bc39ea40fb829dcae004f37cd9";

    /**
     * LOCAL search Settings.
     */
    
    // Activates the local search if the hardware supports it.
    static final boolean SEARCH_OBJECT_LOCAL = false && isHardwareLocalSearchCapable();    
    // Activates the continuous local search if the hardware supports it.
    static boolean SEARCH_OBJECT_LOCAL_CONTINUOUS = false && isHardwareLocalSearchCapable();
   
    /**
     * REMOTE search Settings.
     */
    
    // Maximum duration of a remote search.
    static final long REMOTE_MATCH_MAX_DURATION = 10000;
    // Activates the remote search.
    static final boolean SEARCH_OBJECT_REMOTE = true;


    
    private String localSearchCapableStr = null;    
    
    static final boolean PROCESS_ASYNC = true;

    private static final String FIRST_START_SHARED_PREF = "FIRST_START_SHARED_PREF";

    static final boolean DEBUG = true;
    
    private static final String TAG = DemoActivity.class.getSimpleName();
    
    private Handler handler;

    private Preview preview;

    private ImageButton remoteMatchButton;

    private ImageButton btnShowList;
    
    private ImageButton infoButton;

  //  private ImageView frozenPreview;

  // private ImageView targetArea;

    List<HistoryItem> history;

    private HistoryItemDao historyItemDao;

    static HistoryListAdapter historyListAdapter;

    static IQE iqe;

    private AtomicBoolean capturing = new AtomicBoolean(false);

    private AtomicBoolean localMatchInProgress = new AtomicBoolean(false);

    private AtomicBoolean remoteMatchInProgress = new AtomicBoolean(false);

    private AtomicBoolean activityRunning = new AtomicBoolean(false);

    
    /**
     * Checks whether local search is possible on this hardware.
     * 
     * @return A {@link Boolean} true if local search is possible.
     */
    
    
    private static boolean isHardwareLocalSearchCapable() {
        boolean res = Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1
                && android.os.Build.CPU_ABI.equals("armeabi-v7a");
        Log.d(TAG, "chipset instruction set: " + android.os.Build.CPU_ABI + ", android version:"
                + Build.VERSION.SDK_INT + " => isHardwareLocalSearchCapable=" + res);
        return res;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        handler = new Handler();

        initHistory();
        initHistoryListView();
        initIqSdk();
        initUI();

        localSearchCapableStr = savedInstanceState != null ? savedInstanceState
                .getString("localSearchCapable") : null;
        if (SEARCH_OBJECT_LOCAL_CONTINUOUS && localSearchCapableStr == null) {
            testContinousLocalSearchCapability();
        } else {
            if (SEARCH_OBJECT_LOCAL_CONTINUOUS) {
                boolean localSearchCapable = Boolean.parseBoolean(localSearchCapableStr);
                if (!localSearchCapable) {
                    Toast.makeText(
                            DemoActivity.this,
                            "This device is not capable to perform local continous search so this feature is disabled",
                            Toast.LENGTH_LONG);
                }
                SEARCH_OBJECT_LOCAL_CONTINUOUS &= localSearchCapable;
            }
        }
        
        launchTutorialIfNeeded();
    }

    
    private void launchTutorialIfNeeded() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE); 
        boolean isFirstStart = preferences.getBoolean(FIRST_START_SHARED_PREF, true);
        if (isFirstStart) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(FIRST_START_SHARED_PREF, false);
            editor.commit();
            
            Intent intent = new Intent(DemoActivity.this, TutorialActivity.class);
            startActivity(intent);
        } 
    }

    
    private void testContinousLocalSearchCapability() {
        SEARCH_OBJECT_LOCAL_CONTINUOUS = false;
        
        final ProgressDialog pd = showCenteredProgressDialog("Device local search capability is being determined");

        final Runnable testTimeExpireRunnable = new Runnable() {
            @Override
            public void run() {
                pd.dismiss();
                Toast.makeText(
                        DemoActivity.this,
                        "This device is not capable to perform local continous search so this feature is disabled",
                        Toast.LENGTH_LONG).show();
            }
        };

        new Thread() {
            public void run() {
                long timeSpentOnLocalSearch = iqe.testLocalSearchCapability();
                pd.dismiss();
                SEARCH_OBJECT_LOCAL_CONTINUOUS = timeSpentOnLocalSearch <= IQE.MAX_TEST_LOCAL_SEARCH_TIME;
                Log.d(TAG, "timeSpentOnLocalSearch=" + timeSpentOnLocalSearch
                        + " => SEARCH_OBJECT_LOCAL_CONTINUOUS=" + SEARCH_OBJECT_LOCAL_CONTINUOUS);
                if (SEARCH_OBJECT_LOCAL_CONTINUOUS) {
                    handler.removeCallbacks(testTimeExpireRunnable);
                    localSearchCapableStr = "true";
                }
            }
        }.start();

        handler.postDelayed(testTimeExpireRunnable, IQE.MAX_TEST_LOCAL_SEARCH_TIME);
    }

    
    private void initHistory() {
        historyItemDao = new HistoryItemDao(this);

        history = historyItemDao.loadAll();
        if (history == null) {
            history = new ArrayList<HistoryItem>();
        }
    }

    
    private void initHistoryListView() {
        historyListAdapter = new HistoryListAdapter(this);

        btnShowList = (ImageButton) findViewById(R.id.historyButton);
        btnShowList.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DemoActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });
    }

    
    private void initIqSdk() {
        iqe = new IQE(this, SEARCH_OBJECT_REMOTE, SEARCH_OBJECT_LOCAL_CONTINUOUS
                || SEARCH_OBJECT_LOCAL, KEY, SECRET);
    }

  
   
	@SuppressWarnings("unused") 
	private void initUI() {
        preview = (Preview) findViewById(R.id.preview);

        infoButton = (ImageButton) findViewById(R.id.infoButton);
        infoButton.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DemoActivity.this, TutorialActivity.class);
                startActivity(intent);
            }
        });
        
        remoteMatchButton = (ImageButton) findViewById(R.id.capture);
        remoteMatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!remoteMatchInProgress.get()) {
                    if (preview.mCamera == null) {
                        return;
                    }
                    
//                    final ProgressDialog fPd = showCenteredProgressDialog("Focusing...");
//
//                    preview.mCamera.autoFocus(new AutoFocusCallback() {
//                        @Override
//                        public void onAutoFocus(boolean success, Camera camera) {
//                            fPd.dismiss();
                            // freeze last frame available on preview
                            YuvImage yuv = new YuvImage(preview.getLastFrameCopy(),ImageFormat.NV21,
                                    preview.mPreviewSize.width,preview.mPreviewSize.height,null);
                            freezePreview();
                            remoteMatchInProgress.set(true);
                            remoteMatchButton.setImageResource(R.drawable.btn_ic_camera_shutter);
                            pd = showCenteredProgressDialog("Uploading...");
                            processImageLocallyAndRemotely(yuv);
//                        }
//                    });
                }
            }
        });
        if (!SEARCH_OBJECT_REMOTE && !SEARCH_OBJECT_LOCAL) {
            remoteMatchButton.setVisibility(View.GONE);
        }
    }

    
    public void onSaveInstanceState(Bundle stateBundle) {
        stateBundle.putString("localSearchCapable", localSearchCapableStr);
        super.onSaveInstanceState(stateBundle);
    }

    
    @Override
    public void onResume() {
    	
    	
        super.onResume();
        activityRunning.set(true);
        iqe.resume();
       
        startLocalContinuousCapture();
        
    }

    
    @Override
    public void onPause() {

        stopLocalContinuousCapture();
        
        activityRunning.set(false);

        iqe.pause();

        historyItemDao.saveAll(history);

        super.onPause();
    }


    private void startLocalContinuousCapture() {
    	
        Preview.FrameReceiver receiver = new DemoFrameReceiver();

        capturing.set(true);
        preview.setFrameReceiver(receiver);
    }

    
    private void stopLocalContinuousCapture() {
        capturing.set(false);
        preview.setFrameReceiver(null);
    }

    
    private void freezePreview() {
           	preview.stopPreview();       	
            }
    
    

    
    private void unfreezePreview() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                preview.mCamera.startPreview();
            }
        });
    }

    
    private void reenableRemoteMatch() {
        remoteMatchInProgress.set(false);
        unfreezePreview();
        if (pd != null) {
            pd.dismiss();
        }
        remoteMatchButton.setImageResource(R.drawable.ic_camera);
    }

    
    private ProgressDialog pd;

    private String lastPostedQid = null;

    
    // Called when results from a remote research are ready.
    private OnResultCallback onRemoteResultCallback = new OnResultCallback() {
    	
        @Override
        public void onResult(final String queryId, final String objId, final String objName,
                String objMeta, boolean remoteMatch, Exception e) {
        	
        	// If no exceptions then process the match data.
            if (e == null) {
            	
                if (queryId.equals(lastPostedQid)) {
                    handler.removeCallbacks(postponedToastAction);
                }
                
                Uri uri = null;
                // match's Metadata set as URI.
                if (objMeta != null) {
                	
                    try {
                        uri = Uri.parse(objMeta);
                    } catch (Exception e1) {
                        uri = null;
                    }
                }
                // if no Metadata : match's name set as URI.
                if (uri == null) {
                	
                    if (objName != null) {
                    	
                        try {
                            uri = Uri.parse(objName);
                        } catch (Exception e1) {
                            uri = null;
                            
                        }
                    }
                }
                Log.v("jchun", "META DATA?: " + objMeta);
                
                final Uri fUri = uri;
                final String meta = objMeta;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                    	// process and display the results
                        processSearchResult(queryId, objName, fUri, false, meta);
                    }
                });
            } 
            // An exception occurred : no match found.
            else {
            	
                if (e instanceof IOException) {
                    Log.w(TAG, "Server call failed", e);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            handler.removeCallbacks(postponedToastAction);
                            Toast.makeText(
                                    DemoActivity.this,
                                    "Unable to connect to the server. "
                                            + "Check your intenet connection.", Toast.LENGTH_LONG)
                                    .show();
                            reenableRemoteMatch();
                        }
                    });
                } else {
                    Log.e(TAG, "Unable to complete remote search", e);
                }
                
            }
        }

        
        @Override
        public void onQueryIdAssigned(String queryId, File imgFile) {
            createHistoryItem(queryId, imgFile);
        }
        
    };

    private void createHistoryItem(String qid, File imgFile) {
    	
    	Log.i(TAG, "start create thumb");
        Bitmap origBmp = BitmapFactory.decodeFile(imgFile.getPath());
        Bitmap thumb = transformBitmapToThumb(origBmp);
        Log.i(TAG, "stop create thumb");
        
        HistoryItem item = new HistoryItem();
        item.id = lastPostedQid = qid;
        item.label = "Searching...";
        item.uri = null;
        item.thumb = thumb;
        history.add(item);
        
        if (DEBUG) {
            Log.d(TAG, "History item created for qid: "+qid);
        }
    }

    private Runnable postponedToastAction;

    /**
     * Method that starts a local and remote research on the phone.
     * onResultCallback is called when result is ready.
     * 
     * @param bmp
     * 		  A {@link Bitmap} image to process.
     */
    
    private void processImageLocallyAndRemotely(final YuvImage yuv) {

        new Thread() {
            public void run() {
                postponedToastAction = new Runnable() {
                    public void run() {
                        reenableRemoteMatch();
                        Toast.makeText(
                                DemoActivity.this,
                                "This may take a minute... We will notify you when your photo is recognized.",
                                Toast.LENGTH_LONG).show();
                    }
                };
                handler.postDelayed(postponedToastAction, REMOTE_MATCH_MAX_DURATION);
                iqe.searchWithImage(yuv, onRemoteResultCallback);

                handler.post(new Runnable() {
                    public void run() {
                        pd.setMessage("Searching...");
                    }
                });
            }
        }.start();
    }

    /**
     * Method that starts a local research on the phone.
     * onResultCallback is called when result is ready.
     * 
     * @param yuv
     * 		  A {@link YuvImage} to process.
     * 
     */ 
    
    private void processImageNative(final YuvImage yuv) {
    	
        Thread.yield();
        // starts the local search.
        iqe.searchWithImageLocal(yuv, new OnResultCallback() {
            private File imgFile;

            @Override
            public void onQueryIdAssigned(String queryId, File imgFile) {
                this.imgFile = imgFile;
                // we create history items only for successful continuous searches.
            }

            // Called when match found.
            @Override
            public void onResult(final String queryId, String objId, final String objName,
                    String objMeta, boolean remoteMatch, Exception e) {
            	
                if (e != null) {
                    Log.e(TAG, "Unable to complete local search", e);
                    return;
                }

                //Match found by the local search.
                if (objId != null) {
                    createHistoryItem(queryId, imgFile);

                    Uri uri = null;
                    //Match's Metadata set as Uri. 
                    if (objMeta != null) {
                        try {
                            uri = Uri.parse(objMeta);
                        } catch (Exception e1) {
                            uri = null;
                        }
                    }

                    final String meta = objMeta;
                    final Uri fUri = uri;
                    handler.post(new Runnable() {
                        public void run() {
                        	
                            processSearchResult(queryId, objName, fUri, true, meta);
                        }
                    });
                } else {
                	// No match found.
                    if (DEBUG)
                        Log.d(TAG, "No match detected");
                    localMatchInProgress.set(false);
                }
            }

        });
        Thread.yield();
    }

    /**
     * Checks if the Uri provided is good and displays it.
     * 
     * @param a
     * 		  The current {@link Activity}.
     * @param uri
     * 		  The {@link Uri} to analyze.
     * @return
     */
    
    static boolean processMetaUri(Activity a, Uri uri) {
        if (uri != null && uri.toString().length() > 0) {
            try {
                a.startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "Unable to open view for this meta: " + uri.toString(), e);
                //Toast.makeText(a, "Unable to open view for this meta-field: " + uri.toString(),
                  //      Toast.LENGTH_LONG).show();
                return false;
            }
        } else {
            return false;
        }
    } 
    
    /**
     * Processes the data from the match found and manage the UI.
     * 
     * @param searchId
     * 		  A {@link String} that identifies the query.
     * @param label
     * 		  A {@link String} that gives the match found label.
     * @param uri
     * 		  A {@link Uri} representing the Metadata of the match found.
     * @param continousSearch
     * 		  A {@link Boolean} whether the continuous local search is enable or not.
     */
    
    
    private void processSearchResult(String searchId, String label, Uri uri, boolean continousSearch, String objMeta) {
        HistoryItem item = null;

        for (Iterator<HistoryItem> iter = history.iterator();;) {
        	
            if (!iter.hasNext()) {
                break;
            }
            item = iter.next();
            
            // Checks the query corresponding to the match. Set the data.
            if (searchId.equals(item.id)) {
                item.label = label;
                item.uri = uri;
                break;
            } else {
                item = null;
            }
        }
        
        // APPED TO WIKIPEDIA
        Log.v("QWETY", "THIS IS THE DAMN RESULT: " + label);
        
        // Reading in the MetaData to search
        // Dump Metadata into merchant search
        String searchResults = searchMerchants(objMeta);
        Log.i("jchun", "Search results are: " + searchResults);
        JSONArray resArr = new JSONArray();
        
        try {
			JSONObject jsonObj = new JSONObject(searchResults);
			Log.v("jchun",
					"Number of entries " + jsonObj.length());
			// Get Results
			resArr = jsonObj.getJSONArray("results");
			Log.v("jchun", "Number of results " + resArr.length());
		} catch (Exception e) {
			Log.e("jchun", "Unable to convert to json Array!");
			e.printStackTrace();
		}
        
        // Get the listingIDs
        ArrayList<String> listings = new ArrayList<String>();
        for (int i = 0; i < resArr.length(); i++) {
        	if (i > 5) break;
        	try {
				JSONObject res = resArr.getJSONObject(i);
				String url = res.getString("listing_id");
				Log.i("jchun", "resUrl: " + url);
				listings.add(url);
			} catch (JSONException e) {
				e.printStackTrace();
			}
        }
        
        // Get the Images
        ArrayList<String> picURLs = new ArrayList<String>();
        for (int i = 0; i < listings.size(); i++) {
        	String url = getPicUrl(listings.get(i));
        	Log.v("jchun", "Pic url: " + url);
        }
        
        // Get the different Titles
        ArrayList<String> titles = new ArrayList<String>();
        for (int i = 0; i < listings.size(); i++) {
        	String title = getTitle(listings.get(i));
        	Log.v("jchun", "Title: " + title);
        }
        
        //TODO

        // If no query corresponds, then stop.
        if (item == null) {
            Log.w(TAG, "No entry found for qid: " + searchId);
            return;
        }

        historyListAdapter.notifyDataSetChanged();

        if (!activityRunning.get()) {
            localMatchInProgress.set(false);
            return;
        }

        if (!searchId.equals(lastPostedQid) && !continousSearch) {
            localMatchInProgress.set(false);
            return;
        }

        // Try to display the resources from the Uri.
        boolean validUri = processMetaUri(this, uri);
        
        // If no Metadata available, just display the match found and the label.
        // all commentaries in the next bracket are for devices older than Honeycomb.
        if (!validUri) {
        	
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View resultView = getLayoutInflater().inflate(R.layout.match_dialog, null);
            ImageView iv = (ImageView) resultView.findViewById(R.id.matchThumbIv);
            iv.setImageBitmap(item.thumb);
            TextView tv = (TextView) resultView.findViewById(R.id.matchLabelTv);
            tv.setText(item.label);
            builder.setView(resultView);
            builder.setTitle("Result");
            
            builder.setCancelable(true);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            	
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    localMatchInProgress.set(false);
                    reenableRemoteMatch();
                }
            });
            AlertDialog pd = builder.create();
            pd.show();

        } else {
            localMatchInProgress.set(false);
            reenableRemoteMatch();
        }
    }

    private Bitmap transformBitmapToThumb(Bitmap origBmp) {
        int thumbSize = getResources().getDimensionPixelSize(R.dimen.thumb_size);
        return Utils.cropBitmap(origBmp, thumbSize);
    }

    private ProgressDialog showCenteredProgressDialog(String msg) {
        View titleView = new View(this);

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage(msg);
        if (Build.VERSION.SDK_INT < 11) {
            pd.setCustomTitle(titleView);
        }
        pd.setCanceledOnTouchOutside(false);
        pd.setCancelable(false);
        pd.show();

        return pd;
    }

    class DemoFrameReceiver implements Preview.FrameReceiver {

    	
    	
    	/**
    	 * Starts the continuous local search with the displayed frames.
    	 * 
    	 * @param frameBuffer
    	 * 		A {@link Byte} array, the frame's data.
    	 * @param framePreviewSize
    	 * 		A {@link Size}, the frame dimensions.
    	 */
    	
        @Override
        public void onFrameReceived(byte[] frameBuffer, Size framePreviewSize) {
            if (!iqe.isIndexInitialized()) {
                // local index is not initialized yet
                return;
            }
            
            if (!capturing.get()) {
                return;
            }

            if (!remoteMatchInProgress.get()){
            	
                if (!SEARCH_OBJECT_LOCAL_CONTINUOUS) {
                    return;
                }
                if (localMatchInProgress.get()) {
                    return;
                }
                localMatchInProgress.set(true);
                
               
                // convert the data to a YuvImage
                YuvImage yuvImage = new YuvImage (frameBuffer, 17, framePreviewSize.width, framePreviewSize.height,null);
                // analyze the picture.
                processImageNative(yuvImage);
            }
        }

    }
    protected void onDestroy() {


        //Close the Text to Speech Library
        if(tts != null) {

            tts.stop();
            tts.shutdown();
            Log.d("TTS", "TTS Destroyed");
        }
        super.onDestroy();
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
    	 //do they have the data  
    	 if (requestCode == DATA_CHECKING) {  
    	 //yep - go ahead and instantiate  
    	 if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)  
    	  tts = new TextToSpeech(this, this);  
    	 //no data, prompt to install it  
    	 else {  
    	  Intent promptInstall = new Intent();  
    	  promptInstall.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);  
    	  startActivity(promptInstall);  
    	  }  
    	 }  
    	}  

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {  
			  tts.setLanguage(Locale.US);
			  Log.v("TTS", "I have reached here!!!!");
			 }  
		say(text);
	}
	
	public void say(String text){
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
	}

    
	
	public String searchMerchants(String params) {
		String etsyKey = "uuthkb0lpp5qo2e6u0h8gsd6";
		String paramsConcat = "";
		
		try {
			paramsConcat = java.net.URLEncoder.encode(params, "UTF-8").replace("+", "%20");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		String query = "http://openapi.etsy.com/v2/listings/active?api_key="+etsyKey+"&keywords="+paramsConcat;
		HttpGet httpGet = new HttpGet(query);
		Log.i("jchun", "Query sent is " + query);
		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} else {
				Log.e("UGH", "Failed to download file");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return builder.toString();
	}
	
	public String getTitle(String listingID) {
		String etsyKey = "uuthkb0lpp5qo2e6u0h8gsd6";
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		String query = "http://openapi.etsy.com/v2/listings/"+listingID+"?api_key=" +etsyKey;
		HttpGet httpGet = new HttpGet(query);
		Log.i("jchun", "Query for title is " + query);
		
		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} else {
				Log.e("UGH", "Failed to download file");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String json = builder.toString();
		String title = "";
		try {
			JSONObject jsonObj = new JSONObject(json);
			Log.v("jchun",
					"Number of entries " + jsonObj.length());
			// Get Results
			JSONArray resArr = jsonObj.getJSONArray("results");
			JSONObject res = resArr.getJSONObject(0); 
			title = res.getString("title");
		} catch (Exception e) {
			Log.e("jchun", "Unable to convert to json Array!");
			e.printStackTrace();
		}
		return title;
	}
	
	public String getPicUrl(String listingID) {
		String etsyKey = "uuthkb0lpp5qo2e6u0h8gsd6";
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		String query = "http://openapi.etsy.com/v2/listings/"+listingID+"/images?api_key=" +etsyKey;
		HttpGet httpGet = new HttpGet(query);
		Log.i("jchun", "Query for picurl is " + query);
		
		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} else {
				Log.e("UGH", "Failed to download file");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String json = builder.toString();
		
		String title = "";
		try {
			JSONObject jsonObj = new JSONObject(json);
			Log.v("jchun",
					"Number of entries " + jsonObj.length());
			// Get Results
			JSONArray resArr = jsonObj.getJSONArray("results");
			JSONObject res = resArr.getJSONObject(0); 
			title = res.getString("url_75x75");
		} catch (Exception e) {
			Log.e("jchun", "Unable to convert to json Array!");
			e.printStackTrace();
		}
		return title;
	}

}