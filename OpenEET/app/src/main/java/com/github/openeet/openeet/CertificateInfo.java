package com.github.openeet.openeet;

import android.content.Context;
import android.content.res.AssetManager;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rasekl on 11/1/16.
 */
public class CertificateInfo{
    private static final String LOGTAG="CertificateInfo";

    private static final String PLAYGROUND_CA_ASSETS="certificates/ca/playground";
    private static final String PRODUCTION_CA_ASSETS="certificates/ca/production";
    public static final String KEYSTORE_PRIVATE_STREAM = "keystore";
    public static final String KEYSTORE_PASSWORD_PRIVATE_STREAM = "keystore.password";
    public static final String KEYSTORE_ALIAS = "klic";

    private File file;
    private String info;
    private String passwd;
    private X509Certificate certificate;
    private PrivateKey key;
    private boolean loaded;
    static private List<X509Certificate> playgroundCa;
    static private List<X509Certificate> productionCa;
    private String fileType;
    private Pattern dicPattern;


    public CertificateInfo(File certFile){
        initCaCerts();
        Log.d(LOGTAG,"Creating instance for "+certFile.getName());
        if (certFile==null) throw new NullPointerException("certificate file cant be null");
        //if (certFile.canRead()) throw new IllegalArgumentException("file can't be read:"+certFile.getAbsolutePath());
        this.file=certFile;
        this.info= certFile.getName() + "\n\nPro zobrazení detailu certifikátu zadejte heslo";
        this.passwd=null;
        this.loaded=false;
        this.fileType="PKCS12";
        this.dicPattern=Pattern.compile(".*,CN=(CZ[0-9]+),.*");
    }

    private void loadCertsFromAssets(List<X509Certificate> certs, String assetPath) throws IOException, CertificateException {
        Log.d(LOGTAG,"Loading certs from asset: "+assetPath);
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        String[] list = assetManager.list(assetPath);
        Log.d(LOGTAG,"Found ca cert files: "+list.length);
        for(String item: list) {
            Log.d(LOGTAG,"Loading ca cert:"+item);
            try {
                X509Certificate cert = (X509Certificate) cf.generateCertificate(assetManager.open(assetPath + "/" + item,AssetManager.ACCESS_RANDOM));
                certs.add(cert);
                Log.d(LOGTAG,"Certificate added: "+item);
            }
            catch (Exception e){
                Log.e(LOGTAG, "error loading cert from asset",e);
            }
        }

    }

    public PrivateKey getKey(){
        return key;
    }

    public X509Certificate getCertificate(){
        return certificate;
    }

    private void initCaCerts(){
        Log.d(LOGTAG,"Init ca certs");

        if (assetManager==null) throw new RuntimeException("internal error - missing asset manager");
        try {
            if (productionCa == null) {
                productionCa = new ArrayList<X509Certificate>();
                loadCertsFromAssets(productionCa,PRODUCTION_CA_ASSETS);
            }

            if (playgroundCa == null) {
                playgroundCa = new ArrayList<X509Certificate>();
                loadCertsFromAssets(playgroundCa, PLAYGROUND_CA_ASSETS);
            }
        }
        catch (Exception ex){
            throw new RuntimeException("Fatal failure - init ca certs");
        }

    }

    public boolean isPlaygroundCert(){
        return isIssuedBy(playgroundCa);
    }

    public boolean isProductionCert(){
        return isIssuedBy(productionCa);
    }

    private boolean isIssuedBy(List<X509Certificate> ca){
        for (X509Certificate c: ca) {
            Log.d(LOGTAG,String.format("\nIssuer: %s\nCAsubj: %s",
                    certificate.getIssuerX500Principal().getName().toString(),
                    c.getIssuerX500Principal().getName().toString()));
            if (certificate.getIssuerX500Principal().getName().toString().equals(c.getIssuerX500Principal().getName().toString()))
                return true;
        }
        return false;
    }

    public void setPasswd(String passwd){
        this.passwd=passwd;
        loadCertInfo();
    }

    public String getInfo(){
        return info;
    }

    public File getFile(){
        return file;
    }

    public void setFileType(String fileType){
        this.fileType=fileType;
    }

    public void loadCertInfo() {
        Log.d(LOGTAG,"loading cert: "+file.getName());
        if (passwd == null) {
            throw new IllegalStateException("password must be set before loading cert info");
        }
        try {
            KeyStore ks = KeyStore.getInstance(fileType);
            ks.load(new FileInputStream(file), passwd.toCharArray());

            String alias;
            Enumeration<String> aliases=ks.aliases();
            while (aliases.hasMoreElements()){
                alias=aliases.nextElement();
                Log.d(LOGTAG,"Alias in pkcs12:"+alias);
                if (ks.isKeyEntry(alias)) {
                    Log.d(LOGTAG,"Alias id key entry:"+alias);
                    //if (ks.isCertificateEntry(alias)) {
                        Log.d(LOGTAG,"Alias is cert enry"+alias);
                        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                        Log.d(LOGTAG,"Certificate in question:"+cert.toString());
                        PrivateKey key=(PrivateKey)ks.getKey(alias,passwd.toCharArray());

                        this.certificate=cert;
                        this.key=key;
                        this.loaded=true;
                        info=String.format("\nNačten ze souboru:\n%s\n\nVydán pro:\n%s\n\nProstředí:\n%s\n\nPlatný od:\n%s\n\nPlatný do:\n%s",
                                file.getName(),
                                certificate.getSubjectDN().toString(),
                                isPlaygroundCert()?"Playground":isProductionCert()?"Produkční":"Nepodporovaný!",
                                certificate.getNotBefore().toString(),certificate.getNotAfter().toString());
                        Log.d(LOGTAG,"Found certificate:"+this.certificate.toString());
                    break;
                    //}
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean isLoaded(){
        return loaded;
    }

    public void store(){
        try {
            OutputStream storeOStream=context.openFileOutput(KEYSTORE_PRIVATE_STREAM,Context.MODE_PRIVATE);
            OutputStream passwordOStream=context.openFileOutput(KEYSTORE_PASSWORD_PRIVATE_STREAM,Context.MODE_PRIVATE);
            String password = UUID.randomUUID().toString();
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null,null);
            ks.setKeyEntry(KEYSTORE_ALIAS,key, password.toCharArray(),new X509Certificate[]{certificate});
            ks.store(storeOStream,password.toCharArray());
            storeOStream.close();
            passwordOStream.write(password.getBytes("utf-8"));
            passwordOStream.close();
        }
        catch (Exception e){
            Log.e(LOGTAG,"Exception while saving cert/key",e);
            throw new RuntimeException("Exception while saving cert/key",e);
        }
    }

    public static CertificateInfo load(){
        try {
            File storeFile=context.getFileStreamPath(KEYSTORE_PRIVATE_STREAM);
            InputStream storePassStream=context.openFileInput(KEYSTORE_PASSWORD_PRIVATE_STREAM);
            ByteArrayOutputStream bos=new ByteArrayOutputStream();
            byte [] buf=new byte[200];
            int n;
            while((n=storePassStream.read(buf))>=0) bos.write(buf,0,n);
            String password=new String(bos.toByteArray(),"utf-8");
            CertificateInfo certInfo=new CertificateInfo(storeFile);
            certInfo.setPasswd(password);
            certInfo.loadCertInfo();
            if (certInfo.isLoaded())
                return certInfo;
            throw new IllegalStateException("faliued to load certificate");
        }
        catch (Exception e){
            Log.e(LOGTAG,"Exception while loading cert/key",e);
            throw new RuntimeException("Exception while loading cert/key",e);
        }
    }

    public String getDic(){
        if (!loaded) return null;
        String subj=certificate.getSubjectDN().toString();
        Matcher match=dicPattern.matcher(subj);
        if (!match.matches()) {
            Log.w(LOGTAG,"DIC not find in certificate subject:"+subj);
            return null;
        }
        String dic=match.group(1);
        return dic;
    }

    private static AssetManager assetManager;
    public static void setAssetManager(AssetManager _assetManager){
        assetManager=_assetManager;
    }

    private static Context context;
    public static void setContext(Context _context){
        context=_context;
        if (context!=null)
            assetManager=context.getAssets();
    }

}
