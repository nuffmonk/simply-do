/*
 * Copyright (C) 2010, 2011 Keith Kildare
 * 
 * This file is part of SimplyDo.
 * 
 * SimplyDo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * SimplyDo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with SimplyDo.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package kdk.android.simplydo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

public class SimplyDoActivity extends Activity 
{
    private static final int DELETE_INACTIVE = 100;
    private static final int DELETE_LIST     = 101;
    private static final int EDIT_LIST       = 102;
    private static final int DELETE_ITEM     = 103;
    private static final int EDIT_ITEM       = 104;
    private static final int TOGGLE_STAR     = 105;
    private static final int SETTINGS        = 106;
    private static final int SORT_NOW        = 107;
    private static final int MOVE_ITEM       = 108;
    
    private static final int DIALOG_LIST_DELETE = 200;
    private static final int DIALOG_ITEM_DELETE = 201;
    private static final int DIALOG_LIST_EDIT = 202;
    private static final int DIALOG_ITEM_EDIT = 203;
    private static final int DIALOG_ITEM_MOVE = 204;
    
    private DataViewer dataViewer;
    private ListPropertiesAdapter listPropertiesAdapter;
    private ItemPropertiesAdapter itemPropertiesAdapter;
    
    private ListsListReactor listsListReactor = new ListsListReactor();
    private ItemsListReactor itemsListReactor = new ItemsListReactor();
    
    private ItemDesc ctxItem;
    private AlertDialog.Builder itemDeleteBuilder;
    
    private ListDesc ctxList;
    private AlertDialog.Builder listDeleteBuilder;
    
    private AlertDialog.Builder itemEditBuilder;    
    private EditText itemEditView;

    private AlertDialog.Builder listEditBuilder;    
    private EditText listEditView;
    
    private ListListSorter listListSorter = new ListListSorter();
    private ItemListSorter itemListSorter = new ItemListSorter();
    
    private MoveToAction moveItemToAction;

    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        Log.v(L.TAG, "onCreate called");
        
        setContentView(R.layout.main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                
        DataManager dataManager = new DataManager(this);
        
        //dataViewer = new SimpleDataViewer(dataManager);
        
        CachingDataViewer cdv = new CachingDataViewer(dataManager);
        cdv.start();
        dataViewer = cdv;
        
        listPropertiesAdapter = new ListPropertiesAdapter(this, dataViewer);
        
        ListView listView = (ListView)findViewById(R.id.ListsListView);
        listView.setAdapter(listPropertiesAdapter);
        listView.setOnCreateContextMenuListener(listsListReactor);
        listView.setOnItemClickListener(listsListReactor);
        
        itemPropertiesAdapter = new ItemPropertiesAdapter(this, dataViewer);
        ListView itemView = (ListView)findViewById(R.id.ItemsListView);
        itemView.setAdapter(itemPropertiesAdapter);
        itemView.setOnCreateContextMenuListener(itemsListReactor);
        itemView.setOnItemClickListener(itemsListReactor);
        
        Button addList = (Button)findViewById(R.id.AddListButton);
        addList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                addList();
            }
        });
        
        Button addItem = (Button)findViewById(R.id.AddItemButton);
        addItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                addItem();
            }
        });
        
        ViewSwitcher viewSwitch = (ViewSwitcher)findViewById(R.id.ListsItemsSwitcher);
        viewSwitch.setInAnimation(AnimationUtils.loadAnimation(this,
                android.R.anim.slide_in_left));
        viewSwitch.setOutAnimation(AnimationUtils.loadAnimation(this,
                android.R.anim.slide_out_right));
        
        listDeleteBuilder = new AlertDialog.Builder(this);
        listDeleteBuilder.setMessage("Are you sure you want to delete this list?")
               .setCancelable(true)
               .setTitle("Delete?")
               .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       contextDeleteList();
                       dialog.cancel();
                   }
               })
               .setNegativeButton("No", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                   }
               });
        
        itemDeleteBuilder = new AlertDialog.Builder(this);
        itemDeleteBuilder.setMessage(
                "This item is still active. Are you sure you want to delete it?")
               .setCancelable(true)
               .setTitle("Delete?")
               .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       contextDeleteItem();
                       dialog.cancel();
                   }
               })
               .setNegativeButton("No", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                   }
               });
        
        EditText addListEditText = (EditText)findViewById(R.id.AddListEditText);
        addListEditText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                Log.d(L.TAG, "Editor Action " + actionId);
                addList();
                return true;
            }
        });
        
        EditText addItemEditText = (EditText)findViewById(R.id.AddItemEditText);
        addItemEditText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                addItem();
                return true;
            }
        });
        
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        
        View itemEditLayout = inflater.inflate(R.layout.item_edit, (ViewGroup)findViewById(R.id.item_edit_root));

        itemEditView = (EditText)itemEditLayout.findViewById(R.id.EditItemLabelEditText);
        
        itemEditBuilder = new AlertDialog.Builder(this);
        itemEditBuilder.setView(itemEditLayout)
            .setCancelable(true)
            .setTitle("Edit Item Label")
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    itemEditOk();
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                     //dialog.cancel();
                }
            });
        
        View listEditLayout = inflater.inflate(R.layout.list_edit, (ViewGroup)findViewById(R.id.list_edit_root));

        listEditView = (EditText)listEditLayout.findViewById(R.id.EditListLabelEditText);
        
        listEditBuilder = new AlertDialog.Builder(this);
        listEditBuilder.setView(listEditLayout)
            .setCancelable(true)
            .setTitle("Edit List Label")
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    listEditOk();
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // do nothing
                }
            });
                
        dataViewer.fetchLists();
        listPropertiesAdapter.notifyDataSetChanged();
        
        moveItemToAction = new MoveToAction(
                this, 
                dataViewer, 
                listPropertiesAdapter, 
                itemPropertiesAdapter);
        
        if (savedInstanceState==null) 
        {
            Log.d(L.TAG, "onCreate()");
        }
        else 
        {
            Log.d(L.TAG, "onCreate() with a supplied state");
            
            ListDesc listDesc = (ListDesc)savedInstanceState.getSerializable("currentList");
            if(listDesc != null)
            {
                listSelected(listDesc, false);
            }
        }
    }
    
    
    
    
    @Override
    protected void onStart()
    {
        super.onStart();
        
        Log.v(L.TAG, "onStart called");
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        
        String itemSort = prefs.getString("itemSorting", ItemListSorter.PREF_ACTIVE_STARRED);
        Log.v(L.TAG, "itemSort = " + itemSort);
        
        itemListSorter.setSortingMode(itemSort);
        itemListSorter.sort(dataViewer.getItemData());
        itemPropertiesAdapter.notifyDataSetChanged();
        
        String listSort = prefs.getString("listSorting", ListListSorter.PREF_ALPHA);
        listListSorter.setSortingMode(listSort);
        Log.v(L.TAG, "listSort = " + listSort);
        
        listListSorter.sort(dataViewer.getListData());
        listPropertiesAdapter.notifyDataSetChanged();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        Log.v(L.TAG, "onSaveInstanceState() called");
        
        outState.putSerializable("currentList", dataViewer.getSelectedList());
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.v(L.TAG, "onDestroy() called");
        
        dataViewer.close();
    }


    @Override
    public void onBackPressed()
    {
        Log.v(L.TAG, "onBackPressed() called");
        
        ViewSwitcher viewSwitch = (ViewSwitcher)findViewById(R.id.ListsItemsSwitcher);
        int displayed = viewSwitch.getDisplayedChild();
        
        if(displayed == 0)
        {
            super.onBackPressed();
        }
        else
        {
            setTitle(R.string.app_name);
            viewSwitch.showPrevious();
            dataViewer.setSelectedList(null);
            itemPropertiesAdapter.notifyDataSetChanged();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        Log.v(L.TAG, "onCreateOptionsMenu() called");
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        Log.v(L.TAG, "onPrepareOptionsMenu() called");
        
        menu.clear();
        ViewSwitcher viewSwitch = (ViewSwitcher)findViewById(R.id.ListsItemsSwitcher);
        boolean isItemDisplay = viewSwitch.getDisplayedChild() != 0;
        
        if(isItemDisplay)
        {
            MenuItem deleteInactiveMI = menu.add(Menu.NONE, DELETE_INACTIVE, Menu.NONE, "Delete Inactive");
            deleteInactiveMI.setIcon(android.R.drawable.ic_menu_delete);
        }
        
        MenuItem settingsMI = menu.add(Menu.NONE, SETTINGS, Menu.NONE, "Settings");
        settingsMI.setIcon(android.R.drawable.ic_menu_preferences);
        
        if(isItemDisplay)
        {
            MenuItem sortNowMI = menu.add(Menu.NONE, SORT_NOW, Menu.NONE, "Sort Now");
            sortNowMI.setIcon(android.R.drawable.ic_menu_sort_by_size);
        }
        
        return true;
    }
    
    

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item)
    {
        Log.v(L.TAG, "onMenuItemSelected() called");
        
        switch(item.getItemId())
        {
        case DELETE_INACTIVE:
        {
            ListDesc currentList = dataViewer.getSelectedList();
            if(currentList == null)
            {
                Log.e(L.TAG, "Delete inactive called but selected list was null");
                return true;
            }
            Log.d(L.TAG, "Deleting Inactive");
            
            dataViewer.deleteInactive();
            itemPropertiesAdapter.notifyDataSetChanged();
            listPropertiesAdapter.notifyDataSetChanged();
            return true;
        }
        case SETTINGS:
        {
            Intent settingsActivity = new Intent(getBaseContext(), SettingsActivity.class);
            startActivity(settingsActivity);
            return true;
        }
        case SORT_NOW:
        {
            itemListSorter.sort(dataViewer.getItemData());
            itemPropertiesAdapter.notifyDataSetChanged();
            return true;
        }
        case DELETE_LIST:
            Log.d(L.TAG, "Got Delete List");
            // Call are you sure?
            showDialog(DIALOG_LIST_DELETE);
            return true;
        case DELETE_ITEM:
            Log.d(L.TAG, "Got Delete Item");
            if(ctxItem.isActive())
            {
                // Call are you sure?
                showDialog(DIALOG_ITEM_DELETE);
            }
            else
            {
                contextDeleteItem();
            }
            return true;
        case EDIT_ITEM:
            Log.d(L.TAG, "Got Edit Item");
            showDialog(DIALOG_ITEM_EDIT);
            return true;
        case EDIT_LIST:
            Log.d(L.TAG, "Got Edit List");
            showDialog(DIALOG_LIST_EDIT);
            return true;
        case TOGGLE_STAR:
            if(ctxItem == null)
            {
                Log.e(L.TAG, "Toggle star but no context item!");
                return true;
            }
            Log.d(L.TAG, "Toggling star");
            
            dataViewer.updateItemStarness(ctxItem.getId(), !ctxItem.isStar());
            itemPropertiesAdapter.notifyDataSetChanged();
            return true;
        case MOVE_ITEM:
            if(ctxItem == null)
            {
                Log.e(L.TAG, "Move item selected but no context item!");
                return true;
            }
            Log.d(L.TAG, "Displaying move item dialog");
            
            showDialog(DIALOG_ITEM_MOVE);
            
            return true;
        }
        
        return super.onMenuItemSelected(featureId, item);
    }


    @Override
    protected Dialog onCreateDialog(int id)
    {
        Log.v(L.TAG, "onCreateDialog() called");
                
        switch(id)
        {
            case DIALOG_LIST_DELETE:
            {
                AlertDialog dialog = listDeleteBuilder.create();
                return dialog;
            }
            case DIALOG_ITEM_DELETE:
            {
                AlertDialog dialog = itemDeleteBuilder.create();
                return dialog;
            }
            case DIALOG_ITEM_EDIT:
            {
                AlertDialog editDialog = itemEditBuilder.create();
                editDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                return editDialog;
            }
            case DIALOG_LIST_EDIT:
            {
                AlertDialog editDialog = listEditBuilder.create();
                editDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                return editDialog;
            }
            case DIALOG_ITEM_MOVE:
            {
                return moveItemToAction.createDialog();
            }
        }
        
        return super.onCreateDialog(id);
    }
    
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog)
    {
        super.onPrepareDialog(id, dialog);
        
        Log.v(L.TAG, "onPrepareDialog() called");
        
        switch(id)
        {
        case DIALOG_ITEM_EDIT:
            if(ctxItem != null)
            {
                itemEditView.setText(ctxItem.getLabel());
            }
            else
            {
                Log.e(L.TAG, "onPrepareDialog()/DIALOG_ITEM_EDIT called for nonexistant item");
            }
            break;
        case DIALOG_LIST_EDIT:
            if(ctxList != null)
            {
                listEditView.setText(ctxList.getLabel());            
            }
            else
            {
                Log.e(L.TAG, "onPrepareDialog()/DIALOG_LIST_EDIT called for nonexistant list");
            }
            break;
        case DIALOG_ITEM_MOVE:
            if(ctxItem != null)
            {
                moveItemToAction.prepareDialog(dialog, ctxItem);
            }
            else
            {
                Log.e(L.TAG, "onPrepareDialog()/DIALOG_ITEM_MOVE called for nonexistant item");
            }
            break;
        }

    }
    
    
    private void listSelected(ListDesc list, boolean animate)
    {
        //Log.v(L.TAG, "SimplyDoActivity.listSelected() called on list " + list.getId());
        
        setTitle(list.getLabel());
        
        dataViewer.setSelectedList(list);
        itemListSorter.sort(dataViewer.getItemData());
        itemPropertiesAdapter.notifyDataSetChanged();
        
        ViewSwitcher viewSwitch = (ViewSwitcher)findViewById(R.id.ListsItemsSwitcher);
        
        if(animate)
        {
            viewSwitch.showNext();
        }
        else
        {
            viewSwitch.reset();
            viewSwitch.setAnimateFirstView(false);
            viewSwitch.setDisplayedChild(1);
        }
    }
    
    
    private void itemSelected(ItemDesc item)
    {
        Log.v(L.TAG, "Item selected " + item.getLabel());
        
        dataViewer.updateItemActiveness(item.getId(), !item.isActive());
        itemPropertiesAdapter.notifyDataSetChanged();
        listPropertiesAdapter.notifyDataSetChanged();
    }
    
    
    private void addList()
    {
        EditText editText = (EditText)findViewById(R.id.AddListEditText);
        
        String txt = editText.getText().toString();
        String txtTrim = txt.trim();
        
        if(txtTrim.length() > 0)
        {
            dataViewer.createList(txtTrim);
            listListSorter.sort(dataViewer.getListData());
            listPropertiesAdapter.notifyDataSetChanged();
            editText.getText().clear();
        }
    }
    
    
    private void addItem()
    {
        EditText editText = (EditText)findViewById(R.id.AddItemEditText);
        
        String txt = editText.getText().toString();
        String txtTrim = txt.trim();
        
        if(txtTrim.length() > 0)
        {
            ListDesc currentList = dataViewer.getSelectedList();
            if(currentList != null)
            {
                dataViewer.createItem(txtTrim);
                itemListSorter.sort(dataViewer.getItemData());
                itemPropertiesAdapter.notifyDataSetChanged();
                listPropertiesAdapter.notifyDataSetChanged();
                editText.getText().clear();
            }
            else
            {
                Log.e(L.TAG, "Add item called but selected list was null");
            }
        }
    }
    
    
    private void itemEditOk()
    {
        String txt = itemEditView.getText().toString();
        
        if(txt.trim().length() > 0)
        {
            dataViewer.updateItemLabel(ctxItem.getId(), txt);
            itemPropertiesAdapter.notifyDataSetChanged();
        }
    }
    
    
    private void listEditOk()
    {
        String txt = listEditView.getText().toString();
        
        if(txt.trim().length() > 0)
        {
            dataViewer.updateListLabel(ctxList.getId(), txt);
            listPropertiesAdapter.notifyDataSetChanged();
        }
    }
    
    
    private void contextDeleteList()
    {
        Log.d(L.TAG, "Deleting list " + ctxList.getLabel());
        
        dataViewer.deleteList(ctxList.getId());
        listPropertiesAdapter.notifyDataSetChanged();
        
        ctxList = null;
    }
    
    private void contextDeleteItem()
    {
        Log.d(L.TAG, "Deleting item " + ctxItem.getLabel());
        
        dataViewer.deleteItem(ctxItem.getId());
        itemPropertiesAdapter.notifyDataSetChanged();
        listPropertiesAdapter.notifyDataSetChanged();
        
        ctxItem = null;
    }
    
    
    private class ItemsListReactor implements OnItemClickListener, View.OnCreateContextMenuListener
    {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo)
        {
            Log.v(L.TAG, "ItemsListReactor.onCreateContextMenu()");
            
            AdapterContextMenuInfo ctxMenuInfo = (AdapterContextMenuInfo)menuInfo;
            ListView listView = (ListView)findViewById(R.id.ItemsListView);
            ctxItem = (ItemDesc)listView.getItemAtPosition(ctxMenuInfo.position);

            menu.setHeaderTitle("Item Options");
            menu.add(Menu.NONE, EDIT_ITEM, Menu.NONE, "Edit");
            menu.add(Menu.NONE, DELETE_ITEM, Menu.NONE, "Delete");
            String toggleText;
            if(ctxItem.isStar())
            {
                toggleText = "Remove Star";
            }
            else
            {
                toggleText = "Add Star";
            }
            menu.add(Menu.NONE, TOGGLE_STAR, Menu.NONE, toggleText);
            if(dataViewer.getListData().size() > 1)
            {
                menu.add(Menu.NONE, MOVE_ITEM, Menu.NONE, "Move To");
            }
        }

        
        @Override
        public void onItemClick(AdapterView<?> adapter, View view, int position,
                long id)
        {
            Log.v(L.TAG, "ItemsListReactor.onItemClick()");
            
            ListView listView = (ListView)findViewById(R.id.ItemsListView);
            ItemDesc itemDesc = (ItemDesc)listView.getItemAtPosition(position);
            itemSelected(itemDesc);
        }
        
    }
    
    private class ListsListReactor implements OnItemClickListener, View.OnCreateContextMenuListener
    {
        @Override
        public void onItemClick(AdapterView<?> adapter, View view, int position,
                long id)
        {
            Log.v(L.TAG, "ListsListReactor.onItemClick()");
            
            ListView listView = (ListView)findViewById(R.id.ListsListView);
            ListDesc listDesc = (ListDesc)listView.getItemAtPosition(position);
            listSelected(listDesc, true);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo)
        {
            Log.v(L.TAG, "ListsListReactor.onCreateContextMenu()");
            
            AdapterContextMenuInfo ctxMenuInfo = (AdapterContextMenuInfo)menuInfo;
            ListView listView = (ListView)findViewById(R.id.ListsListView);
            ctxList = (ListDesc)listView.getItemAtPosition(ctxMenuInfo.position);

            menu.setHeaderTitle("List Options");
            menu.add(Menu.NONE, EDIT_LIST, Menu.NONE, "Edit");
            menu.add(Menu.NONE, DELETE_LIST, Menu.NONE, "Delete");
        }
    
    }
}