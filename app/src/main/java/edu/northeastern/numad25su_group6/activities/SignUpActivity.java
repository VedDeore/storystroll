package edu.northeastern.numad25su_group6.activities;

import static edu.northeastern.numad25su_group6.constants.Constants.PICK_IMAGE_REQUEST;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import edu.northeastern.numad25su_group6.R;
import edu.northeastern.numad25su_group6.pojo.User;
import java.util.Objects;

/**
 * SignUpActivity allows users to create a new account by entering their first name, last name,
 * email, password, and confirm password. It validates the input and saves the user data in Firebase
 * Realtime Database.
 */
public class SignUpActivity extends AppCompatActivity {

  // UI elements
  private EditText firstname;
  private EditText lastname;
  private EditText email;
  private EditText password;
  private EditText confirmPassword;
  private Button profileImageButton;
  private ImageView profileImagePreview;
  private Button signUpButton;

  private Uri selectedImageUri;

  // Firebase authentication and database references
  private FirebaseAuth userAuth;
  private DatabaseReference usersRef;
  private StorageReference storageRef;

  // Dialog for loading state
  private AlertDialog loadingDialog;

  // Constants
  private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
  private static final int PERMISSION_REQUEST_CODE = 100;
  private static final String STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_sign_up);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
      Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
      return insets;
    });

    // Initialize UI elements
    firstname = findViewById(R.id.first_name);
    lastname = findViewById(R.id.last_name);
    email = findViewById(R.id.email);
    password = findViewById(R.id.password);
    confirmPassword = findViewById(R.id.confirm_password);
    profileImageButton = findViewById(R.id.profile_image_button);
    profileImagePreview = findViewById(R.id.profile_image_preview);
    signUpButton = findViewById(R.id.signUpButton);

    // Initialize Firebase authentication and database references
    userAuth = FirebaseAuth.getInstance();
    usersRef = FirebaseDatabase.getInstance().getReference("users");
    storageRef = FirebaseStorage.getInstance().getReference("profile_images");

    // Set up the sign-up button click listener
    signUpButton.setOnClickListener(this::onSignUpClick);
    profileImageButton.setOnClickListener(v -> openImagePicker());
  }

  /**
   * This method checks if the app has permission to read external storage.
   *
   * @return true if permission is granted, false otherwise.
   */
  private boolean hasStoragePermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      return true;
    } else {
      return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
          == PackageManager.PERMISSION_GRANTED;
    }
  }

  /**
   * This method requests the user to grant permission to read external storage.
   */
  private void requestStoragePermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      String[] permissions = {STORAGE_PERMISSION};
      requestPermissions(permissions, PERMISSION_REQUEST_CODE);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == PERMISSION_REQUEST_CODE) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        openImagePicker();
      } else {
        showPermissionDeniedDialog();
      }
    }
  }

  /**
   * This method shows a dialog to inform the user that storage permission is required to upload
   * profile images.
   */
  private void showPermissionDeniedDialog() {
    new AlertDialog.Builder(this)
        .setTitle("Permission Required")
        .setMessage(
            "Storage permission is required to upload profile images. Please grant permission in Settings to continue uploading the profile image.")
        .setPositiveButton("Go to Settings", (dialog, which) -> {
          Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
          Uri uri = Uri.fromParts("package", getPackageName(), null);
          intent.setData(uri);
          startActivity(intent);
        })
        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
        .setCancelable(false)
        .show();
  }

  /**
   * This method opens an image picker to allow the user to select a profile image.
   */
  private void openImagePicker() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && !hasStoragePermission()) {
      requestStoragePermission();
      return;
    }

    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    intent.setType("image/*");
    startActivityForResult(Intent.createChooser(intent, "Select Profile Image"),
        PICK_IMAGE_REQUEST);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null
        && data.getData() != null) {
      Uri imageUri = data.getData();
      if (getFileSize(imageUri) > MAX_IMAGE_SIZE) {
        Toast.makeText(this, "Image must be less than 5MB", Toast.LENGTH_SHORT).show();
        selectedImageUri = null;
        findViewById(R.id.imageCard).setVisibility(View.GONE);
      } else {
        selectedImageUri = imageUri;
        profileImagePreview.setBackground(null);
        profileImagePreview.setImageURI(imageUri);
        findViewById(R.id.imageCard).setVisibility(View.VISIBLE);
        Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
      }
    }
  }

  /**
   * This method retrieves the file size of the selected image URI.
   *
   * @param uri The URI of the selected image.
   * @return The size of the file in bytes, or 0 if the size could not be determined.
   */
  private long getFileSize(Uri uri) {
    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
    int sizeIndex = cursor != null ? cursor.getColumnIndex(OpenableColumns.SIZE) : -1;
    long size = 0;
    if (cursor != null && cursor.moveToFirst() && sizeIndex != -1) {
      size = cursor.getLong(sizeIndex);
    }
    if (cursor != null) {
      cursor.close();
    }
    return size;
  }

  /**
   * This method is called when the user clicks the "Sign In" button. It navigates to the
   * SignInActivity.
   *
   * @param view The view that was clicked.
   */
  public void onSignInClick(View view) {
    Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
    startActivity(intent);
  }

  /**
   * This method validates if the email is in correct format.
   *
   * @param email The email string to validate.
   * @return true if email format is valid, false otherwise.
   */
  private boolean isValidEmail(String email) {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
  }

  /**
   * This method is called when the user clicks the "Sign Up" button. It validates the input fields,
   * creates a new user in Firebase Authentication, and saves the user profile in Firebase Realtime
   * Database.
   *
   * @param view The view that was clicked.
   */
  public void onSignUpClick(View view) {
    String first = firstname.getText().toString().trim();
    String last = lastname.getText().toString().trim();
    String emailText = email.getText().toString().trim();
    String pass = password.getText().toString().trim();
    String confirmPass = confirmPassword.getText().toString().trim();

    // Validate input fields
    if (first.isEmpty() || last.isEmpty() || emailText.isEmpty() || pass.isEmpty()
        || confirmPass.isEmpty()) {
      Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
      return;
    }
    if (!isValidEmail(emailText)) {
      Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
      return;
    }
    if (!pass.equals(confirmPass)) {
      Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
      return;
    }

    // Show loading dialog
    showLoadingDialog();

    // Create a new user with email and password
    userAuth.createUserWithEmailAndPassword(emailText, pass)
        .addOnCompleteListener(this, task -> {
          if (task.isSuccessful()) {
            FirebaseUser user = userAuth.getCurrentUser();
            if (user != null) {
              String uid = user.getUid();
              if (selectedImageUri != null) {
                uploadProfileImageAndSaveUser(uid, first, last, emailText);
              } else {
                saveUserToDatabase(uid, first, last, emailText, null);
              }
            }
          } else {
            hideLoadingDialog();
            if (task.getException() != null &&
                task.getException().getClass().getSimpleName()
                    .equals("FirebaseAuthUserCollisionException")) {
              Toast.makeText(this, "Email already exists", Toast.LENGTH_SHORT).show();
            } else {
              Toast.makeText(this,
                  "Signup failed: " + Objects.requireNonNull(task.getException()).getMessage(),
                  Toast.LENGTH_SHORT).show();
            }
          }
        });
  }

  /**
   * This method uploads the selected profile image to Firebase Storage and saves the user data in
   * Firebase Realtime Database.
   *
   * @param uid       The unique identifier of the user.
   * @param first     The first name of the user.
   * @param last      The last name of the user.
   * @param emailText The email address of the user.
   */
  private void uploadProfileImageAndSaveUser(String uid, String first, String last,
      String emailText) {
    StorageReference imgRef = storageRef.child(uid + ".jpg");
    imgRef.putFile(selectedImageUri)
        .addOnSuccessListener(taskSnapshot -> imgRef.getDownloadUrl().addOnSuccessListener(uri -> {
          saveUserToDatabase(uid, first, last, emailText, uri.toString());
        }))
        .addOnFailureListener(e -> {
          hideLoadingDialog();
          Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show();
          saveUserToDatabase(uid, first, last, emailText, null);
        });
  }

  /**
   * This method saves the user profile information in Firebase Realtime Database.
   *
   * @param uid       The unique identifier of the user.
   * @param first     The first name of the user.
   * @param last      The last name of the user.
   * @param emailText The email address of the user.
   * @param imageUrl  The URL of the user's profile image, or null if no image was selected.
   */
  private void saveUserToDatabase(String uid, String first, String last, String emailText,
      String imageUrl) {
    User userProfile = new User(first, last, emailText, imageUrl);
    usersRef.child(uid).setValue(userProfile)
        .addOnCompleteListener(dbTask -> {
          hideLoadingDialog();
          if (dbTask.isSuccessful()) {
            addUserStats(uid);
            startActivity(new Intent(this, TreasureHuntActivity.class));
            finish();
          } else {
            Toast.makeText(this, "Failed to save user info", Toast.LENGTH_SHORT).show();
          }
        });
  }

  /**
   * This method adds user statistics to the Firebase Realtime Database.
   *
   * @param uid The unique identifier of the user.
   */
  private void addUserStats(String uid) {
    DatabaseReference statsRef = FirebaseDatabase.getInstance().getReference("userstats");
    String statsId = statsRef.push().getKey();
    if (statsId != null) {
      statsRef.child(statsId).child("points").setValue(0);
      statsRef.child(statsId).child("userid").setValue(uid);
    }
  }

  /**
   * This method shows a loading dialog while the user is being created.
   */
  private void showLoadingDialog() {
    if (loadingDialog == null) {
      android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
      View view = getLayoutInflater().inflate(R.layout.item_loading, null);
      builder.setView(view);
      builder.setCancelable(false);
      loadingDialog = builder.create();
    }
    loadingDialog.show();
  }

  /**
   * This method hides the loading dialog if it is currently showing.
   */
  private void hideLoadingDialog() {
    if (loadingDialog != null && loadingDialog.isShowing()) {
      loadingDialog.dismiss();
    }
  }
}