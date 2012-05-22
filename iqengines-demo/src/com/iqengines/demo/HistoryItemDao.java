
package com.iqengines.demo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import android.content.Context;
import android.util.Log;

public class HistoryItemDao {

    private static final String HISTORY_FILENAME = "history.ser";

    private static final String TAG = HistoryItemDao.class.getName();

    static Context ctx;

    public HistoryItemDao(Context ctx) {
        HistoryItemDao.ctx = ctx;
    }

    public List<HistoryItem> loadAll() {
        if (!ctx.getFileStreamPath(HISTORY_FILENAME).exists()) {
            return null;
        }
        ObjectInputStream in = null;
        try {
        	Log.d(TAG,"apres creation in");
            in = new ObjectInputStream(ctx.openFileInput(HISTORY_FILENAME));
            Log.d(TAG,"apres creation history Item");
            @SuppressWarnings("unchecked") // readObject() return Object.          
			List<HistoryItem> res = (List<HistoryItem>) in.readObject();
            return res;
        } catch (Exception e) {
            Log.e(TAG, "Unable to deserialize history", e);
            // delete it as it's likely corrupted
            ctx.getFileStreamPath(HISTORY_FILENAME).delete();
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "", e);
                }
            }
        }
    }

    public void saveAll(List<HistoryItem> items) {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(ctx.openFileOutput(HISTORY_FILENAME, Context.MODE_PRIVATE));
            out.writeObject(items);
        } catch (Exception e) {
            Log.e(TAG, "Unable to serialize history", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.e(TAG, "", e);
                }
            }
        }
    }

}
