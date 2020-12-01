package com.nashschultz.sketchit;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Arrays;
import java.util.Random;

public class NewWordActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    String name = "Nash";
    String[] nameList = new String[1];
    String userID = "testID";
    String gameID = "gameID";
    String[] idList = new String[1];
    Boolean isEven = false;
    int evenAddition = 1;
    String randomWord;
    int customCount;
    Boolean isHost = false;
    int time = 45;
    Boolean isRematch = false;
    Bundle bundle;
    ValueEventListener mListener;

    EditText wordField;
    TextView customLabel;
    Button customButton;
    Button submitButton;
    TextView mainLabel;
    AdView mAdView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_word);

        mDatabase = FirebaseDatabase.getInstance().getReference();


        wordField = (EditText) findViewById(R.id.chosenWord);
        customLabel = (TextView) findViewById(R.id.customWordLabel);
        customButton = (Button) findViewById(R.id.useCustomWord);
        submitButton = (Button) findViewById(R.id.enterChosenWord);
        mainLabel = (TextView) findViewById(R.id.choseWordLabel);
        wordField.setEnabled(false);

        bundle = getIntent().getExtras();
        gameID = bundle.getString("gameID");
        name = bundle.getString("name");
        userID = bundle.getString("userID");
        isHost = bundle.getBoolean("isHost");
        isRematch = bundle.getBoolean("isRematch");

        MobileAds.initialize(this, new OnInitializationCompleteListener() {

            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mAdView = findViewById(R.id.newWordAdView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        if (isHost) {
            mDatabase.child("games").child(gameID).child("round0").removeValue();
            // show hostDelete button
        }

        loadWordCount();
        loadPlayerList();
        loadTime();
    }

    private void deleteGame() {
        // add that code here
    }

    public void useCustomWord(View view) {
        if (customCount > 0) {
            // allow access to change field here
            wordField.setEnabled(true);
            String newCustom = "You have " + customCount + " custom tokens left";
            customLabel.setText(newCustom);
            mDatabase.child("users").child(userID).child("custom").setValue(customCount - 1);
        }
    }

    private void loadWordCount() {
        final int FILE_MAX_LINE_INDEX = 577;//if your file has 5000 lines, and it's content never be changed
        Random rnd = new Random();
        final int lineIndex = rnd.nextInt(FILE_MAX_LINE_INDEX);
        try {
            LineNumberReader lnr = new LineNumberReader(new InputStreamReader(getAssets().open("wordlist.txt")));
            String s;
            while ((s = lnr.readLine()) != null) {
                if (lnr.getLineNumber() == lineIndex) {
                    if (s.compareTo("") != 0) {
                        randomWord = s;
                        lnr.close();
                    } else {
                        randomWord = "turtle";
                        lnr.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        wordField.setText(randomWord);
        // read string to txt code here
        final ValueEventListener postListener = new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                Long count = (Long) dataSnapshot.getValue();
                if (count != null) {
                    customCount = Math.toIntExact(count);
                    String newCustom = "You have " + customCount + " custom tokens left";
                    customLabel.setText(newCustom);
                    // adjust label here
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

    }

    public void submitWord(View view) {
        // if text field is not empty
        writeWordToDB();
        // adjust the UI here

        mDatabase.child("games").child(gameID).child("round0").addValueEventListener(mListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() == idList.length + evenAddition) {
                    // everyone submitted
                    // remove observers
                    // go to next page
                    bundle.putStringArray("idList", idList);
                    bundle.putStringArray("nameList", nameList);
                    bundle.putBoolean("isEven", isEven);
                    bundle.putBoolean("isHost", isHost);
                    bundle.putInt("round", 1);
                    bundle.putInt("time", time);

                    mDatabase.child("games").child(gameID).child("round0").removeEventListener(mListener);
                    Intent intent = new Intent(NewWordActivity.this, DrawActivity.class);
                    intent.putExtras(bundle);
                    startActivity(intent);
                } else if (!dataSnapshot.exists()) {
                    // game does not exist
                    // remove observers back t
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Getting Post failed, log a message
                // ...
            }
        });
    }

    private void writeWordToDB() {
        // if text field is not empty
        if (wordField.getText().toString().compareTo("") != 0) {
            String textValue = wordField.getText().toString();
            mDatabase.child("games").child(gameID).child("round0").child(userID).setValue(textValue);
            wordField.setVisibility(View.INVISIBLE);
            mainLabel.setText("Waiting for other players...");
            submitButton.setVisibility(View.INVISIBLE);
            customButton.setVisibility(View.INVISIBLE);
        } else {
            System.out.println("word is empty");
        }
    }

    private void loadPlayerList() {
        final ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int count = 0;
                idList = new String[1];
                nameList = new String[1];
                if (dataSnapshot.getChildrenCount() % 2 == 0) {
                    isEven = true;
                    evenAddition = 0;
                }
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    // TODO: handle the post
                    String name = (String) postSnapshot.child("name").getValue();
                    String id = (String) postSnapshot.getKey();
                    if (count == 0) {
                        nameList[0] = name;
                        idList[0] = id;
                    } else {
                        nameList = addX(nameList.length, nameList, name);
                        idList = addX(idList.length, idList, id);
                    }
                    count = count + 1;
                }
                if (idList.length % 2 != 0) {
                    organizeIdList();
                } else {
                    setUserAtFront();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Getting Post failed, log a message
                // ...
                System.out.println("failed");
            }
        };
        mDatabase.child("games").child(gameID).child("players").orderByChild("timestamp").addListenerForSingleValueEvent(postListener);
    }

    private void organizeIdList() {
        int indexOfSelf = Arrays.asList(idList).indexOf(userID);
        String[] before = new String[1];
        String[] after = new String[1];
        for(int i = 0; i < idList.length; i++) {
            if (i < indexOfSelf) {
                if(before[0] == null) {
                    before[0] = idList[0];
                } else {
                    before = addX(before.length, before, idList[i]);
                }
            } else if (i > indexOfSelf) {
                if(after[0] == null) {
                    after[0] = idList[i];
                } else {
                    after = addX(after.length, after, idList[i]);
                }
            }
        }
        String[] beforeName = new String[1];
        String[] afterName = new String[1];
        for(int i = 0; i < nameList.length; i++) {
            if (i < indexOfSelf) {
                if(beforeName[0] == null) {
                    beforeName[0] = nameList[0];
                } else {
                    beforeName = addX(beforeName.length, beforeName, nameList[i]);
                }
            } else if (i > indexOfSelf) {
                if(afterName[0] == null) {
                    afterName[0] = nameList[i];
                } else {
                    afterName = addX(afterName.length, afterName, nameList[i]);
                }
            }
        }
        if (before[0] == null) {
            idList = after;
            nameList = afterName;
        } else if (after[0] == null) {
            idList = before;
            nameList = beforeName;
        } else {
            idList = concatenateArrays(after, before);
            nameList = concatenateArrays(afterName, beforeName);
        }
    }

    private void setUserAtFront() {
        int indexOfSelf = Arrays.asList(idList).indexOf(userID);
        String[] before = new String[1];
        String[] after = new String[1];
        for(int i = 0; i < idList.length; i++) {
            if (i < indexOfSelf) {
                if(before[0] == null) {
                    before[0] = idList[0];
                } else {
                    before = addX(before.length, before, idList[i]);
                }
            } else {
                if(after[0] == null) {
                    after[0] = idList[indexOfSelf];
                } else {
                    after = addX(after.length, after, idList[i]);
                }
            }
        }
        String[] beforeName = new String[1];
        String[] afterName = new String[1];
        for(int i = 0; i < nameList.length; i++) {
            if (i < indexOfSelf) {
                if(beforeName[0] == null) {
                    beforeName[0] = nameList[0];
                } else {
                    beforeName = addX(beforeName.length, beforeName, nameList[i]);
                }
            } else {
                if(afterName[0] == null) { // check if null not if first input
                    afterName[0] = nameList[indexOfSelf];
                } else {
                    afterName = addX(afterName.length, afterName, nameList[i]);
                }
            }
        }
        if (before[0] == null) {
            idList = after;
            nameList = afterName;
        } else if (after[0] == null) {
            idList = before;
            nameList = beforeName;
        } else {
            idList = concatenateArrays(after, before);
            nameList = concatenateArrays(afterName, beforeName);
        }
    }

    private String[] concatenateArrays(String[] firstArray, String[] secondArray) {
        String[]c = new String[firstArray.length+secondArray.length];
        int count = 0;

        for(int i = 0; i < firstArray.length; i++) {
            c[i] = firstArray[i];
            count++;
        }
        for(int j = 0; j < secondArray.length;j++) {
            c[count++] = secondArray[j];
        }
        return c;
    }


    private void loadTime() {
        final ValueEventListener postListener = new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                Long newTime = (Long) dataSnapshot.getValue();
                if (newTime != null) {
                    time = Math.toIntExact(newTime);
                }
                submitButton.setVisibility(View.VISIBLE);
                // ...
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Getting Post failed, log a message
                // ...
                System.out.println("failed");
            }
        };
        mDatabase.child("games").child(gameID).child("time").addListenerForSingleValueEvent(postListener);
    }

    @Override
    public void onBackPressed() {
        // do nothing
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
}
