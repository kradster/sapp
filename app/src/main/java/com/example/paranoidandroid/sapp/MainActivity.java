package com.example.paranoidandroid.sapp;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static  MainActivity inst;
    ListView listView;
    ArrayList<String> smsListView = new ArrayList<String>();
    ArrayAdapter arrayAdapter;
    private BroadcastReceiver broadcastReceiver;

    public static MainActivity instance(){return inst;}

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVE");
        registerReceiver(broadcastReceiver,intentFilter);
        inst = this;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        broadcastReceiver = new SMSBReciever();
        listView = findViewById(R.id.smsList);
        arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,smsListView);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    String[] smsMessages = smsListView.get(position).split("\n");
                    String address = smsMessages[0];
                    String smsMessage = "";
                    for (int i = 1;i<smsMessages.length;++i){
                        smsMessage += smsMessages[i];
                    }
                    String smsMessageStr = address + "\n";
                    smsMessageStr += smsMessage;
                    Toast.makeText(MainActivity.this,smsMessageStr, Toast.LENGTH_SHORT).show();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        if(ContextCompat.checkSelfPermission(getBaseContext(),"android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED){
            refreshSmsInbox();
        }else{
            final int REQUEST_CODE_ASK_PERMISSION = 123;
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{"android.permission.READ_SMS"},REQUEST_CODE_ASK_PERMISSION);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 123:
                Toast.makeText(MainActivity.this, "its Working", Toast.LENGTH_SHORT).show();
                ContentResolver contentResolver = getContentResolver();
                Cursor smsInboxCurser = contentResolver.query(Uri.parse("content://sms/inbox"),null,null,null,null);
                int indexBody = smsInboxCurser.getColumnIndex("Body");
                int indexAddress = smsInboxCurser.getColumnIndex("address");
                if(indexBody < 0 || !smsInboxCurser.moveToFirst())return;
                do{
                    String str = "SMS From:" + smsInboxCurser.getString(indexAddress)+"\n"+smsInboxCurser.getString(indexBody)+"\n";
                    arrayAdapter.add(str);
                }while (smsInboxCurser.moveToNext());
                smsInboxCurser.close();
                break;
        }
    }

    private void refreshSmsInbox() {
        ContentResolver contentResolver = getContentResolver();
        Cursor smsInboxCurser = contentResolver.query(Uri.parse("content://sms/inbox"),null,null,null,null);
        int indexBody = smsInboxCurser.getColumnIndex("Body");
        int indexAddress = smsInboxCurser.getColumnIndex("address");
        if(indexBody < 0 || !smsInboxCurser.moveToFirst())return;
        arrayAdapter.clear();
        do{
            String str = "SMS From:" + smsInboxCurser.getString(indexAddress)+"\n"+smsInboxCurser.getString(indexBody)+"\n";
            arrayAdapter.add(str);
        }while (smsInboxCurser.moveToNext());
        smsInboxCurser.close();


    }
    public void updateList(final String smsMessage){
        arrayAdapter.insert(smsMessage,0);
        arrayAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(broadcastReceiver);
    }
}
