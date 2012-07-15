package com.iqengines.demo;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

public class ReccActivity extends Activity {
    
    ListView list;
    LazyAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recc);
        
        Bundle bundle = this.getIntent().getExtras();
        ArrayList<String> picUrls = (ArrayList<String>) bundle.get("urls");
        ArrayList<String> titles = (ArrayList<String>) bundle.get("titles");
        
        list=(ListView)findViewById(R.id.list);
        adapter=new LazyAdapter(this, picUrls, titles);
        list.setAdapter(adapter);
        
        //Button b=(Button)findViewById(R.id.button1);
        //b.setOnClickListener(listener);
    }
    
    @Override
    public void onDestroy()
    {
        list.setAdapter(null);
        super.onDestroy();
    }
    
    /*
    public OnClickListener listener=new OnClickListener(){
        @Override
        public void onClick(View arg0) {
            adapter.imageLoader.clearCache();
            adapter.notifyDataSetChanged();
        }
    };
    */
    
}