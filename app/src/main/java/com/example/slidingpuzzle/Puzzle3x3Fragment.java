package com.example.slidingpuzzle;

import android.os.Bundle;

public class Puzzle3x3Fragment extends PuzzleFragment {
    public static Puzzle3x3Fragment newInstance(String imageUri) {
        Puzzle3x3Fragment fragment = new Puzzle3x3Fragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SIZE, 3);
        args.putString(ARG_IMAGE_URI, imageUri);
        fragment.setArguments(args);
        return fragment;
    }
}
