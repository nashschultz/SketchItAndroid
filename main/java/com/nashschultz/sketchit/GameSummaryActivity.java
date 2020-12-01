package com.nashschultz.sketchit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;

public class GameSummaryActivity extends AppCompatActivity {

    String[] wordList;
    String userID;
    String gameID;
    String name;
    String[] nameList;
    String[] idList;
    Boolean isHost = false;
    int roundCount;
    DatabaseReference mDatabase;
    int[] photoIDs;
    Boolean isEven = false;
    Boolean toLobby = false;
    Bundle bundle;
    ArrayList<SubjectData> arrayList;
    ListView list;
    private InterstitialAd mInterstitialAd;
    ValueEventListener mListener;

    Button rematchButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_summary);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        list = findViewById(R.id.gameSummaryList);
        arrayList = new ArrayList<SubjectData>();

        bundle = getIntent().getExtras();
        assert bundle != null;
        userID = bundle.getString("userID");
        gameID = bundle.getString("gameID");
        name = bundle.getString("name");
        nameList = bundle.getStringArray("nameList");
        idList = bundle.getStringArray("idList");
        isHost = bundle.getBoolean("isHost");
        roundCount = bundle.getInt("round");
        isEven = bundle.getBoolean("isEven");

        rematchButton = (Button) findViewById(R.id.rematchButton);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {

            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {}
        });
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-5912556187565517/8351355168");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        if(isEven) {
            int indexOfSelf = Arrays.asList(idList).indexOf(userID);
            idList = removeTheElement(idList, indexOfSelf);
            nameList = removeTheElement(nameList, indexOfSelf);
            bundle.putStringArray("idList", idList);
            bundle.putStringArray("nameList", nameList);
        }
        loadData();
        observeForRematch();
    }

    private void loadData() {
        wordList = new String[1];
        photoIDs = new int[1];
        for(int i = 0; i <= roundCount; i = i + 2) {
            String path = "round" + i;
            final int finalI = i;
            final ValueEventListener postListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String word = (String) dataSnapshot.getValue();
                    if (finalI == 0) {
                        wordList[0] = word;
                    } else {
                        wordList = addX(wordList.length, wordList, word);
                    }
                    if (finalI != roundCount) {
                        if (finalI == 0) {
                            photoIDs[0] = finalI + 1;
                        } else {
                            photoIDs = addXInt(photoIDs.length, photoIDs, finalI + 1);
                        }
                    }
                    if (finalI == roundCount || (finalI + 1) == roundCount) {
                        // reload the listview here
                        loadImages();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Getting Post failed, log a message
                    // ...
                    System.out.println("failed");
                }
            };
            mDatabase.child("games").child(gameID).child(path).child(userID).addListenerForSingleValueEvent(postListener);
            mDatabase.child("games").child(gameID).child("players").child(userID).removeValue();

        }
    }

    private void loadImages() {
        for(int i = 0; i < photoIDs.length; i++) {
            String path = userID + ".jpg";
            String roundPath = "round" + photoIDs[i];
            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("games").child(gameID).child(roundPath).child(path);
            final long ONE_MEGABYTE = 1024 * 1024;
            final int finalI = i;
            storageReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    // Data for "images/island.jpg" is returns, use this as needed
                    if (finalI == 0) {
                        arrayList.add(new SubjectData(wordList[finalI + 1], bytes, wordList[0]));
                    } else {
                        arrayList.add(new SubjectData(wordList[finalI], bytes, ""));
                    }
                    if (arrayList.size() == wordList.length - 1) {
                        updateList();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                }
            });
        }
    }

    private void updateList() {
        CustomAdapter customAdapter = new CustomAdapter(this, arrayList);
        list.setAdapter(customAdapter);
    }

    public void goBackToLobby(View view) {
        // remove observers
        toLobby = true;
        // present interstital her
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            goToLobby();
        }
        mInterstitialAd.setAdListener(new AdListener() {

            @Override
            public void onAdClosed() {
                // Code to be executed when the interstitial ad is closed.
                goToLobby();
            }
        });
    }

    @Override
    public void onBackPressed() {
        goBackToLobby(null);
    }

    private void goToLobby() {
        if (mListener != null) {
            mDatabase.child("games").child(gameID).child("lock").removeEventListener(mListener);
        }
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            Intent intent = new Intent(GameSummaryActivity.this, MainActivity.class);
            startActivity(intent);
        }
        mInterstitialAd.setAdListener(new AdListener() {

            @Override
            public void onAdClosed() {
                // Code to be executed when the interstitial ad is closed.
                Intent intent = new Intent(GameSummaryActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    public void seeOtherDrawings(View view) {
        Intent intent = new Intent(GameSummaryActivity.this, GameSpecificActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void rematchClicked(View view) {
        // remove Observers
        if (isHost) {
            // set rematch button here
            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            } else {
                goToCreate();
            }
            mInterstitialAd.setAdListener(new AdListener() {

                @Override
                public void onAdClosed() {
                    // Code to be executed when the interstitial ad is closed.
                    goToCreate();
                }
            });
        } else {
            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            } else {
                goToJoin();
            }
            mInterstitialAd.setAdListener(new AdListener() {

                @Override
                public void onAdClosed() {
                    // Code to be executed when the interstitial ad is closed.
                    goToJoin();
                }
            });
        }
    }

    private void observeForRematch() {
        final String rematch = "Rematch";
        if (isHost) {
            // set rematch button here
            rematchButton.setAlpha(1);
            rematchButton.setText(rematch);
            rematchButton.setEnabled(true);
        } else {
            mListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    try {
                        Long lockValue = (Long) dataSnapshot.getValue();
                        if (lockValue == 0) {
                            rematchButton.setAlpha(1);
                            rematchButton.setText(rematch);
                            rematchButton.setEnabled(true);
                        }
                        if (rematchButton.getAlpha() == 1 && lockValue == 1) {
                            goToLobby();
                        }
                    } catch (NullPointerException e) {
                        System.out.println(e);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Getting Post failed, log a message
                    // ...
                    System.out.println("failed");
                }
            };
            mDatabase.child("games").child(gameID).child("lock").addValueEventListener(mListener);
        }
    }

    private void goToCreate() {
        Intent intent = new Intent(GameSummaryActivity.this, CreateGameActivity.class);
        Bundle newBundle = new Bundle();
        newBundle.putString("name", name);
        newBundle.putString("userID", userID);
        newBundle.putBoolean("isRematch", true);
        newBundle.putString("finalGameID", gameID);
        intent.putExtras(newBundle);
        startActivity(intent);
    }

    private void goToJoin() {
        Intent intent = new Intent(GameSummaryActivity.this, JoinGameActivity.class);
        Bundle newBundle = new Bundle();
        mDatabase.child("games").child(gameID).child("lock").removeEventListener(mListener);
        newBundle.putString("name", name);
        newBundle.putString("userID", userID);
        newBundle.putBoolean("isRematch", true);
        newBundle.putString("finalGameID", gameID);
        intent.putExtras(newBundle);
        startActivity(intent);
    }

    public static String[] addX(int n, String[] arr, String x)
    {
        int i;

        // create a new array of size n+1
        String[] newarr = new String[n + 1];

        // insert the elements from
        // the old array into the new array
        // insert all elements till n
        // then insert x at n+1
        for (i = 0; i < n; i++)
            newarr[i] = arr[i];

        newarr[n] = x;

        return newarr;
    }

    public static int[] addXInt(int n, int[] arr, int x)
    {
        int i;

        // create a new array of size n+1
        int[] newarr = new int[n + 1];

        // insert the elements from
        // the old array into the new array
        // insert all elements till n
        // then insert x at n+1
        for (i = 0; i < n; i++)
            newarr[i] = arr[i];

        newarr[n] = x;

        return newarr;
    }

    public static String[] removeTheElement(String[] arr,
                                         int index)
    {

        // If the array is empty
        // or the index is not in array range
        // return the original array
        if (arr == null
                || index < 0
                || index >= arr.length) {

            return arr;
        }

        // Create another array of size one less
        String[] anotherArray = new String[arr.length - 1];

        // Copy the elements except the index
        // from original array to the other array
        for (int i = 0, k = 0; i < arr.length; i++) {

            // if the index is
            // the removal element index
            if (i == index) {
                continue;
            }

            // if the index is not
            // the removal element index
            anotherArray[k++] = arr[i];
        }

        // return the resultant array
        return anotherArray;
    }
}
