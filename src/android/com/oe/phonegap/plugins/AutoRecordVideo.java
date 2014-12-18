package com.oe.phonegap.plugins;

/*
Copyright 2014 minhnt@ownego.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

import static com.oe.phonegap.plugins.VideoCapture.INTENT_DURATION;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import android.os.Build;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;


import org.apache.cordova.file.FileUtils;
import org.apache.cordova.file.LocalFilesystemURL;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

public class AutoRecordVideo extends CordovaPlugin {

    private static final String VIDEO_3GPP = "video/3gpp";
    private static final String AUDIO_3GPP = "audio/3gpp";
    private static final String VIDEO_MP4 = "video/mp4";

    public static final int CAPTURE_VIDEO = 2;
    private static final int CAPTURE_INTERNAL_ERR = 0;
//    private static final int CAPTURE_APPLICATION_BUSY = 1;
//    private static final int CAPTURE_INVALID_ARGUMENT = 2;
    private static final int CAPTURE_NO_MEDIA_FILES = 3;

    private CallbackContext callbackContext;        // The callback context from which we were invoked.
    private int duration;                           // optional max duration of video recording in seconds
    private JSONObject result;                      // The result to be returned to the user

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        this.duration = 0;
        this.result = new JSONObject();

        OELog.d("PLUGIN INIT");

        JSONObject options = args.optJSONObject(0);
        if (options != null) {
            duration = options.optInt("duration", 0);
            OELog.d("DURATION: "+duration);
        }

        if (action.equals("record")) {
            this.captureVideo(duration);
        }
        else {
            return false;
        }

        return true;
    }

    private String getTempDirectoryPath() {
        File cache = null;

        // Use internal storage
        cache = cordova.getActivity().getCacheDir();

        // Create the cache directory if it doesn't exist
        cache.mkdirs();
        return cache.getAbsolutePath();
    }

    /**
     * Sets up an intent to capture video.  Result handled by onActivityResult()
     */
    private void captureVideo(int duration) {
        Intent intent = new Intent(this.cordova.getActivity(), VideoCapture.class);

        if(Build.VERSION.SDK_INT > 7){
            intent.putExtra(INTENT_DURATION, duration);
        }
        this.cordova.startActivityForResult((CordovaPlugin) this, intent, CAPTURE_VIDEO);
    }

    /**
     * Called when the video view exits.
     *
     * @param requestCode       The request code originally supplied to startActivityForResult(),
     *                          allowing you to identify who this result came from.
     * @param resultCode        The integer result code returned by the child activity through its setResult().
     * @param intent            An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     * @throws JSONException
     */
    public void onActivityResult(int requestCode, int resultCode, final Intent intent) {

        // Result received okay
        if (resultCode == Activity.RESULT_OK) {
            // An audio clip was requested
            if (requestCode == CAPTURE_VIDEO) {

                final AutoRecordVideo that = this;
                Runnable captureVideo = new Runnable() {

                    @Override
                    public void run() {
                    
                        Uri data = null;
                        
                        if (intent != null){
                            // Get the uri of the video clip
                            data = intent.getData();
                        }
                        
                        if( data == null){
                           File movie = new File(getTempDirectoryPath(), "AutoRecordVideo.avi");
                           
                           OELog.d("data null "+movie.toURI());
                           
                           data = Uri.fromFile(movie);
                        }
                        
                        // create a file object from the uri
                        if(data == null) {
                            that.fail(createErrorObject(CAPTURE_NO_MEDIA_FILES, "Error: data is null"));
                        } else {
                        	result = createMediaFile(data);

                            OELog.d("Camera success"+data);
                            
                            that.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
                        }
                    }
                };
                
                this.cordova.getThreadPool().execute(captureVideo);
            }
        }
        
        // If canceled
        else if (resultCode == Activity.RESULT_CANCELED) {
            // If we have partial results send them back to the user
            if (result != null) {
                this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
            }
            // user canceled the action
            else {
                this.fail(createErrorObject(CAPTURE_NO_MEDIA_FILES, "Canceled."));
            }
        }
        
        // If something else
        else {
            // If we have partial results send them back to the user
            if (result != null) {
                this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
            }
            // something bad happened
            else {
                this.fail(createErrorObject(CAPTURE_NO_MEDIA_FILES, "Did not complete!"));
            }
        }
    }

    /**
     * Creates a JSONObject that represents a File from the Uri
     *
     * @param data the Uri of the audio/image/video
     * @return a JSONObject that represents a File
     * @throws IOException
     */
    private JSONObject createMediaFile(Uri data) {
        File fp = webView.getResourceApi().mapUriToFile(data);
        JSONObject obj = new JSONObject();

        Class webViewClass = webView.getClass();
        PluginManager pm = null;
        try {
            Method gpm = webViewClass.getMethod("getPluginManager");
            pm = (PluginManager) gpm.invoke(webView);
        } catch (NoSuchMethodException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        if (pm == null) {
            try {
                Field pmf = webViewClass.getField("pluginManager");
                pm = (PluginManager)pmf.get(webView);
            } catch (NoSuchFieldException e) {
            } catch (IllegalAccessException e) {
            }
        }
        
        FileUtils filePlugin = (FileUtils) pm.getPlugin("File");
        LocalFilesystemURL url = filePlugin.filesystemURLforLocalPath(fp.getAbsolutePath());

        try {
            // File properties
            obj.put("name", fp.getName());
            obj.put("fullPath", fp.toURI().toString());
            if (url != null) {
                obj.put("localURL", url.toString());
            }
            // Because of an issue with MimeTypeMap.getMimeTypeFromExtension() all .3gpp files
            // are reported as video/3gpp. I'm doing this hacky check of the URI to see if it
            // is stored in the audio or video content store.
            if (fp.getAbsoluteFile().toString().endsWith(".3gp") || fp.getAbsoluteFile().toString().endsWith(".3gpp")) {
                if (data.toString().contains("/audio/")) {
                    obj.put("type", AUDIO_3GPP);
                } else {
                    obj.put("type", VIDEO_3GPP);
                }
            } else {
                obj.put("type", FileHelper.getMimeType(Uri.fromFile(fp), cordova));
            }

            obj.put("lastModifiedDate", fp.lastModified());
            obj.put("size", fp.length());
        } catch (JSONException e) {
            // this will never happen
            e.printStackTrace();
        }
        return obj;
    }

    private JSONObject createErrorObject(int code, String message) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("code", code);
            obj.put("message", message);
        } catch (JSONException e) {
            // This will never happen
        }
        return obj;
    }

    /**
     * Send error message to JavaScript.
     *
     * @param err
     */
    public void fail(JSONObject err) {
        this.callbackContext.error(err);
    }

}