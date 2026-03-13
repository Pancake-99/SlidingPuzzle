package com.example.slidingpuzzle;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    private int currentSize = 3;
    private String currentImageUri = null;
    private TimerFragment timerFragment;
    private Uri tempImageUri;

    //elegir foto galeria
    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    onImagePicked(uri);
                }
            }
    );

    //foto con camara
    private final ActivityResultLauncher<Uri> mTakePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && tempImageUri != null) {
                    onImagePicked(tempImageUri);
                }
            }
    );

    //permiso camara
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "permiso denegado", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //pantalla completa sin bordes
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        //ajuste de padding para no tapar con botones del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //boton ayuda
        findViewById(R.id.btn_help).setOnClickListener(v -> showAppTour());

        //boton records
        findViewById(R.id.btn_highscores).setOnClickListener(v -> showHighscores());

        //arranque inicial
        if (savedInstanceState == null) {
            loadPuzzleFragment();
            
            //tutorial si es primera vez
            SharedPreferences prefs = getSharedPreferences("puzzle_prefs", MODE_PRIVATE);
            boolean firstTime = prefs.getBoolean("first_time", true);
            if (firstTime) {
                showAppTour();
                prefs.edit().putBoolean("first_time", false).apply();
            }
        }
    }

    //cambio a pantalla de records
    private void showHighscores() {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, new HighscoresFragment())
                .addToBackStack(null)
                .commit();
    }

    //menu galeria o camara
    public void showImagePickerDialog() {
        String[] options = {"galeria", "camara"};
        new AlertDialog.Builder(this)
                .setTitle("Elige una opción de imagen")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        mGetContent.launch("image/*");
                    } else {
                        checkCameraPermission();
                    }
                })
                .show();
    }

    //chequeo permiso camara
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    //abre camara y genera uri temporal
    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "new picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "from camera");
        tempImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        mTakePicture.launch(tempImageUri);
    }

    //actualiza imagen elegida
    private void onImagePicked(Uri uri) {
        currentImageUri = uri.toString();
        loadPuzzleFragment();
        if (timerFragment != null) {
            timerFragment.resetTimer();
        }
    }

    //tutorial del juego
    private void showAppTour() {
        new AlertDialog.Builder(this)
                .setTitle("Bienvenido")
                .setMessage("Pasos basicos:\n\n" +
                        "1. Elige un tamaño de puzzle.\n" +
                        "2. Usa 'imagen' para poner la foto que quieras.\n" +
                        "3. Toca a 'mezclar'.\n" +
                        "4. Resuelve el puzzle lo mas rápido que puedas.\n" +
                        "5. 'resolver' completara el puzzle en caso de que te rindas.\n" +
                        "6. Mira tus records.")
                .setPositiveButton("Ok", null)
                .show();
    }

    //aviso cambio tamaño
    public void changeSizeWithConfirmation(int newSize) {
        if (newSize == currentSize) return;

        new AlertDialog.Builder(this)
                .setTitle("Cambiar de tamaño?")
                .setMessage("se reiniciara el tiempo")
                .setPositiveButton("Si", (dialog, which) -> {
                    currentSize = newSize;
                    loadPuzzleFragment();
                    if (timerFragment != null) {
                        timerFragment.resetTimer();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    //getter timer
    public TimerFragment getTimerFragment() {
        return timerFragment;
    }

    //setter timer
    public void setTimerFragment(TimerFragment timerFragment) {
        this.timerFragment = timerFragment;
    }

    //elige que fragmento de puzzle cargar
    private void loadPuzzleFragment() {
        Fragment fragment;
        switch (currentSize) {
            case 4:
                fragment = Puzzle4x4Fragment.newInstance(currentImageUri);
                break;
            case 5:
                fragment = Puzzle5x5Fragment.newInstance(currentImageUri);
                break;
            case 3:
            default:
                fragment = Puzzle3x3Fragment.newInstance(currentImageUri);
                break;
        }
        loadFragment(fragment);
    }

    //reemplazo de fragmento
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
