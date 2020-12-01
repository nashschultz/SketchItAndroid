package com.nashschultz.sketchit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class JoinGameActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    String finalID;
    String name;
    String[] nameList = new String[1];
    String userID;
    Boolean isRematch = false;
    Boolean isInGame = false;
    ListView listView;
    ArrayAdapter adapter;
    TextView playerLabel;
    TextView waitingLabel;
    TextView enterCodeLabel;
    EditText gameCodeInput;
    Button doneButton;
    Button shareButton;
    Bundle bundle;
    ValueEventListener mListener;
    ValueEventListener lockListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_game);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        listView = (ListView) findViewById(R.id.joinPlayerListView);
        playerLabel = (TextView) findViewById(R.id.joinPlayerLabel);
        waitingLabel = (TextView) findViewById(R.id.waitingForHostLabel);
        gameCodeInput = (EditText) findViewById(R.id.gameCodeInput);
        doneButton = (Button) findViewById(R.id.submitCode);
        shareButton = (Button) findViewById(R.id.joinShareButton);
        enterCodeLabel = (TextView) findViewById(R.id.joinGameCodeLabel);
        bundle = getIntent().getExtras();
        name = bundle.getString("name");
        userID = bundle.getString("userID");
        isRematch = bundle.getBoolean("isRematch");

        checkIfRematch();
    }

    private void checkIfRematch() {
        if (isRematch) {
            finalID = bundle.getString("finalGameID");
            HashMap<String, Object> result = new HashMap<>();
            result.put("name", name);
            result.put("timestamp", ServerValue.TIMESTAMP);

            mDatabase.child("games").child(finalID).child("players").child(userID).setValue(result);

            gameCodeInput.setText(finalID);
            getPlayerList();
            // set up layout views
        }
    }

    public void submitGameCode(View view) {
        // if text is not empty
        if (gameCodeInput.getText() != null) {
            final String gameID = gameCodeInput.getText().toString();
            final ValueEventListener postListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Long numPlayers = (Long) dataSnapshot.child("count").getValue();
                    Long isLocked = (Long) dataSnapshot.child("lock").getValue();
                    try {
                        if (numPlayers != null && numPlayers < 12 && isLocked != 1) {
                            finalID = gameID;
                            HashMap<String, Object> player = new HashMap<>();
                            player.put("name", name);
                            player.put("timestamp", ServerValue.TIMESTAMP);

                            mDatabase.child("games").child(finalID).child("players").child(userID).setValue(player);
                            getPlayerList();
                        } else {
                            System.out.println("game does not exist");
                        }
                    } catch (NullPointerException e) {
                        System.out.println("error game not exist");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Getting Post failed, log a message
                    // ...
                    System.out.println("failed");
                }
            };
            mDatabase.child("games").child(gameID).addListenerForSingleValueEvent(postListener);
        }
    }

    private void getPlayerList() {
        waitingLabel.setVisibility(View.VISIBLE);
        playerLabel.setVisibility(View.VISIBLE);
        listView.setVisibility(View.VISIBLE);
        doneButton.setVisibility(View.INVISIBLE);
        //shareButton.setVisibility(View.VISIBLE);
        enterCodeLabel.setVisibility(View.INVISIBLE);
        gameCodeInput.setEnabled(false);

        mDatabase.child("games").child(finalID).child("players").addValueEventListener(mListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;
                nameList = new String[1];
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    // TODO: handle the post
                    String name = (String) postSnapshot.child("name").getValue();
                    String id = (String) postSnapshot.getKey();
                    if (count == 0) {
                        nameList[0] = name;
                    } else {
                        nameList = addX(nameList.length, nameList, name);
                    }
                    if (postSnapshot.getKey().compareTo(userID) == 0) {
                        isInGame = true;
                    }
                    count = count + 1;
                }
                if (!isInGame) {
                    // remove observers here
                    // go back to lobby
                    Intent intent = new Intent(JoinGameActivity.this, MainActivity.class);
                    startActivity(intent);
                    System.out.println("not in game");
                    onBackPressed();
                }
                String players = "Players: " + count + "/12";
                playerLabel.setText(players);
                updateList();
                // reload data here and set up UI
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Getting Post failed, log a message
                // ...
            }
        });
        mDatabase.child("games").child(finalID).child("lock").addValueEventListener(lockListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    Long lockValue = (Long) dataSnapshot.getValue();
                    if (lockValue == 1) {
                        // remove all observers game started
                        // go to next screen
                        mDatabase.child("games").child(finalID).child("players").removeEventListener(mListener);
                        mDatabase.child("games").child(finalID).child("lock").removeEventListener(lockListener);
                        bundle.putString("gameID", finalID);
                        Intent intent = new Intent(JoinGameActivity.this, NewWordActivity.class);
                        intent.putExtras(bundle);
                        startActivity(intent);
                    }
                } catch (NullPointerException e) {
                    // remove all observers
                    // go back to lobby
                }
                // reload data here and set up UI
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Getting Post failed, log a message
                // ...
            }
        });
    }

    private void updateList() {
        adapter = new ArrayAdapter<String>(this,R.layout.activity_playerlistview,nameList);
        listView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        if (finalID != null) {
            // remove all observers
            mDatabase.child("games").child(finalID).child("players").removeEventListener(mListener);
            mDatabase.child("games").child(finalID).child("lock").removeEventListener(lockListener);
            mDatabase.child("games").child(finalID).child("players").child(userID).removeValue();
        }
        Intent intent = new Intent(JoinGameActivity.this, MainActivity.class);
        startActivity(intent);
        // leave back to lobby
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
