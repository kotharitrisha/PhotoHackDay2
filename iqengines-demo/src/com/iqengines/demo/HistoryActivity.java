
package com.iqengines.demo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

public class HistoryActivity extends Activity {

    private static final int DELETE_MENU_ITEM = 0;

    private ListView historyListView;
    
    private ImageButton goBack;
    
    private Uri uriShop = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.history);
        DemoActivity.iqe.pause();
        initUi();
    }

    private void initUi() {
    	
    	historyListView = (ListView) findViewById(R.id.list);

        historyListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parentView, View view, int position, long id) {
                HistoryItem item = (HistoryItem) historyListView.getAdapter().getItem(position);
                if(item.label!=null)
                uriShop = Uri.parse("http://google.com//search?q="+Uri.parse(item.label)+"&tbm=shop");
                
                DemoActivity.processMetaUri(HistoryActivity.this, uriShop);
            }
            
        });
        
        
        goBack = (ImageButton) findViewById(R.id.goBack);
        goBack.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HistoryActivity.this, DemoActivity.class);
                startActivity(intent);
            }
        });
        
        

        // empty header to show separator on the top of the view per
        historyListView.setHeaderDividersEnabled(false);
        historyListView.setDividerHeight(0);
        registerForContextMenu(historyListView);
    }

    @Override
    public void onResume() {
        historyListView.setAdapter(DemoActivity.historyListAdapter);

        DemoActivity.iqe.resume();
        
        super.onResume();
    }

    @Override
    public void onPause() {
        DemoActivity.iqe.pause();     
        super.onPause();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.list) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            HistoryItem item = (HistoryItem) DemoActivity.historyListAdapter
                    .getItem(info.position/* - 1 subtracting header view-item */);
            menu.setHeaderTitle(item.label);
            menu.add(Menu.NONE, DELETE_MENU_ITEM, 0, "Remove");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem
                .getMenuInfo();
        if (menuItem.getItemId() == DELETE_MENU_ITEM) {
            HistoryItem removedItem = DemoActivity.historyListAdapter
                    .removeItem(info.position/* - 1/* subtracting header view-item */);
            DemoActivity.historyListAdapter.notifyDataSetChanged();
            Toast.makeText(this, "'" + removedItem.label + "' removed from the history",
                    Toast.LENGTH_SHORT).show();
        }
        return true;
    }
}
