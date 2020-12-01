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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class GuessActivity extends AppCompatActivity {

    EditText guessWord;
    TextView mainLabel;
    ImageView guessImage;
    Button submitButton;
    TextView waitingForDrawings;

    int round;
    String[] idList;
    String gameID;
    String userID;
    DatabaseReference mDatabase;
    int count;
    Boolean isEven;
    Boolean isHost = false;
    String name;
    String[] nameList;
    String[] waitingForList;
    int evenAddition = 1;
    int timerCount;
    Boolean hasSubmitted = false;
    Bundle bundle;
    String timeLeft;

    ValueEventListener mListener;
    ValueEventListener wordListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guess);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        bundle = getIntent().getExtras();
        round = bundle.getInt("round");
        idList = bundle.getStringArray("idList");
        gameID = bundle.getString("gameID");
        userID = bundle.getString("userID");
        //count = bundle.getInt("count");
        isEven = bundle.getBoolean("isEven");
        isHost = bundle.getBoolean("isHost");
        name = bundle.getString("name");
        nameList = bundle.getStringArray("nameList");
        timerCount = bundle.getInt("time");

        guessWord = (EditText) findViewById(R.id.guessField);
        guessImage = (ImageView) findViewById(R.id.guessImageView);
        submitButton = (Button) findViewById(R.id.submitGuessButton);
        mainLabel = (TextView) findViewById(R.id.mainLabelGuess);
        waitingForDrawings = (TextView) findViewById(R.id.waitingForDrawingLabel);

        if (isEven) {
            evenAddition = 0;
        }
        if (isHost) {
            mDatabase.child("games").child(gameID).child("round" + round).removeValue();
        }
        checkForAllDrawings();
    }

    private void checkForAllDrawings() {
        final CountDownTimer drawTimer = new CountDownTimer( 45000, 1000) {

            public void onTick(long millisUntilFinished) {
                timeLeft = "Waiting for drawings... " + millisUntilFinished / 1000;
                waitingForDrawings.setText(timeLeft);
            }

            public void onFinish() {
                // remove draw observer
                waitingForDrawings.setVisibility(View.INVISIBLE);
                mainLabel.setVisibility(View.VISIBLE);
                loadImage();
            }
        }.start();

        mDatabase.child("games").child(gameID).child("draw").addValueEventListener(mListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() == (idList.length + evenAddition)) {
                    System.out.println("everyone submitted");
                    // remove observer here
                    drawTimer.cancel();
                    mDatabase.child("games").child(gameID).child("draw").removeEventListener(mListener);
                    waitingForDrawings.setVisibility(View.INVISIBLE);
                    mainLabel.setVisibility(View.VISIBLE);
                    loadImage();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Getting Post failed, log a message
                // ...
            }
        });
    }

    private void loadImage() {
        String path = idList[round - 1] + ".jpg";
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("games").child(gameID).child("round" + (round - 1)).child(path);
        final long ONE_MEGABYTE = 1024 * 1024;
        storageReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Data for "images/island.jpg" is returns, use this as needed
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                guessImage.setImageBitmap(bitmap);
                new CountDownTimer( 45000, 1000) {

                    public void onTick(long millisUntilFinished) {
                        timeLeft = "Time left: " + millisUntilFinished / 1000 + "s";
                        mainLabel.setText(timeLeft);
                    }

                    public void onFinish() {
                        // remove draw observer
                        if (!hasSubmitted) {
                            String didNotGuess = name + " did not guess!";
                            guessWord.setText(didNotGuess);
                            submitGuess(null);
                        }

                    }
                }.start();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });

    }

    @Override
    public void onBackPressed() {
        // do nothing
    }

    public void submitGuess(View view) {
        if (guessWord.getText().toString().compareTo("") != 0) {
            hasSubmitted = true;
            mDatabase.child("games").child(gameID).child("round" + round).child(idList[round - 1]).setValue(guessWord.getText().toString());
            mDatabase.child("games").child(gameID).child("round" + round).child("names").child(name).setValue(1);
            submitButton.setVisibility(View.INVISIBLE);
            guessWord.setVisibility(View.INVISIBLE);
            mainLabel.setVisibility(View.VISIBLE);
        } else {
            System.out.println("guess empty");
        }

        mDatabase.child("games").child(gameID).child("round" + round).addValueEventListener(wordListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() == (idList.length + evenAddition + 1)) {
                    // everyone submitted
                    // remove observers
                    if (round == idList.length) {
                        // go to GameOver
                        mDatabase.child("games").child(gameID).child("round" + round).removeEventListener(wordListener);
                        Intent intent = new Intent(GuessActivity.this, GameSummaryActivity.class);
                        intent.putExtras(bundle);
                        startActivity(intent);
                    } else {
                        // go back to Draw
                        bundle.putInt("round", round + 1);
                        mDatabase.child("games").child(gameID).child("round" + round).removeEventListener(wordListener);
                        Intent intent = new Intent(GuessActivity.this, DrawActivity.class);
                        intent.putExtras(bundle);
                        startActivity(intent);
                    }
                } else {
                    // check for waiting names here
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Getting Post failed, log a message
                // ...
            }
        });
    }


}
