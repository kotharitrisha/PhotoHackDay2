
package com.iqengines.demo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

public class HistoryItem implements Serializable {

    private static final long serialVersionUID = 9056935202104448194L;

    private static final String TAG = HistoryItem.class.getName();

    String id;

    String label;

    Uri uri;

    transient Bitmap thumb;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(label);
        out.writeUTF(uri != null ? uri.toString() : "null");
        if (thumb != null) {
            FileOutputStream thumbOut = HistoryItemDao.ctx.openFileOutput(getThumbFilename(),
                    Context.MODE_PRIVATE);
            try {
                thumb.compress(CompressFormat.PNG, 100, thumbOut);
            } finally {
                thumbOut.close();
            }
        } else {
            Log.e(TAG, "thumb is null !");
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        label = in.readUTF();
        String uriStr = in.readUTF();
        uri = uriStr.equals("null") ? null : Uri.parse(uriStr);
        thumb = BitmapFactory.decodeFile(HistoryItemDao.ctx.getFileStreamPath(getThumbFilename())
                .getPath());
    }

    private String getThumbFilename() throws UnsupportedEncodingException {
    
        return URLEncoder.encode("thumb_" + label + ".png", "UTF-8");
        
    }

}
