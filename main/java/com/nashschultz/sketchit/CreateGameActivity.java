package com.nashschultz.sketchit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.renderscript.Sampler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Random;

public class CreateGameActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    Boolean isRematch = false;
    String finalID;
    String name;
    String userID;
    String[] idList = new String[1];
    String[] nameList = new String[1];
    ArrayAdapter adapter;
    ListView listView;
    Button startButton;
    Bundle bundle;

    private ValueEventListener mListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_game);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        listView = (ListView) findViewById(R.id.playerListView);
        startButton = (Button) findViewById(R.id.startButton);
        bundle = getIntent().getExtras();
        assert bundle != null;
        name = bundle.getString("name");
        userID = bundle.getString("userID");
        isRematch = bundle.getBoolean("isRematch");
        createGame();
    }

    private void createGame() {
        if (!isRematch) {
            Random rand = new Random();
            int rand_int1 = rand.nextInt(1000000 - 100000 + 1) + 100000;
            System.out.println(rand_int1);
            final String tempID = Integer.toString(rand_int1);
            final ValueEventListener postListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Get Post object and use the values to update the UI
                    Long count = (Long) dataSnapshot.getValue();
                    if (count == null) {
                        finalID = tempID;
                        codeGenerated();
                    } else {
                        createGame();
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
            mDatabase.child("games").child(tempID).child("count").addListenerForSingleValueEvent(postListener);
        } else {
            finalID = bundle.getString("finalGameID");
            codeGenerated();
        }
    }

    private void codeGenerated() {
        TextView tv = (TextView) findViewById(R.id.gameCodeInputLabel);
        tv.setText(finalID);
        HashMap<String, Object> result = new HashMap<>();
        result.put("count", 1);
        result.put("round", 0);
        result.put("lock", 0);
        result.put("timestamp", ServerValue.TIMESTAMP);

        HashMap<String, Object> player = new HashMap<>();
        player.put("name", name);
        player.put("timestamp", ServerValue.TIMESTAMP);

        mDatabase.child("games").child(finalID).updateChildren(result);
        mDatabase.child("games").child(finalID).child("players").child(userID).setValue(player);

        mDatabase.child("games").child(finalID).child("players").addValueEventListener(
                mListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;
                nameList = new String[1];
                idList = new String[1];
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
                System.out.println(nameList[0]);
                updateList();
                if (nameList.length > 1) {
                    // THE GAME CAN BE STARTED HERE
                    // Make button visible
                    startButton.setAlpha(1);
                    startButton.setText("Start Game");

                }
                TextView playerCount = (TextView) findViewById(R.id.playerCountLabel);
                String players = "Players: " + count + "/12";
                playerCount.setText(players);
                mDatabase.child("games").child(finalID).child("count").setValue(nameList.length);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Getting Post failed, log a message
                // ...
            }
        });
    }

    private void updateList() {
        adapter = new ArrayAdapter<String>(this, R.layout.activity_playerlistview, nameList);
        listView.setAdapter(adapter);
    }


    public void startGame(View view) {
        if (startButton.getAlpha() == 1) {
            HashMap<String, Object> player = new HashMap<>();
            player.put("time", 45);
            player.put("lock", 1);
            mDatabase.child("games").child(finalID).child("players").removeEventListener(mListener);
            mDatabase.child("games").child(finalID).updateChildren(player);
            bundle.putString("gameID", finalID);
            bundle.putBoolean("isHost", true);
            Intent intent = new Intent(CreateGameActivity.this, NewWordActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
        }
    }

    @Override
    public void onBackPressed() {
        leaveGame();
        Intent intent = new Intent(CreateGameActivity.this, MainActivity.class);
        startActivity(intent);
    }

    private void leaveGame() {
        //mDatabase.child("games").child(finalID).child("players").removeEventListener();
        // need to remove the listeners
        if (finalID != null && !isRematch) {
            mDatabase.child("games").child(finalID).child("players").removeEventListener(mListener);
            mDatabase.child("games").child(finalID).removeValue();
        } else if (finalID != null) {
            mDatabase.child("games").child(finalID).child("players").removeEventListener(mListener);
            mDatabase.child("games").child(finalID).child("players").removeValue();
            mDatabase.child("games").child(finalID).child("lock").setValue(1);
        }
        // go back to menu
    }

    private void removePlayer() {
        String selectedPlayer = "";
        mDatabase.child("games").child(finalID).child("players").child(selectedPlayer).removeValue();
    }

    private void changeTime() {
        // fill in code here
    }

    private void shareButton() {
        // share game code
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
