package com.example.calculatorvault;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.*;
import java.io.File;
import java.util.*;

public class VaultActivity extends AppCompatActivity{
 private VaultType type;private VaultStorage storage;private RecyclerView list;private TextView info;private final List<VaultItem> items=new ArrayList<>();private ActivityResultLauncher<String[]> picker;
 protected void onCreate(Bundle b){super.onCreate(b);type="FAKE".equals(getIntent().getStringExtra("VAULT_TYPE"))?VaultType.FAKE:VaultType.REAL;storage=new VaultStorage(this);storage.clearTemporaryFiles();picker=registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(),u->{if(u!=null)for(Uri x:u)try{storage.importFile(x,type);}catch(Exception e){Toast.makeText(this,"Erro ao importar arquivo.",Toast.LENGTH_SHORT).show();}load();});build();load();}
 private void build(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(24),dp(16),dp(12));TextView title=new TextView(this);title.setText(type==VaultType.REAL?"🔒 Cofre privado":"📁 Cofre alternativo");title.setTextSize(25);title.setGravity(Gravity.CENTER);root.addView(title);info=new TextView(this);info.setPadding(0,dp(10),0,dp(10));root.addView(info);Button add=new Button(this);add.setText("Adicionar fotos/vídeos");add.setOnClickListener(v->picker.launch(new String[]{"image/*","video/*"}));root.addView(add);list=new RecyclerView(this);list.setLayoutManager(new LinearLayoutManager(this));list.setAdapter(new VaultAdapter(items,this::open));root.addView(list,new LinearLayout.LayoutParams(-1,0,1));Button lock=new Button(this);lock.setText("🔒 Bloquear cofre");lock.setOnClickListener(v->finish());root.addView(lock);setContentView(root);}
 private void load(){items.clear();items.addAll(storage.getItems(type));list.getAdapter().notifyDataSetChanged();info.setText("Arquivos: "+items.size()+"\nEspaço usado: "+size(storage.getStorageUsed(type)));}
 private void open(VaultItem item){try{File f=storage.createTemporaryDecryptedFile(item.getFile(),item.getMimeType());Uri u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);Intent i=new Intent(Intent.ACTION_VIEW).setDataAndType(u,item.getMimeType()).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);}catch(Exception e){Toast.makeText(this,"Não foi possível abrir o arquivo.",Toast.LENGTH_SHORT).show();}}
 protected void onDestroy(){storage.clearTemporaryFiles();super.onDestroy();}
 private String size(long b){if(b<1024)return b+" B";if(b<1048576)return String.format("%.1f KB",b/1024.0);return String.format("%.1f MB",b/1048576.0);}
 private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
}