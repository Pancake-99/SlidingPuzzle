package com.example.slidingpuzzle;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PuzzleFragment extends Fragment {

    private static final String ARG_SIZE = "size";
    private static final String ARG_IMAGE_URI = "image_uri";
    private int size;
    private String imageUriString;
    private GridLayout puzzleGrid;
    private List<TextView> vTv = new ArrayList<>();
    private int pivot;
    private Bitmap[] tileBitmaps;

    public static PuzzleFragment newInstance(int size) {
        return newInstance(size, null);
    }

    public static PuzzleFragment newInstance(int size, String imageUri) {
        PuzzleFragment fragment = new PuzzleFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SIZE, size);
        args.putString(ARG_IMAGE_URI, imageUri);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            size = getArguments().getInt(ARG_SIZE);
            imageUriString = getArguments().getString(ARG_IMAGE_URI);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_puzzle, container, false);
        puzzleGrid = view.findViewById(R.id.puzzle_grid);
        
        if (imageUriString != null) {
            prepareImageTiles();
        }
        
        setupGrid();
        return view;
    }

    private void prepareImageTiles() {
        try {
            Uri uri = Uri.parse(imageUriString);
            InputStream is = getContext().getContentResolver().openInputStream(uri);
            Bitmap source = BitmapFactory.decodeStream(is);
            is.close();

            //cuadros
            int width = source.getWidth();
            int height = source.getHeight();
            int minSide = Math.min(width, height);
            Bitmap cropped = Bitmap.createBitmap(source, (width - minSide) / 2, (height - minSide) / 2, minSide, minSide);

            //a piezas
            tileBitmaps = new Bitmap[size * size];
            int pieceSide = minSide / size;
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    tileBitmaps[i * size + j] = Bitmap.createBitmap(cropped, j * pieceSide, i * pieceSide, pieceSide, pieceSide);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
            tileBitmaps = null;
        }
    }

    private void setupGrid() {
        puzzleGrid.removeAllViews();
        puzzleGrid.setRowCount(size);
        puzzleGrid.setColumnCount(size);
        vTv.clear();

        int totalPieces = size * size;
        pivot = totalPieces - 1;

        for (int i = 0; i < totalPieces; i++) {
            TextView tv = new TextView(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            int tileSize = dpToPx(300 / size);
            params.width = tileSize;
            params.height = tileSize;
            params.setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));
            tv.setLayoutParams(params);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextSize(size > 4 ? 16 : 24);
            tv.setTextColor(Color.WHITE);

            if (i == pivot) {
                tv.setText("x");
                tv.setBackgroundColor(Color.parseColor("#121718"));
            } else {
                if (tileBitmaps != null && tileBitmaps[i] != null) {
                    tv.setText("");
                    tv.setBackground(new BitmapDrawable(getResources(), tileBitmaps[i]));
                } else {
                    tv.setText(String.valueOf(i + 1));
                    tv.setBackgroundColor(getTileColor(i));
                }
            }

            final int index = i;
            tv.setOnClickListener(v -> handleMove(index));

            vTv.add(tv);
            puzzleGrid.addView(tv);
        }
    }

    private int getTileColor(int i) {
        String[] colors = {"#AD61AA", "#7D61AD", "#617AAD", "#61A5AD", "#61AD6F"};
        return Color.parseColor(colors[i % colors.length]);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private void handleMove(int clickedIndex) {
        int pRow = pivot / size;
        int pCol = pivot % size;
        int cRow = clickedIndex / size;
        int cCol = clickedIndex % size;

        if ((pRow == cRow && Math.abs(pCol - cCol) == 1) || (pCol == cCol && Math.abs(pRow - cRow) == 1)) {
            swap(pivot, clickedIndex);
            pivot = clickedIndex;
        } else {
            Toast.makeText(getContext(), "not movable", Toast.LENGTH_SHORT).show();
        }
    }

    private void swap(int a, int b) {
        TextView tvA = vTv.get(a);
        TextView tvB = vTv.get(b);

        CharSequence textA = tvA.getText();
        Drawable bgA = tvA.getBackground();

        tvA.setText(tvB.getText());
        tvA.setBackground(tvB.getBackground());

        tvB.setText(textA);
        tvB.setBackground(bgA);
    }
}