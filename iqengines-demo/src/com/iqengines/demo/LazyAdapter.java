package com.iqengines.demo;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class LazyAdapter extends BaseAdapter {
    
    private Activity activity;
    private ArrayList<String> data;
    private ArrayList<String> titles;
    private static LayoutInflater inflater=null;
    public ImageLoader imageLoader; 
    
    public LazyAdapter(Activity a, ArrayList<String> urls, ArrayList<String> titles) {
        activity = a;
        data=urls;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imageLoader=new ImageLoader(activity.getApplicationContext());
        this.titles = titles;
    }

    public int getCount() {
        return data.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        if(convertView==null) {
            vi = inflater.inflate(R.layout.recclist, null);
            //Log.v("jchun", "COnverView Triggered");
        }
            
        TextView text=(TextView)vi.findViewById(R.id.text);
        ImageView image=(ImageView)vi.findViewById(R.id.image);
        text.setText(titles.get(position));
        imageLoader.DisplayImage(data.get(position), image);
        return vi;
    }
}