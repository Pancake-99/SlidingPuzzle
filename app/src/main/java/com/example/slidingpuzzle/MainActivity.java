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

    //esto es para elegir una foto de la galeria
    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    onImagePicked(uri);
                }
            }
    );

    //esto es para sacar una foto con la camara
    private final ActivityResultLauncher<Uri> mTakePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && tempImageUri != null) {
                    onImagePicked(tempImageUri);
                }
            }
    );

    //pedir permiso de camara
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Permiso de camara denegado", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //el boton de ayuda "?"
        findViewById(R.id.btn_help).setOnClickListener(v -> showAppTour());

        //el boton de records
        findViewById(R.id.btn_highscores).setOnClickListener(v -> showHighscores());

        //por defecto que empiece en 3x3
        if (savedInstanceState == null) {
            loadPuzzleFragment();
            
            //checar si es la primera vez que abre la app
            SharedPreferences prefs = getSharedPreferences("puzzle_prefs", MODE_PRIVATE);
            boolean firstTime = prefs.getBoolean("first_time", true);
            if (firstTime) {
                showAppTour();
                prefs.edit().putBoolean("first_time", false).apply();
            }
        }
    }

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

    public void showImagePickerDialog() {
        String[] options = {"Galeria", "Camara"};
        new AlertDialog.Builder(this)
                .setTitle("Elige una foto")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        mGetContent.launch("image/*");
                    } else {
                        checkCameraPermission();
                    }
                })
                .show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        tempImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        mTakePicture.launch(tempImageUri);
    }

    private void onImagePicked(Uri uri) {
        currentImageUri = uri.toString();
        loadPuzzleFragment();
        if (timerFragment != null) {
            timerFragment.resetTimer();
        }
    }

    private void showAppTour() {
        new AlertDialog.Builder(this)
                .setTitle("¡Bienvenido al Puzzle!")
                .setMessage("Aquí tienes los pasos básicos:\n\n" +
                        "1. Elige un tamaño (3x3, 4x4 o 5x5).\n" +
                        "2. Usa 'Pick Image' si quieres jugar con una foto tuya.\n" +
                        "3. Dale a 'Shuffle' para mezclar las piezas y activar el cronómetro.\n" +
                        "4. ¡Desliza las piezas para ordenarlas!\n" +
                        "5. Si te trabas, 'Solve' te ayuda (pero reinicia el tiempo).\n" +
                        "6. ¡Consulta tus mejores tiempos en el 'Hall of Fame'!")
                .setPositiveButton("¡Entendido!", null)
                .show();
    }

    //metodo para avisar antes de cambiar de tamaño
    public void changeSizeWithConfirmation(int newSize) {
        if (newSize == currentSize) return;

        new AlertDialog.Builder(this)
                .setTitle("¿Cambiar tamaño?")
                .setMessage("Se va a reiniciar el puzzle y el tiempo.")
                .setPositiveButton("Sí", (dialog, which) -> {
                    currentSize = newSize;
                    loadPuzzleFragment();
                    if (timerFragment != null) {
                        timerFragment.resetTimer();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    public TimerFragment getTimerFragment() {
        return timerFragment;
    }

    public void setTimerFragment(TimerFragment timerFragment) {
        this.timerFragment = timerFragment;
    }

    //metodo para decidir que fragmento cargar segun el tamaño
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

    //el que hace el cambio de fragmento de verdad
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
