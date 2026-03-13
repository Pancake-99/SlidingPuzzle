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
import java.util.Stack; //pila para pasos

public abstract class PuzzleFragment extends Fragment {

    protected static final String ARG_SIZE = "size";
    protected static final String ARG_IMAGE_URI = "image_uri";
    protected int size;
    protected String imageUriString;
    private GridLayout puzzleGrid;
    private GridLayout guideGrid;
    private List<TextView> vTv = new ArrayList<>();
    private int pivot; //hueco vacio
    private Bitmap[] tileBitmaps;
    private boolean isAnimating = false;
    private boolean timerStarted = false;
    private boolean canStartTimer = false; 
    
    //pila para backtracking
    private Stack<Integer> miPilaDePasos = new Stack<>();
    
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
        guideGrid = view.findViewById(R.id.guide_grid);
        
        //cambio de dificultad
        view.findViewById(R.id.btn3x3).setOnClickListener(v -> ((MainActivity)getActivity()).changeSizeWithConfirmation(3));
        view.findViewById(R.id.btn4x4).setOnClickListener(v -> ((MainActivity)getActivity()).changeSizeWithConfirmation(4));
        view.findViewById(R.id.btn5x5).setOnClickListener(v -> ((MainActivity)getActivity()).changeSizeWithConfirmation(5));

        //boton para foto
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
        
        //timer
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

    //corta la imagen en cuadros
    private void prepareImageTiles() {
        try {
            Uri uri = Uri.parse(imageUriString);
            InputStream is = getContext().getContentResolver().openInputStream(uri);
            Bitmap source = BitmapFactory.decodeStream(is);
            is.close();
            //corrige si la foto salio rotada
            source = rotateBitmapIfRequired(source, uri);
            int width = source.getWidth();
            int height = source.getHeight();
            int minSide = Math.min(width, height);
            //recorte cuadrado central
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
            Toast.makeText(getContext(), "error con la imagen", Toast.LENGTH_SHORT).show();
            tileBitmaps = null;
        }
    }

    //lee orientacion de la foto exif
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

    //giro de pixeles con matriz
    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    //random de una de las 4 puntas
    private int elegirEsquinaAlAzar() {
        int[] esquinas = {
            0,
            size - 1,
            size * (size - 1),
            size * size - 1
        };
        Random r = new Random();
        return esquinas[r.nextInt(esquinas.length)];
    }

    //dibuja el tablero
    private void setupGrid() {
        puzzleGrid.removeAllViews();
        puzzleGrid.setRowCount(size);
        puzzleGrid.setColumnCount(size);
        vTv.clear();
        miPilaDePasos.clear(); //limpia historial
        
        int totalPieces = size * size;
        pivot = elegirEsquinaAlAzar(); //hueco inicial
        
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
            tv.setTag(i); //id de la pieza
            
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
        setupGuideGrid();
    }

    //tablero chico de ayuda
    private void setupGuideGrid() {
        if (guideGrid == null) return;
        guideGrid.removeAllViews();
        guideGrid.setRowCount(size);
        guideGrid.setColumnCount(size);
        int totalPieces = size * size;
        int guideTileSize = dpToPx(72 / size);
        
        for (int i = 0; i < totalPieces; i++) {
            TextView tv = new TextView(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = guideTileSize;
            params.height = guideTileSize;
            params.setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));
            tv.setLayoutParams(params);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextSize(size > 4 ? 6 : 8);
            tv.setClipToOutline(false);
            tv.setTextColor(Color.parseColor("#071013"));
            tv.setTypeface(ResourcesCompat.getFont(getContext(), R.font.silkscreen_bold));
            
            if (i == pivot) {
                tv.setText("");
                tv.setBackgroundColor(Color.TRANSPARENT);
            } else {
                if (tileBitmaps != null && tileBitmaps[i] != null) {
                    tv.setText("");
                    tv.setBackground(new BitmapDrawable(getResources(), tileBitmaps[i]));
                } else {
                    tv.setText(String.valueOf(i + 1));
                    tv.setBackgroundColor(getTileColor(i));
                }
            }
            guideGrid.addView(tv);
        }
    }

    //colores arcade
    private int getTileColor(int i) {
        String[] colors = {"#D81E5B", "#00CECB", "#FF9F1C"};
        return Color.parseColor(colors[i % colors.length]);
    }

    //unidades dp a px
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    //click en pieza
    private void handleMove(int clickedIndex) {
        if (isAnimating) return;
        int pRow = pivot / size;
        int pCol = pivot % size;
        int cRow = clickedIndex / size;
        int cCol = clickedIndex % size;
        //vecino directo
        if ((pRow == cRow && Math.abs(pCol - cCol) == 1) || (pCol == cCol && Math.abs(pRow - cRow) == 1)) {
            if (canStartTimer && !timerStarted) {
                if (timerFragment != null) {
                    timerFragment.startTimer();
                    timerStarted = true;
                }
            }
            animateAndSwap(clickedIndex, 80, this::checkSolved);
        } else {
            Toast.makeText(getContext(), "bloqueado", Toast.LENGTH_SHORT).show();
        }
    }

    //movimiento con animacion
    private void animateAndSwap(int clickedIndex, int duration, Runnable onEnd) {
        isAnimating = true;
        TextView clickedView = vTv.get(clickedIndex);
        TextView pivotView = vTv.get(pivot);
        int oldPivot = pivot;
        clickedView.setZ(10); //sobre los demas
        pivotView.setVisibility(View.INVISIBLE);
        float dx = (float) pivotView.getLeft() - clickedView.getLeft();
        float dy = (float) pivotView.getTop() - clickedView.getTop();
        clickedView.animate()
                .translationX(dx)
                .translationY(dy)
                .setDuration(duration)
                .withEndAction(() -> {
                    swap(pivot, clickedIndex);
                    clickedView.setTranslationX(0);
                    clickedView.setTranslationY(0);
                    clickedView.setZ(0);
                    pivotView.setVisibility(View.VISIBLE);
                    clickedView.setVisibility(View.VISIBLE);
                    pivot = clickedIndex;
                    isAnimating = false;
                    
                    //guarda paso si no es solve
                    if (!isSolving) {
                        //limpieza de pila si vuelve atras
                        if (!miPilaDePasos.isEmpty() && clickedIndex == miPilaDePasos.peek()) {
                            miPilaDePasos.pop();
                        } else {
                            miPilaDePasos.push(oldPivot);
                        }
                    }
                    
                    if (onEnd != null) {
                        onEnd.run();
                    }
                })
                .start();
    }

    //revisa si ganaste
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
            Toast.makeText(getContext(), "Ganaste!", Toast.LENGTH_LONG).show();
            timerStarted = false;
            canStartTimer = false;
            miPilaDePasos.clear();
        }
    }

    //pide nombre para record
    private void showHighscorePrompt(String time) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("record");
        builder.setMessage("tiempo: " + time + "\nnombre:");
        final EditText input = new EditText(getContext());
        input.setTypeface(ResourcesCompat.getFont(getContext(), R.font.silkscreen_bold));
        builder.setView(input);
        builder.setPositiveButton("listo", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) name = "anonimo";
            HighscoreDbHelper dbHelper = new HighscoreDbHelper(getContext());
            dbHelper.addScore(name, time, size + "x" + size);
            Toast.makeText(getContext(), "guardado", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("chau", null);
        builder.show();
    }

    //confirmacion shuffle
    private void showShuffleConfirmation() {
        new AlertDialog.Builder(getContext())
                .setTitle("Mezclar?")
                .setMessage("Se reiniciara el tiempo")
                .setPositiveButton("Si", (dialog, which) -> {
                    shufflePuzzle();
                    resetTimer();
                })
                .setNegativeButton("No", null)
                .show();
    }

    //confirmacion solve
    private void showSolveConfirmation() {
        new AlertDialog.Builder(getContext())
                .setTitle("resolver")
                .setMessage("el tiempo se resetea")
                .setPositiveButton("si", (dialog, which) -> {
                    resetTimer();
                    solvePuzzle();
                })
                .setNegativeButton("no", null)
                .show();
    }

    //limpia timer
    private void resetTimer() {
        if (timerFragment != null) {
            timerFragment.resetTimer();
            timerStarted = false;
            canStartTimer = false;
        }
    }

    //mueve piezas al azar
    private void shufflePuzzle() {
        if (isAnimating) return;
        canStartTimer = false;
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
                    animateAndSwap(move, 25, this);
                } else {
                    canStartTimer = true;
                }
            }
        }.run();
    }

    //movimiento random legal
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

    //swapea vistas en la lista
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

    //elige algoritmo
    private void solvePuzzle() {
        if (isAnimating) return;
        if (size >= 4) {
            resolverPasoAPaso();
        } else {
            //a* para el chiquito
            solveWithAStar();
        }
    }

    //backtrack usando la pila
    private void resolverPasoAPaso() {
        if (miPilaDePasos.isEmpty()) {
            Toast.makeText(getContext(), "listo", Toast.LENGTH_SHORT).show();
            return;
        }
        isSolving = true;
        new Runnable() {
            @Override
            public void run() {
                if (!miPilaDePasos.isEmpty()) {
                    int destino = miPilaDePasos.pop();
                    animateAndSwap(destino, 25, this);
                } else {
                    isSolving = false;
                    Toast.makeText(getContext(), "listo", Toast.LENGTH_SHORT).show();
                }
            }
        }.run();
    }

    //busca solucion optima
    private void solveWithAStar() {
        if (size > 3) {
            Toast.makeText(getContext(), "eeeeeeeeeeeee", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getContext(), "sin salida", Toast.LENGTH_SHORT).show();
        }
    }

    //mueve piezas segun ruta de a*
    private void executeAStarSolution(List<Integer> path) {
        new Runnable() {
            int step = 0;
            @Override
            public void run() {
                if (step < path.size()) {
                    animateAndSwap(path.get(step), 25, this);
                    step++;
                } else {
                    isSolving = false;
                    miPilaDePasos.clear();
                }
            }
        }.run();
    }

    //core del a* busca menor f = g + h
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

    //distancia manhattan: suma de pasos a destino
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

    //nodo para grafo de estados
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
        //arma lista de movimientos
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
