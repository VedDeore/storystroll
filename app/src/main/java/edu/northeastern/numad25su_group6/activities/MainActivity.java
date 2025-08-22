package edu.northeastern.numad25su_group6.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import edu.northeastern.numad25su_group6.R;

/**
 * MainActivity is the entry point of the application. It checks if a user is signed in and
 * redirects them to the appropriate activity.
 */
public class MainActivity extends AppCompatActivity {

  // Flag to ensure the activity is started only once
  private boolean hasStarted = false;

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
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!isInternetAvailable()) {
      showNoInternetDialog();
      return;
    }

    // Ensure the activity is started only once
    if (!hasStarted) {
      hasStarted = true;
      FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
      if (user != null) {
        startActivity(new Intent(this, TreasureHuntActivity.class));
      } else {
        startActivity(new Intent(this, SignInActivity.class));
      }
      finish();
    }
  }

  /**
   * Checks if the device has an active internet connection.
   *
   * @return true if internet is available, false otherwise
   */
  private boolean isInternetAvailable() {
    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    if (cm != null) {
      NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
      return activeNetwork != null && activeNetwork.isConnected();
    }
    return false;
  }

  /**
   * Displays a dialog informing the user that internet access is required. Provides options to go
   * to settings or exit the app.
   */
  private void showNoInternetDialog() {
    new AlertDialog.Builder(this)
        .setTitle("No Internet Connection")
        .setMessage("Internet is required to use this app. Please enable it or exit.")
        .setPositiveButton("Settings", (dialog, which) -> {
          startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
        })
        .setNegativeButton("Exit", (dialog, which) -> finish())
        .setCancelable(false)
        .show();
  }
}