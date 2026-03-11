package com.example.slidingpuzzle;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    private int currentSize = 3;
    private String currentImageUri = null;

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    currentImageUri = uri.toString();
                    loadFragment(PuzzleFragment.newInstance(currentSize, currentImageUri));
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

        findViewById(R.id.btn3x3).setOnClickListener(v -> {
            currentSize = 3;
            loadFragment(PuzzleFragment.newInstance(currentSize, currentImageUri));
        });
        findViewById(R.id.btn4x4).setOnClickListener(v -> {
            currentSize = 4;
            loadFragment(PuzzleFragment.newInstance(currentSize, currentImageUri));
        });
        findViewById(R.id.btn5x5).setOnClickListener(v -> {
            currentSize = 5;
            loadFragment(PuzzleFragment.newInstance(currentSize, currentImageUri));
        });

        findViewById(R.id.btnPickImage).setOnClickListener(v -> {
            mGetContent.launch("image/*");
        });

        // Default 3x3
        if (savedInstanceState == null) {
            loadFragment(PuzzleFragment.newInstance(3));
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}