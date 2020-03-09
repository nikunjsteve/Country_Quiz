package com.example.countryquiz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.animation.Animator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class QuestionsActivity extends AppCompatActivity {

    public static final String FILE_NAME = "COUNTRYQUIZ";
    public static final String KEY_NAME = "QUESTIONS";

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();

    private List<QuestionModel> list;

    private TextView question, noIndicator;
    private FloatingActionButton btnBookmark;
    private LinearLayout optionsContainer;
    private Button btnShare, btnNext;

    private int count = 0;
    private int postion = 0;
    private int score = 0;

    private String category;
    private int setNo;
    private Dialog loadingDialog;

    private List<QuestionModel> bookmarksList;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private Gson gson;
    private int matchedQuestionPosition;

    private InterstitialAd interstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        loadAds();

        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        question = findViewById(R.id.question);
        noIndicator = findViewById(R.id.numberIndicator);
        btnBookmark = findViewById(R.id.bookmarkButton);
        optionsContainer = findViewById(R.id.optionsContainer);
        btnShare = findViewById(R.id.btnShare);
        btnNext = findViewById(R.id.btnNext);

        preferences = getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
        gson = new Gson();

        getBookmarks();

        btnBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(modelMatch()){
                    bookmarksList.remove(matchedQuestionPosition);
                    btnBookmark.setImageDrawable(getDrawable(R.drawable.ic_bookmark_border));
                }else {
                    bookmarksList.add(list.get(postion));
                    btnBookmark.setImageDrawable(getDrawable(R.drawable.ic_bookmark));
                }
            }
        });

        category = getIntent().getStringExtra("category");
        setNo = getIntent().getIntExtra("setNo", 1);

        loadingDialog = new Dialog(this);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_corners));
        loadingDialog.setContentView(R.layout.loading);
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);

        list = new ArrayList<>();

        loadingDialog.show();
        myRef.child("SETS").child(category).child("questions").orderByChild("setNo").equalTo(setNo).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    list.add(dataSnapshot1.getValue(QuestionModel.class));
                }
                if (list.size() > 0) {

                    for (int i = 0; i < 4; i++) {
                        optionsContainer.getChildAt(i).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                checkAnswer((Button) view);
                            }
                        });
                    }
                    playAnim(question, 0, list.get(postion).getQuestion());
                    btnNext.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            btnNext.setEnabled(false);
                            btnNext.setAlpha(0.7f);
                            enableOption(true);
                            postion++;
                            if (postion == list.size()) {
                                if(interstitialAd.isLoaded()){
                                    interstitialAd.show();
                                    return;
                                }
                                Intent scoreIntent = new Intent(QuestionsActivity.this,ScoreActivity.class);
                                scoreIntent.putExtra("score",score);
                                scoreIntent.putExtra("total",list.size());
                                startActivity(scoreIntent);
                                finish();
                                return;
                            }
                            count = 0;
                            playAnim(question, 0, list.get(postion).getQuestion());
                        }
                    });

                    btnShare.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String body = list.get(postion).getQuestion() + "\n\n" +
                                    list.get(postion).getOptionA() + "\n" +
                                    list.get(postion).getOptionB() + "\n" +
                                    list.get(postion).getOptionC() + "\n" +
                                    list.get(postion).getOptionD();
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("text/plain");
                            shareIntent.putExtra(Intent.EXTRA_SUBJECT,"Country Quiz Challenge");
                            shareIntent.putExtra(Intent.EXTRA_TEXT,body);
                            startActivity(Intent.createChooser(shareIntent,"share via"));
                        }
                    });
                }else{
                    finish();
                    Toast.makeText(QuestionsActivity.this,"No questions",Toast.LENGTH_SHORT).show();
                }
                loadingDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(QuestionsActivity.this,databaseError.getMessage(),Toast.LENGTH_SHORT).show();
                    loadingDialog.dismiss();
                    finish();
            }
        });


    }

    @Override
    protected void onPause() {
        super.onPause();
        storeBookmarks();
    }

    private void playAnim(final View view, final int value, final String data) {

        view.animate().alpha(value).scaleX(value).scaleY(value).setDuration(500).setStartDelay(100)
                .setInterpolator(new DecelerateInterpolator()).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                if (value == 0 && count < 4) {
                    String option = "";
                    if (count == 0) {
                        option = list.get(postion).getOptionA();
                    } else if (count == 1) {
                        option = list.get(postion).getOptionB();
                    } else if (count == 2) {
                        option = list.get(postion).getOptionC();
                    } else if (count == 3) {
                        option = list.get(postion).getOptionD();
                    }
                    playAnim(optionsContainer.getChildAt(count), 0, option);
                    count++;
                }
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (value == 0) {
                    try {
                        ((TextView) view).setText(data);
                        noIndicator.setText(postion + 1 + "/" + list.size());
                        if(modelMatch()){
                            btnBookmark.setImageDrawable(getDrawable(R.drawable.ic_bookmark));
                        }else {
                            btnBookmark.setImageDrawable(getDrawable(R.drawable.ic_bookmark_border));
                        }
                    } catch (ClassCastException e) {
                        e.printStackTrace();
                        ((Button) view).setText(data);
                    }
                    view.setTag(data);
                    playAnim(view, 1, data);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

    }

    private void checkAnswer(Button selectedOption) {
        enableOption(false);
        btnNext.setEnabled(true);
        btnNext.setAlpha(1);
        if (selectedOption.getText().toString().equals(list.get(postion).getCorrectAns())) {
            score++;
            selectedOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            selectedOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF0000")));
            Button correctOption = (Button) optionsContainer.findViewWithTag(list.get(postion).getCorrectAns());
            correctOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        }
    }

    private void enableOption(boolean enable) {
        for (int i = 0; i < 4; i++) {
            optionsContainer.getChildAt(i).setEnabled(enable);
            if (enable) {
                optionsContainer.getChildAt(i).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#989898")));
            }
        }
    }

    private void getBookmarks(){

        String json = preferences.getString(KEY_NAME,"");

        Type type = new TypeToken<List<QuestionModel>>(){}.getType();

        bookmarksList = gson.fromJson(json,type);

        if(bookmarksList == null){
            bookmarksList = new ArrayList<>();
        }
    }

    private  boolean modelMatch(){
        boolean matched = false;
        int i=0;
        for (QuestionModel model : bookmarksList){
            if(model.getQuestion().equals(list.get(postion).getQuestion())
            && model.getCorrectAns().equals(list.get(postion).getCorrectAns())
            && model.getSetNo() == list.get(postion).getSetNo()){
                matched = true;
                matchedQuestionPosition = i;
            }
            i++;
        }
        return matched;
    }

    private void storeBookmarks(){

        String json  = gson.toJson(bookmarksList);
        editor.putString(KEY_NAME,json);
        editor.commit();

    }

    private void loadAds(){
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        interstitialAd = new InterstitialAd(this);
        interstitialAd.setAdUnitId(getString(R.string.interstitialAd_id));
        interstitialAd.loadAd(new AdRequest.Builder().build());
        interstitialAd.setAdListener(new AdListener(){
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                interstitialAd.loadAd(new AdRequest.Builder().build());
                Intent scoreIntent = new Intent(QuestionsActivity.this,ScoreActivity.class);
                scoreIntent.putExtra("score",score);
                scoreIntent.putExtra("total",list.size());
                startActivity(scoreIntent);
                finish();
                return;
            }
        });

    }
}
