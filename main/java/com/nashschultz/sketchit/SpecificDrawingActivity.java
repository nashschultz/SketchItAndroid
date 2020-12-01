package com.nashschultz.sketchit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ListView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
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

public class SpecificDrawingActivity extends AppCompatActivity {

    String[] wordList;
    String userID;
    String gameID;
    int roundCount;
    DatabaseReference mDatabase;
    String name;
    int[] photoIDs;
    Bundle bundle;
    ArrayList<SubjectData> arrayList;
    ListView list;
    AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_specific_drawing);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        bundle = getIntent().getExtras();
        userID = bundle.getString("id");
        gameID = bundle.getString("gameID");
        roundCount = bundle.getInt("round");
        name = bundle.getString("name");
        list = (ListView) findViewById(R.id.otherChainListView);
        arrayList = new ArrayList<SubjectData>();

        MobileAds.initialize(this, new OnInitializationCompleteListener() {

            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mAdView = findViewById(R.id.specificAdView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        loadData();
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

    private void updateList() {
        CustomAdapter customAdapter = new CustomAdapter(this, arrayList);
        list.setAdapter(customAdapter);
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
}
