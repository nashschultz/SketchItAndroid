package com.nashschultz.sketchit;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.util.Arrays;

public class GameSpecificActivity extends AppCompatActivity {

    String[] playerList;
    String[] idList;
    String gameID;
    int roundCount;
    Bundle bundle;
    ArrayAdapter adapter;
    ListView listView;
    AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_specific);

        bundle = getIntent().getExtras();
        playerList = bundle.getStringArray("nameList");
        roundCount = bundle.getInt("round");
        gameID = bundle.getString("gameID");
        idList = bundle.getStringArray("idList");

        MobileAds.initialize(this, new OnInitializationCompleteListener() {

            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mAdView = findViewById(R.id.gameSpecificAdView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        listView = (ListView) findViewById(R.id.otherPlayerListView);
        updateList();
    }

    private void updateList() {
        String[] newList = new String[playerList.length];
        for(int i = 0; i < playerList.length; i++) {
            newList[i] = "View " + playerList[i] + "'s word chain!";
        }
        adapter = new ArrayAdapter<String>(this,R.layout.activity_playerlistview,newList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //TextView textView = (TextView) view.findViewById(R.id.label);
                //String text = textView.getText().toString();
                //int indexOfSelf = Arrays.asList(playerList).indexOf(text);
                String selectedName = playerList[position];
                String selectedId = idList[position];
                Bundle newBundle = new Bundle();
                newBundle.putString("name", selectedName);
                newBundle.putString("id", selectedId);
                newBundle.putInt("round", roundCount);
                newBundle.putString("gameID", gameID);

                Intent intent = new Intent(GameSpecificActivity.this, SpecificDrawingActivity.class);
                intent.putExtras(newBundle);
                startActivity(intent);
            }});
    }
}
