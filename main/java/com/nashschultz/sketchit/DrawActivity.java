package com.nashschultz.sketchit;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class DrawActivity extends AppCompatActivity {

    private PaintView paintView;

    String gameID;
    String userID;
    String[] idList;
    DatabaseReference mDatabase;
    Boolean isEven = false;
    String currentWordUserID;
    String path;
    int round;
    Boolean isHost = false;
    String name;
    String[] nameList;
    Bundle bundle;
    String timeLeft;

    int currentStroke = 20;

    int timerCount;
    int tempTimerCount;
    TextView wordLabel;
    TextView timerLabel;
    TextView flashWord;
    FirebaseStorage storage = FirebaseStorage.getInstance();

    Button strokeButton;
    Button tempColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);
        paintView = (PaintView) findViewById(R.id.paintView);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        paintView.init(metrics);


        mDatabase = FirebaseDatabase.getInstance().getReference();

        bundle = getIntent().getExtras();
        assert bundle != null;
        gameID = bundle.getString("gameID");
        userID = bundle.getString("userID");
        idList = bundle.getStringArray("idList");
        isEven = bundle.getBoolean("isEven");
        round = bundle.getInt("round");
        isHost = bundle.getBoolean("isHost");
        name = bundle.getString("name");
        nameList = bundle.getStringArray("nameList");
        timerCount = bundle.getInt("time");

        wordLabel = (TextView) findViewById(R.id.wordLabel);
        timerLabel = (TextView) findViewById(R.id.timeLeftLabel);
        strokeButton = (Button) findViewById(R.id.strokeButton);
        flashWord = (TextView) findViewById(R.id.flashWord);

        tempColor = (Button) findViewById(R.id.blackColor);

        loadWord();
        if (isHost) {
            mDatabase.child("games").child(gameID).child("draw").removeValue();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.normal:
                paintView.normal();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // do nothing
    }

    private void animateWord() {

        final float startSize = 12; // Size in pixels
        final float endSize = 48;
        long animationDuration = 800; // Animation duration in ms

        ValueAnimator animator = ValueAnimator.ofFloat(startSize, endSize);
        animator.setDuration(animationDuration);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float animatedValue = (float) valueAnimator.getAnimatedValue();
                flashWord.setTextSize(animatedValue);
            }

        });
        animator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                // done
                flashWord.setVisibility(View.INVISIBLE);
            }
        });
        animator.start();
    }

    private void loadWord() {
        path = "round" + (round - 1);
        currentWordUserID = idList[round - 1];
        final ValueEventListener postListener = new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                String word = (String) dataSnapshot.getValue();
                System.out.println(word);
                String newWordLabel = "You are drawing: " + word;
                wordLabel.setText(newWordLabel);
                flashWord.setText(word);
                drawingTime();
                animateWord();
                // update word on screen here
                // ...
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Getting Post failed, log a message
                // ...
                System.out.println("failed");
            }
        };
        mDatabase.child("games").child(gameID).child(path).child(currentWordUserID).addListenerForSingleValueEvent(postListener);
    }

    private void drawingTime() {
        new CountDownTimer(timerCount * 1000, 1000) {

            public void onTick(long millisUntilFinished) {
                timeLeft = "Time left: " + millisUntilFinished / 1000;
                timerLabel.setText(timeLeft);
            }

            public void onFinish() {
                uploadImage();
            }
        }.start();
    }

    private void uploadImage() {
        Bitmap bitmap = paintView.getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        byte[] byteArray = stream.toByteArray();

        StorageReference storageRef = storage.getReference();

        String pathString = currentWordUserID + ".jpg";
        UploadTask uploadTask = storageRef.child("games").child(gameID).child("round" + round).child(pathString).putBytes(byteArray);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
                mDatabase.child("games").child(gameID).child("draw").child(userID).setValue(1);
                bundle.putInt("round", round + 1);

                Intent intent = new Intent(DrawActivity.this, GuessActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }

    public void changeColor(View view) {
        switch(view.getId())
        {
            case R.id.blackColor:
                paintView.setColor(1);
                tempColor.setAlpha(.4f);
                tempColor = (Button) findViewById(R.id.blackColor);
                tempColor.setAlpha(1f);
                break;
            case R.id.redColor:
                paintView.setColor(2);
                tempColor.setAlpha(.4f);
                tempColor = (Button) findViewById(R.id.redColor);
                tempColor.setAlpha(1f);
                break;
            case R.id.yellowColor:
                paintView.setColor(3);
                tempColor.setAlpha(.4f);
                tempColor = (Button) findViewById(R.id.yellowColor);
                tempColor.setAlpha(1f);
                break;
            case R.id.greenColor:
                paintView.setColor(4);
                tempColor.setAlpha(.4f);
                tempColor = (Button) findViewById(R.id.greenColor);
                tempColor.setAlpha(1f);
                break;
            case R.id.blueColor:
                paintView.setColor(5);
                tempColor.setAlpha(.4f);
                tempColor = (Button) findViewById(R.id.blueColor);
                tempColor.setAlpha(1f);
                break;
            case R.id.pinkColor:
                paintView.setColor(6);
                tempColor.setAlpha(.4f);
                tempColor = (Button) findViewById(R.id.pinkColor);
                tempColor.setAlpha(1f);
                break;
            case R.id.purpleColor:
                paintView.setColor(7);
                tempColor.setAlpha(.4f);
                tempColor = (Button) findViewById(R.id.purpleColor);
                tempColor.setAlpha(1f);
                break;
            case R.id.brownColor:
                paintView.setColor(8);
                tempColor.setAlpha(.4f);
                tempColor = (Button) findViewById(R.id.brownColor);
                tempColor.setAlpha(1f);
                break;
            case R.id.eraserButton:
                paintView.setColor(9);
                tempColor.setAlpha(.4f);
                tempColor = (Button) findViewById(R.id.eraserButton);
                tempColor.setAlpha(1f);
                break;
            default:
                throw new RuntimeException("Unknown button ID");
        }
    }

    public void undoDrawing(View view) {

    }

    public void setCurrentStroke(View view) {
        if (currentStroke == 20) {
            currentStroke += 5;
            strokeButton.setBackground(getResources().getDrawable(R.drawable.dot2));
            paintView.setSize(currentStroke + 10);
        } else if (currentStroke == 25) {
            currentStroke += 5;
            strokeButton.setBackground(getResources().getDrawable(R.drawable.dot3));
            paintView.setSize(currentStroke + 10);
        } else if (currentStroke == 30) {
            currentStroke += 5;
            strokeButton.setBackground(getResources().getDrawable(R.drawable.dot4));
            paintView.setSize(currentStroke + 10);
        } else {
            currentStroke = 20;
            strokeButton.setBackground(getResources().getDrawable(R.drawable.dot1));
            paintView.setSize(currentStroke + 10);
        }
    }

    public void deleteDrawing(View view) {
        paintView.clear();
    }
}
