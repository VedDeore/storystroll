package edu.northeastern.numad25su_group6.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import edu.northeastern.numad25su_group6.R;

/**
 * SignInActivity handles user sign-in functionality. It allows users to enter their email and
 * password to authenticate. If successful, it navigates to the TreasureHuntActivity.
 */
public class SignInActivity extends AppCompatActivity {

  // UI elements
  private EditText emailField;
  private EditText passwordField;
  private Button signInButton;

  // Firebase authentication instance
  private FirebaseAuth userAuth;
  private AlertDialog loadingDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_sign_in);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
      Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
      return insets;
    });

    // Initialize UI elements
    emailField = findViewById(R.id.usernameField);
    passwordField = findViewById(R.id.passwordField);
    signInButton = findViewById(R.id.submitButton);
    userAuth = FirebaseAuth.getInstance();

    // Set up the sign-in button click listener
    signInButton.setOnClickListener(v -> {
      String email = emailField.getText().toString().trim();
      String password = passwordField.getText().toString().trim();

      // Validate email and password input
      if (email.isEmpty() || password.isEmpty()) {
        Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
        return;
      }

      // Show loading dialog while signing in
      showLoadingDialog();

      // Sign in with Firebase authentication
      userAuth.signInWithEmailAndPassword(email, password)
          .addOnCompleteListener(this, task -> {
            hideLoadingDialog();
            if (task.isSuccessful()) {
              startActivity(new Intent(this, TreasureHuntActivity.class));
              finish();
            } else {
              Toast.makeText(this, "Invalid email or password. Please try again.",
                  Toast.LENGTH_SHORT).show();
            }
          });
    });
  }

  /**
   * Handles the sign-up button click event. Navigates to the SignUpActivity.
   *
   * @param view The view that was clicked.
   */
  public void onSignUpClick(View view) {
    Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
    startActivity(intent);
  }

  /**
   * Displays a loading dialog while the sign-in process is ongoing.
   */
  private void showLoadingDialog() {
    if (loadingDialog == null) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      View view = getLayoutInflater().inflate(R.layout.item_loading, null);
      builder.setView(view);
      builder.setCancelable(false);
      loadingDialog = builder.create();
    }
    loadingDialog.show();
  }

  /**
   * Hides the loading dialog if it is currently showing.
   */
  private void hideLoadingDialog() {
    if (loadingDialog != null && loadingDialog.isShowing()) {
      loadingDialog.dismiss();
    }
  }
}