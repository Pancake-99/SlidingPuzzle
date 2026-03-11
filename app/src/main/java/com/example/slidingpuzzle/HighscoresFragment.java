package com.example.slidingpuzzle;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import java.util.List;

public class HighscoresFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_highscores, container, false);

        LinearLayout list = view.findViewById(R.id.scores_list);
        Button btnBack = view.findViewById(R.id.btn_back);

        HighscoreDbHelper dbHelper = new HighscoreDbHelper(getContext());
        List<HighscoreDbHelper.Score> scores = dbHelper.getAllScores();

        Typeface regularFont = ResourcesCompat.getFont(getContext(), R.font.silkscreen_regular);
        Typeface boldFont = ResourcesCompat.getFont(getContext(), R.font.silkscreen_bold);

        if (scores.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("SIN RECORDS AUN");
            empty.setTextColor(Color.parseColor("#F7F7FF"));
            empty.setTextSize(20);
            empty.setTypeface(boldFont);
            empty.setGravity(Gravity.CENTER);
            list.addView(empty);
        } else {
            // Header row in Spanish
            list.addView(createScoreRow("#", "NOMBRE", "TIEMPO", "MODO", Color.parseColor("#F7F7FF"), boldFont));

            for (int i = 0; i < scores.size(); i++) {
                HighscoreDbHelper.Score s = scores.get(i);
                
                int position = i + 1;
                String posStr = getOrdinal(position);
                
                int color = Color.parseColor("#F7F7FF");
                if (position == 1) color = Color.parseColor("#FF9F1C");
                else if (position == 2) color = Color.parseColor("#00CECB");
                else if (position == 3) color = Color.parseColor("#D81E5B");

                String name = s.getName().length() > 8 ? s.getName().substring(0, 8) : s.getName();
                
                list.addView(createScoreRow(posStr, name.toUpperCase(), s.getTime(), s.getSize(), color, regularFont));
            }
        }

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        return view;
    }

    private LinearLayout createScoreRow(String pos, String name, String time, String size, int color, Typeface font) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);
        row.setWeightSum(10);

        row.addView(createColumn(pos, 2, color, font));
        row.addView(createColumn(name, 3, color, font));
        row.addView(createColumn(time, 3, color, font));
        row.addView(createColumn(size, 2, color, font));

        return row;
    }

    private TextView createColumn(String text, float weight, int color, Typeface font) {
        TextView tv = new TextView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        tv.setLayoutParams(params);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(14);
        tv.setTypeface(font);
        tv.setGravity(Gravity.START);
        return tv;
    }

    private String getOrdinal(int i) {
        String[] suffixes = new String[] { "o", "o", "o", "o", "o", "o", "o", "o", "o", "o" };
        return i + "º";
    }
}
