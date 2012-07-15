
package com.iqengines.demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class HistoryListAdapter extends BaseAdapter {

    private DemoActivity demoActivity;

    private LayoutInflater layoutInflater;

    public HistoryListAdapter(DemoActivity demoActivity) {
        this.demoActivity = demoActivity;
        layoutInflater = demoActivity.getLayoutInflater();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        HistoryItem item = (HistoryItem) getItem(position);

        View result = null;
        if (convertView == null) {
            result = layoutInflater.inflate(R.layout.history_item, null);
        } else {
            result = convertView;
        }
        ImageView iv = (ImageView) result.findViewById(R.id.historyItemIv);
        iv.setImageBitmap(item.thumb);
        TextView tv = (TextView) result.findViewById(R.id.historyItemTv);
        tv.setText(item.label);

        return result;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return demoActivity.history.get(demoActivity.history.size() - 1 - position);
    }

    @Override
    public int getCount() {
        return demoActivity.history.size();
    }

    public HistoryItem removeItem(int position) {
        return demoActivity.history.remove(demoActivity.history.size() - 1 - position);
    }

}
