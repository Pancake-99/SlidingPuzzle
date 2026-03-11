package com.example.slidingpuzzle;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

public abstract class PuzzleFragment extends Fragment {

    protected static final String ARG_SIZE = "size";
    protected static final String ARG_IMAGE_URI = "image_uri";
    protected int size;
    protected String imageUriString;
    private GridLayout puzzleGrid;
    private List<TextView> vTv = new ArrayList<>();
    private int pivot;
    private Bitmap[] tileBitmaps;
    private boolean isAnimating = false;
    private boolean timerStarted = false;
    private boolean canStartTimer = false; 
    
    private List<Integer> moveHistory = new ArrayList<>();
    private boolean isSolving = false;
    private TimerFragment timerFragment;

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
        
        //botones de tamaño
        view.findViewById(R.id.btn3x3).setOnClickListener(v -> ((MainActivity)getActivity()).changeSizeWithConfirmation(3));
        view.findViewById(R.id.btn4x4).setOnClickListener(v -> ((MainActivity)getActivity()).changeSizeWithConfirmation(4));
        view.findViewById(R.id.btn5x5).setOnClickListener(v -> ((MainActivity)getActivity()).changeSizeWithConfirmation(5));

        //pick image
        Button btnPick = view.findViewById(R.id.btnPickImage);
        btnPick.setOnClickListener(v -> ((MainActivity)getActivity()).showImagePickerDialog());

        Button btnShuffle = view.findViewById(R.id.btn_shuffle);
        btnShuffle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9F1C")));
        btnShuffle.setTextColor(Color.parseColor("#071013"));
        btnShuffle.setOnClickListener(v -> showShuffleConfirmation());

        Button btnSolve = view.findViewById(R.id.btn_solve);
        btnSolve.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#00CECB")));
        btnSolve.setTextColor(Color.parseColor("#071013"));
        btnSolve.setOnClickListener(v -> showSolveConfirmation());
        
        //cada fragmento de puzzle crea su propio cronometro
        timerFragment = new TimerFragment();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.timer_holder, timerFragment)
                .commit();

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
            source = rotateBitmapIfRequired(source, uri);
            int width = source.getWidth();
            int height = source.getHeight();
            int minSide = Math.min(width, height);
            Bitmap cropped = Bitmap.createBitmap(source, (width - minSide) / 2, (height - minSide) / 2, minSide, minSide);
            tileBitmaps = new Bitmap[size * size];
            int pieceSide = minSide / size;
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    tileBitmaps[i * size + j] = Bitmap.createBitmap(cropped, j * pieceSide, i * pieceSide, pieceSide, pieceSide);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al cargar Imagen", Toast.LENGTH_SHORT).show();
            tileBitmaps = null;
        }
    }

    private Bitmap rotateBitmapIfRequired(Bitmap img, Uri selectedImage) throws Exception {
        InputStream input = getContext().getContentResolver().openInputStream(selectedImage);
        ExifInterface ei;
        if (android.os.Build.VERSION.SDK_INT > 23) {
            ei = new ExifInterface(input);
        } else {
            ei = new ExifInterface(selectedImage.getPath());
        }
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90: return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180: return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270: return rotateImage(img, 270);
            default: return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    private void setupGrid() {
        puzzleGrid.removeAllViews();
        puzzleGrid.setRowCount(size);
        puzzleGrid.setColumnCount(size);
        vTv.clear();
        moveHistory.clear();
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
            tv.setClipToOutline(true);
            tv.setTextColor(Color.parseColor("#071013"));
            tv.setTypeface(ResourcesCompat.getFont(getContext(), R.font.silkscreen_bold));
            tv.setTag(i);
            if (i == pivot) {
                tv.setText(""); 
                tv.setBackgroundColor(Color.TRANSPARENT); 
            } else {
                if (tileBitmaps != null && tileBitmaps[i] != null) {
                    tv.setText("");
                    tv.setBackgroundResource(R.drawable.tile_rounded);
                    tv.setForeground(new BitmapDrawable(getResources(), tileBitmaps[i]));
                } else {
                    tv.setText(String.valueOf(i + 1));
                    GradientDrawable shape = new GradientDrawable();
                    shape.setCornerRadius(dpToPx(8));
                    shape.setColor(getTileColor(i));
                    tv.setBackground(shape);
                }
            }
            final int index = i;
            tv.setOnClickListener(v -> handleMove(index));
            vTv.add(tv);
            puzzleGrid.addView(tv);
        }
    }

    private int getTileColor(int i) {
        String[] colors = {"#D81E5B", "#00CECB", "#FF9F1C"};
        return Color.parseColor(colors[i % colors.length]);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private void handleMove(int clickedIndex) {
        if (isAnimating) return;
        int pRow = pivot / size;
        int pCol = pivot % size;
        int cRow = clickedIndex / size;
        int cCol = clickedIndex % size;
        if ((pRow == cRow && Math.abs(pCol - cCol) == 1) || (pCol == cCol && Math.abs(pRow - cRow) == 1)) {
            if (canStartTimer && !timerStarted) {
                if (timerFragment != null) {
                    timerFragment.startTimer();
                    timerStarted = true;
                }
            }
            animateAndSwap(clickedIndex, this::checkSolved);
        } else {
            Toast.makeText(getContext(), "no se puede mover", Toast.LENGTH_SHORT).show();
        }
    }

    private void animateAndSwap(int clickedIndex, Runnable onEnd) {
        isAnimating = true;
        TextView clickedView = vTv.get(clickedIndex);
        TextView pivotView = vTv.get(pivot);
        int oldPivot = pivot;
        clickedView.setZ(10);
        pivotView.setVisibility(View.INVISIBLE);
        float dx = (float) pivotView.getLeft() - clickedView.getLeft();
        float dy = (float) pivotView.getTop() - clickedView.getTop();
        clickedView.animate()
                .translationX(dx)
                .translationY(dy)
                .setDuration(80)
                .withEndAction(() -> {
                    swap(pivot, clickedIndex);
                    clickedView.setTranslationX(0);
                    clickedView.setTranslationY(0);
                    clickedView.setZ(0);
                    pivotView.setVisibility(View.VISIBLE);
                    clickedView.setVisibility(View.VISIBLE);
                    pivot = clickedIndex;
                    isAnimating = false;
                    if (!isSolving) {
                        if (!moveHistory.isEmpty() && clickedIndex == moveHistory.get(moveHistory.size() - 1)) {
                            moveHistory.remove(moveHistory.size() - 1);
                        } else {
                            moveHistory.add(oldPivot);
                        }
                    }
                    if (onEnd != null) {
                        onEnd.run();
                    }
                })
                .start();
    }

    private void checkSolved() {
        boolean isSolved = true;
        for (int i = 0; i < vTv.size(); i++) {
            if (!vTv.get(i).getTag().equals(i)) {
                isSolved = false;
                break;
            }
        }
        if (isSolved && timerStarted) {
            if (timerFragment != null) {
                timerFragment.stopTimer();
                String finalTime = timerFragment.getFormattedTime();
                showHighscorePrompt(finalTime);
            }
            Toast.makeText(getContext(), "¡Ganaste!", Toast.LENGTH_LONG).show();
            timerStarted = false;
            canStartTimer = false;
            moveHistory.clear();
        }
    }

    private void showHighscorePrompt(String time) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("¡Nuevo Record!");
        builder.setMessage("Tu tiempo: " + time + "\nEscribe tu nombre:");
        final EditText input = new EditText(getContext());
        input.setTypeface(ResourcesCompat.getFont(getContext(), R.font.silkscreen_bold));
        builder.setView(input);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) name = "Jugador Anonimo";
            HighscoreDbHelper dbHelper = new HighscoreDbHelper(getContext());
            dbHelper.addScore(name, time, size + "x" + size);
            Toast.makeText(getContext(), "Puntaje guardado", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void showShuffleConfirmation() {
        new AlertDialog.Builder(getContext())
                .setTitle("Mezclar piezas")
                .setMessage("¿Seguro que quieres mezclar? Se va a reiniciar el tiempo.")
                .setPositiveButton("Sí", (dialog, which) -> {
                    shufflePuzzle();
                    resetTimer();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showSolveConfirmation() {
        new AlertDialog.Builder(getContext())
                .setTitle("¿Resolver automáticamente?")
                .setMessage("El tiempo se va a reiniciar porque no cuenta como victoria.")
                .setPositiveButton("Dale", (dialog, which) -> {
                    resetTimer();
                    solvePuzzle();
                })
                .setNegativeButton("No, yo puedo", null)
                .show();
    }

    private void resetTimer() {
        if (timerFragment != null) {
            timerFragment.resetTimer();
            timerStarted = false;
            canStartTimer = false;
        }
    }

    private void shufflePuzzle() {
        if (isAnimating) return;
        canStartTimer = false;
        moveHistory.clear();
        final int totalMoves;
        if (size == 3) totalMoves = 30;
        else if (size == 4) totalMoves = 65;
        else totalMoves = 120;
        final Random random = new Random();
        new Runnable() {
            int count = 0;
            int lastPivot = -1;
            @Override
            public void run() {
                if (count < totalMoves) {
                    count++;
                    int currentPivotBeforeMove = pivot;
                    int move = getRandomValidMove(random, lastPivot);
                    lastPivot = currentPivotBeforeMove;
                    animateAndSwap(move, this);
                } else {
                    canStartTimer = true;
                }
            }
        }.run();
    }

    private int getRandomValidMove(Random random, int excludeIndex) {
        int pRow = pivot / size;
        int pCol = pivot % size;
        List<Integer> validMoves = new ArrayList<>();
        if (pRow > 0) validMoves.add(pivot - size);
        if (pRow < size - 1) validMoves.add(pivot + size);
        if (pCol > 0) validMoves.add(pivot - 1);
        if (pCol < size - 1) validMoves.add(pivot + 1);
        for (int i = 0; i < validMoves.size(); i++) {
            if (validMoves.get(i) == excludeIndex) {
                validMoves.remove(i);
                break;
            }
        }
        return validMoves.get(random.nextInt(validMoves.size()));
    }

    private void swap(int a, int b) {
        TextView tvA = vTv.get(a);
        TextView tvB = vTv.get(b);
        CharSequence textA = tvA.getText();
        Drawable bgA = tvA.getBackground();
        Drawable fgA = tvA.getForeground();
        Object tagA = tvA.getTag();
        int colorA = tvA.getCurrentTextColor();
        tvA.setText(tvB.getText());
        tvA.setBackground(tvB.getBackground());
        tvA.setForeground(tvB.getForeground());
        tvA.setTag(tvB.getTag());
        tvA.setTextColor(tvB.getCurrentTextColor());
        tvB.setText(textA);
        tvB.setBackground(bgA);
        tvB.setForeground(fgA);
        tvB.setTag(tagA);
        tvB.setTextColor(colorA);
    }

    private void solvePuzzle() {
        if (isAnimating) return;
        if (size == 5) backtrackSolution();
        else solveWithAStar();
    }

    private void backtrackSolution() {
        if (moveHistory.isEmpty()) {
            Toast.makeText(getContext(), "Ya esta listo o no se como volver", Toast.LENGTH_SHORT).show();
            return;
        }
        isSolving = true;
        new Runnable() {
            @Override
            public void run() {
                if (!moveHistory.isEmpty()) {
                    int targetIndex = moveHistory.remove(moveHistory.size() - 1);
                    animateAndSwap(targetIndex, this);
                } else {
                    isSolving = false;
                    Toast.makeText(getContext(), "¡Solucionado!", Toast.LENGTH_SHORT).show();
                }
            }
        }.run();
    }

    private void solveWithAStar() {
        if (size > 3) {
            Toast.makeText(getContext(), "Esto va a tardar un ratito...", Toast.LENGTH_SHORT).show();
        }
        List<Integer> currentState = new ArrayList<>();
        for (TextView tv : vTv) {
            currentState.add((Integer) tv.getTag());
        }
        List<Integer> solutionPath = findPath(currentState);
        if (solutionPath != null && !solutionPath.isEmpty()) {
            isSolving = true;
            executeAStarSolution(solutionPath);
        } else {
            Toast.makeText(getContext(), "No encontre solucion", Toast.LENGTH_SHORT).show();
        }
    }

    private void executeAStarSolution(List<Integer> path) {
        new Runnable() {
            int step = 0;
            @Override
            public void run() {
                if (step < path.size()) {
                    animateAndSwap(path.get(step), this);
                    step++;
                } else {
                    isSolving = false;
                    moveHistory.clear();
                }
            }
        }.run();
    }

    private List<Integer> findPath(List<Integer> start) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
        Set<List<Integer>> closedSet = new HashSet<>();
        openSet.add(new Node(start, 0, getHeuristic(start), -1, null));
        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            if (current.h == 0) return current.getPath();
            closedSet.add(current.state);
            int blankPos = current.state.indexOf(size * size - 1);
            int row = blankPos / size;
            int col = blankPos % size;
            int[] dr = {-1, 1, 0, 0};
            int[] dc = {0, 0, -1, 1};
            for (int i = 0; i < 4; i++) {
                int nr = row + dr[i];
                int nc = col + dc[i];
                if (nr >= 0 && nr < size && nc >= 0 && nc < size) {
                    int nextPos = nr * size + nc;
                    List<Integer> nextState = new ArrayList<>(current.state);
                    Collections.swap(nextState, blankPos, nextPos);
                    if (!closedSet.contains(nextState)) {
                        openSet.add(new Node(nextState, current.g + 1, getHeuristic(nextState), nextPos, current));
                    }
                }
            }
        }
        return null;
    }

    private int getHeuristic(List<Integer> state) {
        int dist = 0;
        for (int i = 0; i < state.size(); i++) {
            int val = state.get(i);
            if (val != size * size - 1) {
                int targetRow = val / size;
                int targetCol = val % size;
                int currentRow = i / size;
                int currentCol = i % size;
                dist += Math.abs(targetRow - currentRow) + Math.abs(targetCol - currentCol);
            }
        }
        return dist;
    }

    private static class Node {
        List<Integer> state;
        int g, h, f;
        int movedIndex;
        Node parent;
        Node(List<Integer> state, int g, int h, int movedIndex, Node parent) {
            this.state = state;
            this.g = g;
            this.h = h;
            this.f = g + h;
            this.movedIndex = movedIndex;
            this.parent = parent;
        }
        List<Integer> getPath() {
            List<Integer> path = new ArrayList<>();
            Node curr = this;
            while (curr.parent != null) {
                path.add(curr.movedIndex);
                curr = curr.parent;
            }
            Collections.reverse(path);
            return path;
        }
    }
}
