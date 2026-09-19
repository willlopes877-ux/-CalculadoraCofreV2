package com.example.calculatorvault;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.io.*;
import java.util.*;

public class VaultStorage {
 private final Context context; private final SharedPreferences meta;
 public VaultStorage(Context c){context=c.getApplicationContext();meta=context.getSharedPreferences("vault_metadata",Context.MODE_PRIVATE);}
 private File dir(VaultType t){File b=new File(context.getNoBackupFilesDir(),"vault");File d=new File(b,t==VaultType.REAL?"real":"fake");if(!d.exists())d.mkdirs();return d;}
 public File importFile(Uri uri,VaultType t)throws Exception{
  String mime=context.getContentResolver().getType(uri);if(mime==null||(!mime.startsWith("image/")&&!mime.startsWith("video/")))throw new IllegalArgumentException("Selecione somente fotos ou vídeos.");
  String name=queryName(uri);if(name==null)name=UUID.randomUUID().toString();String id=UUID.randomUUID().toString();File out=new File(dir(t),id+".vault");
  try(InputStream in=context.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Não foi possível abrir o arquivo.");VaultCrypto.encrypt(in,out);}
  meta.edit().putString(id+".name",name).putString(id+".mime",mime).apply();return out;
 }
 private String queryName(Uri uri){try(Cursor c=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())return c.getString(0);}catch(Exception ignored){}return null;}
 public List<VaultItem> getItems(VaultType t){List<VaultItem> r=new ArrayList<>();for(File f:getFiles(t)){String id=f.getName().replace(".vault","");r.add(new VaultItem(f,meta.getString(id+".name",f.getName()),meta.getString(id+".mime","application/octet-stream"),f.length(),f.lastModified()));}return r;}
 public List<File> getFiles(VaultType t){File[] a=dir(t).listFiles();List<File> r=new ArrayList<>();if(a!=null)for(File f:a)if(f.isFile()&&f.getName().endsWith(".vault"))r.add(f);return r;}
 public void deleteItem(VaultItem i){if(i==null)return;String id=i.getFile().getName().replace(".vault","");i.getFile().delete();meta.edit().remove(id+".name").remove(id+".mime").apply();}
 public File createTemporaryDecryptedFile(File encrypted,String mime)throws Exception{File d=new File(context.getCacheDir(),"vault_temp");if(!d.exists())d.mkdirs();String ext=mime!=null&&mime.startsWith("video/")?".mp4":".jpg";File out=new File(d,UUID.randomUUID()+ext);VaultCrypto.decrypt(encrypted,out);return out;}
 public void clearTemporaryFiles(){File d=new File(context.getCacheDir(),"vault_temp");File[] a=d.listFiles();if(a!=null)for(File f:a)if(f.isFile())f.delete();}
 public long getStorageUsed(VaultType t){long n=0;for(File f:getFiles(t))n+=f.length();return n;}
}