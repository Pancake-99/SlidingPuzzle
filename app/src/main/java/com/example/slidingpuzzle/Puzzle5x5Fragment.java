package com.example.slidingpuzzle;

import android.os.Bundle;

public class Puzzle5x5Fragment extends PuzzleFragment {
    public static Puzzle5x5Fragment newInstance(String imageUri) {
        Puzzle5x5Fragment fragment = new Puzzle5x5Fragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SIZE, 5);
        args.putString(ARG_IMAGE_URI, imageUri);
        fragment.setArguments(args);
        return fragment;
    }
}
