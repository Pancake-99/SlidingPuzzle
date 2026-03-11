package com.example.slidingpuzzle;

import android.os.Bundle;

public class Puzzle4x4Fragment extends PuzzleFragment {
    public static Puzzle4x4Fragment newInstance(String imageUri) {
        Puzzle4x4Fragment fragment = new Puzzle4x4Fragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SIZE, 4);
        args.putString(ARG_IMAGE_URI, imageUri);
        fragment.setArguments(args);
        return fragment;
    }
}
