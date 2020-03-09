package com.example.countryquiz;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

public class GridAdapter extends BaseAdapter {

    private int sets = 0;
    private String category;
    private InterstitialAd interstitialAd;

    public GridAdapter(int sets, String category, InterstitialAd interstitialAd) {
        this.sets = sets;
        this.category = category;
        this.interstitialAd = interstitialAd;
    }

    @Override
    public int getCount() {
        return sets;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {

        View view1;

        if (view == null) {
            view1 = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.set_item, viewGroup, false);
        } else {
            view1 = view;
        }

        view1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {

                interstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        interstitialAd.loadAd(new AdRequest.Builder().build());
                        Intent questionIntent = new Intent(view.getContext(), QuestionsActivity.class);
                        questionIntent.putExtra("category", category);
                        questionIntent.putExtra("setNo", i + 1);
                        view.getContext().startActivity(questionIntent);
                    }
                });

                if (interstitialAd.isLoaded()) {
                    interstitialAd.show();
                    return;
                }

                Intent questionIntent = new Intent(view.getContext(), QuestionsActivity.class);
                questionIntent.putExtra("category", category);
                questionIntent.putExtra("setNo", i + 1);
                view.getContext().startActivity(questionIntent);
            }
        });

        ((TextView) view1.findViewById(R.id.textView)).setText(String.valueOf(i + 1));

        return view1;
    }
}
