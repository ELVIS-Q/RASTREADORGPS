package com.example.rastreadorgps;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException; // Para manejar si el email ya está en uso
import com.google.firebase.auth.FirebaseAuthWeakPasswordException; // Para contraseñas débiles
import com.google.firebase.auth.FirebaseUser;

public class RegistroActivity extends AppCompatActivity {

    private EditText emailField;
    private EditText passwordField;
    private EditText confirmPasswordField;
    private MaterialButton registerButton;
    private TextView loginTextView; // Para el texto "Ya tienes cuenta? Inicia sesión"
    private ProgressBar progressBar;

    private FirebaseAuth mAuth; // Instancia de Firebase Authentication

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro); // Asegúrate de que este sea el nombre de tu layout XML

        // Inicializa Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 1. Inicializar vistas desde el layout XML
        emailField = findViewById(R.id.registerEmailField);
        passwordField = findViewById(R.id.registerPasswordField);
        confirmPasswordField = findViewById(R.id.confirmPasswordField);
        registerButton = findViewById(R.id.registerButton);
        loginTextView = findViewById(R.id.loginTextView);
        progressBar = findViewById(R.id.registerProgressBar);

        // 2. Configurar listeners para los botones y texto
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        loginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navegar a LoginActivity
                startActivity(new Intent(RegistroActivity.this, LoginActivity.class));
                finish(); // Finaliza RegisterActivity para que no se acumule en el stack
            }
        });
    }

    /**
     * Intenta registrar un nuevo usuario con correo electrónico y contraseña.
     */
    private void registerUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();

        // Validaciones de los campos
        if (TextUtils.isEmpty(email)) {
            emailField.setError("El correo electrónico es requerido.");
            emailField.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordField.setError("La contraseña es requerida.");
            passwordField.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordField.setError("Confirma tu contraseña.");
            confirmPasswordField.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordField.setError("Las contraseñas no coinciden.");
            confirmPasswordField.requestFocus();
            return;
        }
        if (password.length() < 6) {
            passwordField.setError("La contraseña debe tener al menos 6 caracteres.");
            passwordField.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE); // Muestra el ProgressBar

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE); // Oculta el ProgressBar

                        if (task.isSuccessful()) {
                            // Registro exitoso
                            Toast.makeText(RegistroActivity.this, "Registro exitoso.", Toast.LENGTH_SHORT).show();
                            FirebaseUser user = mAuth.getCurrentUser();
                            navigateToMainActivity(user.getEmail()); // Redirige a MainActivity
                        } else {
                            // Si el registro falla, muestra un mensaje al usuario
                            String errorMessage = "Error en el registro.";
                            if (task.getException() instanceof FirebaseAuthWeakPasswordException) {
                                errorMessage = "La contraseña es demasiado débil.";
                            } else if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                errorMessage = "Ya existe una cuenta con este correo electrónico.";
                            } else {
                                errorMessage += " " + task.getException().getMessage();
                            }
                            Toast.makeText(RegistroActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            // Log.e("RegisterActivity", "Error de registro", task.getException()); // Para depuración
                        }
                    }
                });
    }

    /**
     * Navega a la MainActivity y finaliza la RegisterActivity.
     * @param userEmail El correo electrónico del usuario para pasarlo a MainActivity.
     */
    private void navigateToMainActivity(String userEmail) {
        Intent intent = new Intent(RegistroActivity.this, MainActivity.class);
        intent.putExtra("user", userEmail); // Pasa el email del usuario a MainActivity
        startActivity(intent);
        finish(); // Finaliza RegisterActivity para que el usuario no pueda volver con el botón "atrás"
    }
}
