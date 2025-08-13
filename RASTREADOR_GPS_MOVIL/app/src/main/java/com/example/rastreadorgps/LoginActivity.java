package com.example.rastreadorgps;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText emailField;
    private EditText passwordField;
    private MaterialButton loginButton;
    private MaterialButton registerButton;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirige a RegisterActivity en lugar de registrar directamente aquí
                startActivity(new Intent(LoginActivity.this, RegistroActivity.class));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMainActivity(currentUser.getEmail());
        }
    }

    /**
     * Intenta registrar un nuevo usuario con correo electrónico y contraseña.
     * ESTE MÉTODO DEBE ESTAR EN RegisterActivity.java, NO AQUÍ.
     * Si lo tienes aquí, es una duplicación o un error de lógica.
     */
    // private void registerUser() { ... } // Elimina este método si lo tienes aquí

    /**
     * Intenta iniciar sesión con un usuario existente usando correo electrónico y contraseña.
     */
    private void loginUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        // **REFORZAR VALIDACIONES AQUÍ**
        if (TextUtils.isEmpty(email)) {
            emailField.setError("El correo electrónico es requerido.");
            emailField.requestFocus(); // Enfoca el campo para que el usuario lo vea
            return; // Detiene la ejecución si el campo está vacío
        }
        if (TextUtils.isEmpty(password)) {
            passwordField.setError("La contraseña es requerida.");
            passwordField.requestFocus(); // Enfoca el campo
            return; // Detiene la ejecución si el campo está vacío
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show();
                            FirebaseUser user = mAuth.getCurrentUser();
                            navigateToMainActivity(user.getEmail());
                        } else {
                            String errorMessage = "Error en el inicio de sesión: " + task.getException().getMessage();
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void navigateToMainActivity(String userEmail) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("user", userEmail);
        startActivity(intent);
        finish();
    }
}
