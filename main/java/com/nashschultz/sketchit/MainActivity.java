package com.nashschultz.sketchit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardedAd;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;


public class MainActivity extends AppCompatActivity {

    EditText nameField;
    String userID;
    TextView customLabel;
    int customCount;


    private FirebaseAuth mAuth;
    DatabaseReference mDatabase;
    RewardedAd rewardedAd;
    AdView mAdView;
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        sharedPref = getSharedPreferences("myPref", MODE_PRIVATE);

        rewardedAd = new RewardedAd(this,
                "ca-app-pub-5912556187565517/8869750983");

        RewardedAdLoadCallback adLoadCallback = new RewardedAdLoadCallback() {
            @Override
            public void onRewardedAdLoaded() {
                // Ad successfully loaded.
            }

            @Override
            public void onRewardedAdFailedToLoad(int errorCode) {
                // Ad failed to load.
            }
        };
        rewardedAd.loadAd(new AdRequest.Builder().build(), adLoadCallback);

        mAdView = findViewById(R.id.menuAdView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        nameField = (EditText) findViewById(R.id.nameField);
        customLabel = (TextView) findViewById(R.id.customTokensMenuLabel);
        mAuth = FirebaseAuth.getInstance();

        String name = sharedPref.getString("user_id", "");
        nameField.setText(name);
        signIn();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    private void signIn() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            assert user != null;
                            userID = user.getUid();
                            final ValueEventListener postListener = new ValueEventListener() {
                                @RequiresApi(api = Build.VERSION_CODES.N)
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    // Get Post object and use the values to update the UI
                                    Long count = (Long) dataSnapshot.getValue();
                                    if (count != null) {
                                        customCount = Math.toIntExact(count);
                                        String newCustom = "You have " + customCount + " custom words";
                                        customLabel.setText(newCustom);
                                        // adjust label here
                                    } else {
                                        mDatabase.child("users").child(userID).child("custom").setValue(3);
                                        String customText = "You have 3 custom words!";
                                        customLabel.setText(customText);
                                    }
                                    // ...
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    // Getting Post failed, log a message
                                    // ...
                                    System.out.println("failed");
                                }
                            };
                            mDatabase.child("users").child(userID).child("custom").addListenerForSingleValueEvent(postListener);
                        } else {
                            // If sign in fails, display a message to the user.

                        }

                        // ...
                    }
                });
    }

    public void createGame(View view) {
        if (nameField.getText().toString().compareTo("") != 0) {
            sharedPref.edit().putString("user_id", nameField.getText().toString()).apply();
            Intent intent = new Intent(MainActivity.this, CreateGameActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("name", nameField.getText().toString());
            bundle.putString("userID", userID);
            bundle.putBoolean("isRematch", false);
            System.out.println(userID);
            intent.putExtras(bundle);
            startActivity(intent);
        } else {

        }
    }

    @Override
    public void onBackPressed() {
        // do nothing
    }



    public void joinGame(View view) {
        if (nameField.getText().toString().compareTo("") != 0) {
            sharedPref.edit().putString("user_id", nameField.getText().toString()).apply();
            Intent intent = new Intent(MainActivity.this, JoinGameActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("name", nameField.getText().toString());
            bundle.putString("userID", userID);
            bundle.putBoolean("isRematch", false);
            intent.putExtras(bundle);
            startActivity(intent);
        } else {

        }
    }

    public void getThreeWords(View view) {
        if (rewardedAd.isLoaded()) {
            Activity activityContext = this;
            RewardedAdCallback adCallback = new RewardedAdCallback() {
                @Override
                public void onRewardedAdOpened() {
                    // Ad opened.
                }

                @Override
                public void onRewardedAdClosed() {
                    // Ad closed.
                }

                @Override
                public void onUserEarnedReward(@NonNull RewardItem reward) {
                    // User earned reward.
                    customCount = customCount + 3;
                    mDatabase.child("users").child(userID).child("custom").setValue(customCount);
                    String newLabel = "You have " + customCount + " custom words!";
                    customLabel.setText(newLabel);

                }

                @Override
                public void onRewardedAdFailedToShow(int errorCode) {
                    // Ad failed to display.
                }
            };
            rewardedAd.show(activityContext, adCallback);
        } else {
            Log.d("TAG", "The rewarded ad wasn't loaded yet.");
        }
    }


}