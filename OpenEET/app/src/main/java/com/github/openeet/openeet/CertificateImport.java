package com.github.openeet.openeet;

import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;


public class CertificateImport extends AppCompatActivity {
    private static final String LOGTAG="CertificateImport";

    private List<CertificateInfo> certInfos;
    private int currentCert=-1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOGTAG, "Creating activity");
        setContentView(R.layout.activity_certificate_import);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Log.d(LOGTAG, "Initing listeners");

        initListeners();

        Log.d(LOGTAG, "Injecting assetmanager");
        //inject asset manager
        CertificateInfo.setAssetManager(getAssets());
        CertificateInfo.setContext(getApplicationContext());

        findCertInfos();
        displayCert();
    }

    private void initListeners(){
        Button btnImport=(Button) findViewById(R.id.btnImport);
        Button btnSkip=(Button) findViewById(R.id.btnSkip);
        Button btnCancel=(Button) findViewById(R.id.btnCancel);
        final TextView txtPassword=(TextView) findViewById(R.id.txtPassword);

        btnImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                importCurrent();
            }
        });

        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextCert();
                displayCert();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        txtPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                tryPassword(txtPassword.getText().toString());
            }
        });
    }

    private void importCurrent(){


    }

    private void tryPassword(String password){
        if (password==null || password.isEmpty()) return;
        CertificateInfo certInfo=certInfos.get(currentCert);
        certInfo.setPasswd(password);
        certInfo.loadCertInfo();
        displayCert();
    }

    private void findCertInfos() {
        File[] certFiles;
        final File externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        certFiles = externalStoragePublicDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                if (s.toLowerCase().endsWith("p12") || s.toLowerCase().endsWith("pfx")) {
                    Log.d(LOGTAG, "actepted file:" + s);
                    return true;
                }
                else {
                    return false;

                }
            }
        });
        Log.d(LOGTAG, "Found candidates:"+certFiles.length);

        Arrays.sort(certFiles, new Comparator<File>() {
            @Override
            public int compare(File file, File t1) {
                long diff=file.lastModified() - t1.lastModified();
                return (int)(diff/Math.abs(diff));
            }
        });

        certInfos=new ArrayList<CertificateInfo>();
        for (File f: certFiles){
            try {
                Log.d(LOGTAG,"ading certinf for: "+f.getName());
                certInfos.add(new CertificateInfo(f));
            }catch (Exception e){
                Log.w(LOGTAG,"Failed to ad d cert file:"+f.getAbsolutePath(), e);
            }

        }
        currentCert=0;
    }

    private void nextCert(){
        if (certInfos.size()>0) {
            currentCert = (currentCert + 1) % certInfos.size();
        }
    }

    private void displayCert(){
        TextView txtCertInfo=(TextView)findViewById(R.id.txtCertInfo);
        if (certInfos.size()>0 && currentCert>=0) {
            CertificateInfo certInfo = certInfos.get(currentCert);

            txtCertInfo.setText(certInfo.getInfo());

            if (certInfo.isLoaded()) {
                if (certInfo.isPlaygroundCert() || certInfo.isProductionCert()) {
                    findViewById(R.id.btnImport).setEnabled(true);
                } else {
                    findViewById(R.id.btnImport).setEnabled(false);
                }
                TextView txtPasswd=(TextView)findViewById(R.id.txtPassword);
                txtPasswd.setEnabled(false);
                txtPasswd.setText("");
            }
            else {
                findViewById(R.id.txtPassword).setEnabled(true);

            }

        }
        else {
            txtCertInfo.setText("Mezi staženými soubory nebyly nalezeny žádné certifikáty (p12 nebo pfx).");
            findViewById(R.id.btnImport).setEnabled(false);
            findViewById(R.id.txtPassword).setEnabled(false);
        }
    }
}
