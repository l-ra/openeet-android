package com.github.openeet.openeet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import openeet.lite.EetSaleDTO;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String LOGTAG="MainActivity";
    public static final String PREFERENCE_TEST_MODE = "test-mode";
    public static final String PREFERENCE_PLAYGROUND_MODE = "playground-mode";
    public static final String PREFERENCE_DIC = "dic";
    public static final String PREFERENCE_CHANGED_TIME = "changed-time";
    public static final String PREFERENCE_FILE = "APP";


    private enum ListViewContent {
        ALL,
        ONLINE,
        OFFLINE,
        UNREGISTERED,
        ERROR
    }

    private ListViewContent listViewContent=ListViewContent.ALL;

    private static final int REGISTER_SALE=0;

    final protected List<EetSaleDTO> list = new ArrayList<EetSaleDTO>();

    private BroadcastReceiver broadcastReceiver;

    private void updateTestMode(){
        //get mode from prefs
        boolean testMode=PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(PREFERENCE_TEST_MODE,false);
        Log.d(LOGTAG,"Update test mode by preference: "+testMode);

        //set menu
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        MenuItem itmTestMode=navigationView.getMenu().findItem(R.id.nav_test_mode);
        itmTestMode.setChecked(testMode);
        itmTestMode.setTitle(testMode?"Vypnout testovací režim":"Zapnout testovací režim");

        //snackbar
        /*
        if (testMode)
            Snackbar.make(findViewById(R.id.content_main_activity),"Nastaven režim testování, odeslané účtenky nebudou evidovány!",4000).show();
        else
            Snackbar.make(findViewById(R.id.content_main_activity),"Nastaven evidenční režim, odeslané účtenky BUDOU evidovány!",4000).show();
        */

        //update title
        TextView lbl=(TextView) findViewById(R.id.lblTestMode);
        if (lbl==null) return;
        if (testMode)
            lbl.setText("TEST");
        else
            lbl.setText((""));
    }

    private void updatePlagroundMode(){
        boolean playgroundMode=PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(PREFERENCE_PLAYGROUND_MODE,false);
        Log.d(LOGTAG,"Update playground mode by preference: "+playgroundMode);
        TextView lbl=(TextView) findViewById(R.id.lblPlayground);
        if (lbl==null) {
            Log.d(LOGTAG,"no label for playground mode");
            return;
        }
        if (playgroundMode)
            lbl.setText("PLAYGROUND");
        else
            lbl.setText((""));
    }

    private void updateDic(){
        String dic=PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(PREFERENCE_DIC,"Importujte certifikát");
        Log.d(LOGTAG,"Update dic label:"+dic);
        TextView lbl=(TextView)findViewById(R.id.lblDic);
        if (lbl==null) {
            Log.d(LOGTAG,"no label for dic");
            return;
        }
        lbl.setText("DIČ: "+dic);
    }

    private void updateOfflineCount(){
        TextView lbl=(TextView) findViewById(R.id.lblOfflineCount);
        if (lbl==null) return;
        lbl.setText("");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        broadcastReceiver=new MainBroadcastReceiver(this);

        setContentView(R.layout.activity_main_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent registerSaleIntent = new Intent(MainActivity.this, RegisterSale.class);
                startActivityForResult(registerSaleIntent,REGISTER_SALE);
            }
        });

        ListView salesList = (ListView) findViewById(R.id.salesList);
        salesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                SaleService.SaleEntry entry=(SaleService.SaleEntry) adapterView.getItemAtPosition(i);
                Intent detail=new Intent(MainActivity.this, SaleDetailActivity.class);
                detail.putExtra(SaleDetailActivity.EXTRA_SALE_ENTRY, entry);
                startActivity(detail);
            }
        });

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_retry_register:
                new RetryRegisterSalesTask(getBaseContext()).execute("");
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_last_receipts) {
            listViewContent=ListViewContent.ALL;
            Snackbar.make(findViewById(R.id.content_main_activity),"Zobrazeny všechny účtenky",3000).show();
        } else if (id == R.id.nav_unregistereg_receipts) {
            listViewContent=ListViewContent.UNREGISTERED;
            Snackbar.make(findViewById(R.id.content_main_activity),"Zobrazeny pouze místně uložené účtenky",3000).show();
        } else if (id == R.id.nav_error_receipts) {
            listViewContent=ListViewContent.ERROR;
            Snackbar.make(findViewById(R.id.content_main_activity),"Zobrazeny účtenky s chybou",3000).show();
        } else if (id == R.id.nav_test_mode) {
            boolean testModeNew=! PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(PREFERENCE_TEST_MODE,false);
            Log.d(LOGTAG,"Changing test mode to "+testModeNew);
            SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
            editor.putBoolean(PREFERENCE_TEST_MODE, testModeNew);
            editor.putLong(MainActivity.PREFERENCE_CHANGED_TIME,System.currentTimeMillis());
            editor.commit();
        }
        else if (id == R.id.nav_import_cert) {
            Intent importCertIntent = new Intent(MainActivity.this, CertificateImport.class);
            startActivity(importCertIntent);
        }
        else if (id == R.id.nav_settings) {
            Intent settingsActivityIntent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settingsActivityIntent);
        }
        else if (id == R.id.nav_reset_store) {
            final MainActivity act=this;
            new AlertDialog.Builder(this)
                    .setTitle("Vymazání uložených účtenek")
                    .setMessage("Účtenky uložené v tomto zařízení budou vymazány.")
                    .setIcon(R.drawable.ic_warning_black_24dp)
                    .setPositiveButton("Smazat!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                SaleStore.getInstance(getApplicationContext()).resetStore(SaleStoreFileImpl.BY_BKP);
                                Snackbar.make(findViewById(R.id.content_main_activity),"Soubor úložiště zálohován a přejmenován.",3000).show();
                                updateList();
                            }
                            catch (Exception e){
                                Log.e(LOGTAG,"failed to delete store");
                                Snackbar.make(findViewById(R.id.content_main_activity),"Chyba pri resety: "+e.getMessage(),3000).show();
                            }
                        }
                    })
                    .setNegativeButton("Nemazat", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    }).show();

        } else if (id == R.id.nav_about) {
            Intent showAboutIntent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(showAboutIntent);
        }
        updateList();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void listProviders() {
        try {
            Provider p[] = Security.getProviders();
            for (int i = 0; i < p.length; i++) {
                Log.d(LOGTAG,p[i].toString());
                for (Enumeration e = p[i].keys(); e.hasMoreElements();)
                    Log.d(LOGTAG,"      " + e.nextElement().toString());
            }
        } catch (Exception e) {
            Log.e(LOGTAG,"error listing providers/algs");
        }
    }

    protected void updateList() {
        SaleStore store=SaleStore.getInstance(getBaseContext().getApplicationContext());
        SaleService.SaleEntry[] items=null;

        try {
            switch (listViewContent) {
                case ALL:
                    items = store.findAll(-1, -1, SaleStore.LimitType.COUNT);
                    break;
                case ONLINE:
                    items = store.findOnline(-1, -1, SaleStore.LimitType.COUNT);
                    break;
                case OFFLINE:
                    items = store.findOffline(-1, -1, SaleStore.LimitType.COUNT);
                    break;
                case UNREGISTERED:
                    items = store.findUnregistered(-1, -1, SaleStore.LimitType.COUNT);
                    break;
                case ERROR:
                    items = store.findError(-1, -1, SaleStore.LimitType.COUNT);
                    break;
                default:
                    items = store.findAll(-1, -1, SaleStore.LimitType.COUNT);
                    break;
            }
        }
        catch (Exception e){
            Log.e(LOGTAG,"Error while updating data from storaqe",e);
            Snackbar.make(findViewById(R.id.content_main_activity),"Chyba úložiště",3000).show();
        }

        if (items!=null) {
            ArrayAdapter<SaleService.SaleEntry> adapter = new SaleListArrayAdapter(this, items);
            ListView salesList = (ListView) findViewById(R.id.salesList);
            salesList.setAdapter(adapter);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case REGISTER_SALE: processRegisterSaleResult(resultCode,data);break;
        }
    }

    protected void processRegisterSaleResult(int resultCode, Intent data){
        Log.d(LOGTAG,"Processing result");
        if (resultCode==RESULT_OK && data!=null) {
            EetSaleDTO dtoSale = (EetSaleDTO) data.getSerializableExtra(RegisterSale.RESULT);
            new RegisterSaleTask(getApplicationContext()).execute(dtoSale);
        }
    }

    @Override
    protected void onStart() {
        Log.d(LOGTAG,"onStart");
        super.onStart();
        registerBroadcastReceivers();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
        updatePlagroundMode();
        updateTestMode();
        updateDic();
        updateOfflineCount();
        updateList();
    }

    @Override
    protected void onStop() {
        Log.d(LOGTAG,"onStop");
        super.onStop();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        Log.d(LOGTAG,"onResume");
        super.onResume();
        registerBroadcastReceivers();
        updatePlagroundMode();
        updateTestMode();
        updateOfflineCount();
        updateDic();
        updateList();
    }

    @Override
    protected void onPause() {
        Log.d(LOGTAG,"onPause");
        super.onPause();
        unregisterBroadcastReceivers();
    }

    private void unregisterBroadcastReceivers() {
        Log.d(LOGTAG,"unregister br. recv");
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(broadcastReceiver);
    }

    private void registerBroadcastReceivers() {
        Log.d(LOGTAG,"register br. recv");
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(broadcastReceiver,RegisterSaleTask.getMatchAllFilter());
    }

    public void processBroadcast(Context context, Intent intent) {
        Log.d(LOGTAG,"onReceive: "+intent.getAction());
        updateList();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(LOGTAG, "Shared pref changed: "+key);
        updatePlagroundMode();
        updateTestMode();
        updateOfflineCount();
        updateDic();
    }


}
