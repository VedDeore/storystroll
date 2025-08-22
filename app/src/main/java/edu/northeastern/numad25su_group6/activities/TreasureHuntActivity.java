package edu.northeastern.numad25su_group6.activities;

import static edu.northeastern.numad25su_group6.constants.Constants.PICK_IMAGE_REQUEST;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.view.animation.Interpolator;

import edu.northeastern.numad25su_group6.R;
import edu.northeastern.numad25su_group6.constants.Constants;

import edu.northeastern.numad25su_group6.constants.DiscoveryDialogType;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * TreasureHuntActivity allows users to find, add, and manage treasures on a map. It integrates
 * Google Maps for location tracking and Firebase for data storage.
 */
public class TreasureHuntActivity extends BaseActivity implements OnMapReadyCallback {

  // Required permissions for location and camera access
  private static final String[] REQUIRED_PERMISSIONS =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
          ? new String[]{
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.CAMERA,
          Manifest.permission.READ_MEDIA_IMAGES // For Android 13+
      }
          : new String[]{
              Manifest.permission.ACCESS_FINE_LOCATION,
              Manifest.permission.CAMERA,
              Manifest.permission.READ_EXTERNAL_STORAGE // For Android 12 and below
          };
  private static final float DEFAULT_ZOOM = 15f;
  private static final float DISCOVERY_RADIUS_METERS = 20f;
  private GoogleMap mMap;
  private FloatingActionButton btnAddTreasure, btnMyTreasures;

  // Firebase references for treasures, discoveries, and saved treasures
  private DatabaseReference treasuresRef, discoveriesRef, savedTreasuresRef;
  private FirebaseUser user;
  private FusedLocationProviderClient fusedLocationClient;
  private StorageReference storageReference;

  // Flags to track map readiness and location permissions
  private boolean isMapReady = false;
  private boolean hasLocationPermission = false;

  // Uri for selected image from gallery or camera
  private Uri selectedImageUri;
  private ImageView imgPreview;
  private AlertDialog loadingDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_treasure_hunt);
    setupToolbarWithProfile();
    setupDrawerWithHamburger();
    setupNavigationView();

    initializeComponents();
    setupMapFragment();
    setupClickListeners();
    requestAllNecessaryPermissions();

    // Exit confirmation on back press
    getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        new AlertDialog.Builder(TreasureHuntActivity.this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes", (dialog, which) -> finishAffinity())
            .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
            .show();
      }
    });
  }

  /**
   * Initializes Firebase references, location client, and UI components.
   */
  private void initializeComponents() {
    treasuresRef = FirebaseDatabase.getInstance().getReference("treasures");
    discoveriesRef = FirebaseDatabase.getInstance().getReference("discoveries");
    savedTreasuresRef = FirebaseDatabase.getInstance().getReference("savedTreasures");
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    storageReference = FirebaseStorage.getInstance().getReference();

    btnAddTreasure = findViewById(R.id.btnAddTreasure);
    btnMyTreasures = findViewById(R.id.btnMyTreasures);

    user = FirebaseAuth.getInstance().getCurrentUser();
  }

  /**
   * Sets up the map fragment and initializes the map when ready.
   */
  private void setupMapFragment() {
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
    if (mapFragment != null) {
      mapFragment.getMapAsync(this);
    }
  }

  /**
   * Sets up click listeners for the buttons in the activity.
   */
  private void setupClickListeners() {
    btnAddTreasure.setOnClickListener(v -> {
      if (hasLocationPermission) {
        openAddTreasureDialog();
      } else {
        showPermissionRequiredMessage();
      }
    });

    // Navigate to MyTreasuresActivity when the button is clicked
    btnMyTreasures.setOnClickListener(v -> {
      startActivity(new Intent(this, MyTreasuresActivity.class));
    });
  }

  /**
   * Requests all necessary permissions for location and camera access.
   */
  private void requestAllNecessaryPermissions() {
    if (!hasAllPermissions()) {
      ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
          Constants.LOCATION_PERMISSION_REQUEST_CODE);
    } else {
      hasLocationPermission = true;
    }
  }

  /**
   * Checks if all required permissions are granted.
   *
   * @return true if all permissions are granted, false otherwise
   */
  private boolean hasAllPermissions() {
    for (String permission : REQUIRED_PERMISSIONS) {
      if (ContextCompat.checkSelfPermission(this, permission)
          != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if the app has location permissions.
   *
   * @return true if location permissions are granted, false otherwise
   */
  private boolean hasLocationPermission() {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED;
  }

  @Override
  public void onMapReady(@NonNull GoogleMap googleMap) {
    mMap = googleMap;
    isMapReady = true;

    if (hasLocationPermission()) {
      enableMyLocationOnMap();
    }

    loadTreasuresFromRealtimeDB();

    // Handle marker clicks
    mMap.setOnMarkerClickListener(marker -> {
      handleMarkerClick(marker);
      return true;
    });
  }

  /**
   * Enables the My Location layer on the map and moves the camera to the current location.
   */
  private void enableMyLocationOnMap() {
    if (!hasLocationPermission() || !isMapReady) {
      return;
    }

    try {
      mMap.setMyLocationEnabled(true);
      moveToCurrentLocation();
    } catch (SecurityException e) {
      showToast("Location permission error: " + e.getMessage());
    }
  }

  /**
   * Moves the camera to the user's current location with a default zoom level.
   */
  private void moveToCurrentLocation() {
    getCurrentLocation(location -> {
      if (location != null && isMapReady) {
        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
      }
    });
  }

  /**
   * Fetches the current location of the user and invokes the callback with the location.
   *
   * @param callback Callback to handle the received location
   */
  private void getCurrentLocation(LocationCallback callback) {
    if (!hasLocationPermission()) {
      showPermissionRequiredMessage();
      return;
    }

    try {
      fusedLocationClient.getLastLocation()
          .addOnSuccessListener(callback::onLocationReceived)
          .addOnFailureListener(e -> {
            showToast("Unable to fetch location: " + e.getMessage());
            callback.onLocationReceived(null);
          });
    } catch (SecurityException e) {
      showToast("Location access denied");
      callback.onLocationReceived(null);
    }
  }

  /**
   * Opens a dialog to add a new treasure with an option to choose an image from the gallery or
   * camera.
   */
  private void openAddTreasureDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_treasure, null);
    builder.setView(dialogView);

    EditText etTitle = dialogView.findViewById(R.id.etTreasureTitle);
    EditText etDesc = dialogView.findViewById(R.id.etTreasureDesc);
    imgPreview = dialogView.findViewById(R.id.imgPreview);

    // Set up image preview with a default image
    imgPreview.post(() -> {
      imgPreview.animate()
          .scaleX(1.03f).scaleY(1.03f)
          .setDuration(650)
          .withEndAction(new Runnable() {
            @Override
            public void run() {
              imgPreview.animate()
                  .scaleX(1f).scaleY(1f)
                  .setDuration(650)
                  .withEndAction(this)
                  .start();
            }
          })
          .start();
    });

    // Set up buttons for choosing image or taking a photo
    Button btnChooseImage = dialogView.findViewById(R.id.btnChooseImage);
    Button btnCameraImage = dialogView.findViewById(R.id.btnCameraImage);
    Button btnSave = dialogView.findViewById(R.id.btnSave);
    Button btnCancel = dialogView.findViewById(R.id.btnCancel);

    // Set up click listeners for image selection buttons
    btnChooseImage.setOnClickListener(v -> {
      Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
      intent.setType("image/*");
      startActivityForResult(Intent.createChooser(intent, "Select Profile Image"),
          PICK_IMAGE_REQUEST);
    });

    // Set up click listener for camera button
    btnCameraImage.setOnClickListener(v -> {
      Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      startActivityForResult(intent, Constants.CAMERA_REQUEST);
    });

    // Create and show the dialog
    AlertDialog dialog = builder.create();
    btnSave.setOnClickListener(v -> {
      handleSaveTreasureWithImage(etTitle, etDesc, selectedImageUri);
      dialog.dismiss();
    });

    // Set up cancel button to dismiss the dialog
    btnCancel.setOnClickListener(v -> dialog.dismiss());
    Objects.requireNonNull(dialog.getWindow())
        .setBackgroundDrawableResource(android.R.color.transparent);

    // Show the dialog
    dialog.show();
  }

  /**
   * Handles saving the treasure with an optional image.
   *
   * @param etTitle  EditText for treasure title
   * @param etDesc   EditText for treasure description
   * @param imageUri Uri of the selected image, can be null if no image is selected
   */
  private void handleSaveTreasureWithImage(EditText etTitle, EditText etDesc, Uri imageUri) {
    String title = etTitle.getText().toString().trim();
    String desc = etDesc.getText().toString().trim();

    // Validate title and description
    if (title.isEmpty() || desc.isEmpty()) {
      showToast("Please fill all fields");
      return;
    }

    // Show loading dialog
    showLoadingDialog();

    // Get the current location before saving the treasure
    getCurrentLocation(location -> {
      if (location != null) {
        if (imageUri != null) {
          uploadImageToFirebase(imageUri, url -> {
            saveTreasureToRealtimeDB(title, desc, location, url);
          });
        } else {
          saveTreasureToRealtimeDB(title, desc, location, null);
        }
      } else {
        hideLoadingDialog();
        showToast("Unable to get location");
      }
    });
  }

  /**
   * Uploads the selected image to Firebase Storage and invokes the callback with the image URL.
   *
   * @param imageUri Uri of the image to upload
   * @param callback Callback to handle the uploaded image URL
   */
  private void uploadImageToFirebase(Uri imageUri, OnImageUploadCallback callback) {
    String fileName = "treasure_images/" + System.currentTimeMillis() + ".jpg";
    StorageReference fileRef = storageReference.child(fileName);

    fileRef.putFile(imageUri)
        .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
            .addOnSuccessListener(uri -> callback.onImageUploaded(uri.toString())))
        .addOnFailureListener(e -> showToast("Image upload failed: " + e.getMessage()));
  }

  /**
   * Saves the treasure details to Firebase Realtime Database.
   *
   * @param title    Title of the treasure
   * @param desc     Description of the treasure
   * @param location Location of the treasure
   * @param imageUrl Optional image URL of the treasure
   */
  private void saveTreasureToRealtimeDB(String title, String desc, Location location,
      @Nullable String imageUrl) {
    String userId = user.getUid();
    String treasureId = treasuresRef.push().getKey();
    if (treasureId == null) {
      hideLoadingDialog();
      showToast("Error creating treasure");
      return;
    }

    // Create a map to hold the treasure details
    Map<String, Object> treasure = createTreasureMap(title, desc, location, userId);
    if (imageUrl != null) {
      treasure.put("imageUrl", imageUrl);
    }

    // Save the treasure to Firebase Realtime Database
    treasuresRef.child(treasureId).setValue(treasure)
        .addOnSuccessListener(aVoid -> {
          hideLoadingDialog();
          showToast("Treasure Added Successfully!");
        })
        .addOnFailureListener(e -> {
          hideLoadingDialog();
          showToast("Error adding treasure: " + e.getMessage());
        });
  }

  /**
   * Creates a map representation of the treasure details.
   *
   * @param title    Title of the treasure
   * @param desc     Description of the treasure
   * @param location Location of the treasure
   * @param userId   ID of the user who created the treasure
   * @return Map containing treasure details
   */
  private Map<String, Object> createTreasureMap(String title, String desc, Location location,
      String userId) {
    Map<String, Object> treasure = new HashMap<>();
    treasure.put("title", title);
    treasure.put("description", desc);
    treasure.put("latitude", location.getLatitude());
    treasure.put("longitude", location.getLongitude());
    treasure.put("location", formatLocationString(location));
    treasure.put("createdByUserId", userId);
    treasure.put("timestamp", System.currentTimeMillis());
    treasure.put("totalDiscoveries", 0);
    return treasure;
  }

  /**
   * Formats the location into a string representation.
   *
   * @param location Location object to format
   * @return Formatted string with latitude and longitude
   */
  private String formatLocationString(Location location) {
    return String.format(Locale.US, "Lat: %.6f, Lng: %.6f",
        location.getLatitude(), location.getLongitude());
  }

  /**
   * Loads treasures from Firebase Realtime Database and adds markers to the map.
   */
  private void loadTreasuresFromRealtimeDB() {
    treasuresRef.addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot snapshot) {
        if (!isMapReady) {
          return;
        }

        mMap.clear();
        for (DataSnapshot data : snapshot.getChildren()) {
          Double lat = data.child("latitude").getValue(Double.class);
          Double lng = data.child("longitude").getValue(Double.class);
          String title = data.child("title").getValue(String.class);
          String desc = data.child("description").getValue(String.class);
          String createdByUserId = data.child("createdByUserId").getValue(String.class);
          String treasureId = data.getKey();
          Integer totalDiscoveriesInt = data.child("totalDiscoveries").getValue(Integer.class);
          String totalDiscoveries =
              totalDiscoveriesInt != null ? String.valueOf(totalDiscoveriesInt) : "0";

          // Check if all required fields are present
          if (lat != null && lng != null && title != null && treasureId != null) {
            checkAndAddMarker(lat, lng, title, desc != null ? desc : "", createdByUserId,
                treasureId, totalDiscoveries);
          }
        }
      }

      @Override
      public void onCancelled(@NonNull DatabaseError error) {
        showToast("Failed to load treasures: " + error.getMessage());
      }
    });
  }

  /**
   * Scales the bitmap resource to a fixed size and returns a BitmapDescriptor.
   *
   * @param resId Resource ID of the drawable
   * @return Scaled BitmapDescriptor
   */
  private BitmapDescriptor getScaledBitmapDescriptor(int resId) {
    int width = 96;
    int height = 96;

    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
    return BitmapDescriptorFactory.fromBitmap(scaledBitmap);
  }

  /**
   * Checks if the treasure has already been discovered by the user and adds a marker to the map.
   *
   * @param lat              Latitude of the treasure
   * @param lng              Longitude of the treasure
   * @param title            Title of the treasure
   * @param desc             Description of the treasure
   * @param createdByUserId  ID of the user who created the treasure
   * @param treasureId       ID of the treasure
   * @param totalDiscoveries Total number of discoveries for the treasure
   */
  private void checkAndAddMarker(double lat, double lng, String title, String desc,
      String createdByUserId, String treasureId,
      String totalDiscoveries) {
    String currentUserId = user.getUid();
    discoveriesRef.orderByChild("userId").equalTo(currentUserId)
        .addListenerForSingleValueEvent(new ValueEventListener() {
          @Override
          public void onDataChange(@NonNull DataSnapshot snapshot) {
            boolean discovered = false;
            for (DataSnapshot child : snapshot.getChildren()) {
              String foundTreasureId = child.child("treasureId").getValue(String.class);
              if (treasureId.equals(foundTreasureId)) {
                discovered = true;
                break;
              }
            }
            BitmapDescriptor icon;
            if (discovered) {
              icon = getScaledBitmapDescriptor(R.drawable.ic_treasure_discovered);
            } else if (currentUserId.equals(createdByUserId)) {
              icon = getScaledBitmapDescriptor(R.drawable.ic_treasure_silver);
            } else {
              icon = getScaledBitmapDescriptor(R.drawable.ic_treasure_gold);
            }

            Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lng))
                .title(title)
                .snippet(desc)
                .icon(icon));
            if (marker != null) {
              marker.setTag(new TreasureInfo(treasureId, createdByUserId, totalDiscoveries));
              startBigBounceAnimation(marker);
            }
          }

          @Override
          public void onCancelled(@NonNull DatabaseError error) {
            // Handle error if needed
          }
        });
  }

  /**
   * Handles marker click events to show discovery dialog or initiate discovery.
   *
   * @param marker The clicked marker
   */
  private void handleMarkerClick(Marker marker) {
    TreasureInfo info = (TreasureInfo) marker.getTag();
    if (info == null) {
      return;
    }

    String currentUserId = user.getUid();

    if (currentUserId.equals(info.createdByUserId)) {
      showDiscoveryDialog(marker, info, DiscoveryDialogType.OWN_TREASURE, 0, 0);
    } else {
      discoveriesRef.orderByChild("userId").equalTo(currentUserId)
          .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
              boolean alreadyDiscovered = false;
              long discoveryTime = 0;
              int pointsEarned = 0;

              for (DataSnapshot child : snapshot.getChildren()) {
                String foundTreasureId = child.child("treasureId").getValue(String.class);
                if (info.treasureId.equals(foundTreasureId)) {
                  alreadyDiscovered = true;
                  Long ts = child.child("discoveryTime").getValue(Long.class);
                  Integer points = child.child("pointsEarned").getValue(Integer.class);
                  if (ts != null) {
                    discoveryTime = ts;
                  }
                  if (points != null) {
                    pointsEarned = points;
                  }
                  break;
                }
              }

              if (alreadyDiscovered) {
                showDiscoveryDialog(marker, info, DiscoveryDialogType.REVISIT, discoveryTime,
                    pointsEarned);
              } else {
                checkProximityAndDiscover(marker, info);
              }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
              showToast("Error checking discovery");
            }
          });
    }
  }

  /**
   * Checks the proximity of the user to the treasure marker and initiates discovery if within
   * range.
   *
   * @param marker The marker representing the treasure
   * @param info   The TreasureInfo object containing treasure details
   */
  private void checkProximityAndDiscover(Marker marker, TreasureInfo info) {
    getCurrentLocation(location -> {
      if (location == null) {
        return;
      }

      float[] distance = new float[1];
      Location.distanceBetween(location.getLatitude(), location.getLongitude(),
          marker.getPosition().latitude, marker.getPosition().longitude, distance);

      if (distance[0] <= DISCOVERY_RADIUS_METERS) {
        handleDiscovery(marker, info);
      } else {
        showToast("Move closer to discover this treasure!");
      }
    });
  }

  /**
   * Handles the discovery of a treasure by saving it to the database and updating user points.
   *
   * @param marker The marker representing the discovered treasure
   * @param info   The TreasureInfo object containing treasure details
   */
  private void handleDiscovery(Marker marker, TreasureInfo info) {
    // Save discovery
    String discoveryId = discoveriesRef.push().getKey();
    final int pointsEarned;
    if (info.totalDiscoveries != null && Integer.parseInt(info.totalDiscoveries) < 3) {
      pointsEarned = 10;
    } else {
      pointsEarned = 0;
    }
    if (discoveryId != null) {
      Map<String, Object> discovery = new HashMap<>();
      discovery.put("userId", user.getUid());
      discovery.put("treasureId", info.treasureId);
      discovery.put("discoveryTime", System.currentTimeMillis());
      discovery.put("pointsEarned", pointsEarned);
      discoveriesRef.child(discoveryId).setValue(discovery);
    }
    treasuresRef.child(info.treasureId).child("totalDiscoveries")
        .setValue(Integer.parseInt(info.totalDiscoveries) + 1);

    // Update user points in userstats
    DatabaseReference userStatsRef = FirebaseDatabase.getInstance()
        .getReference("userstats");
    String userId = user.getUid();
    userStatsRef.orderByChild("userid").equalTo(userId)
        .addListenerForSingleValueEvent(new ValueEventListener() {
          @Override
          public void onDataChange(@NonNull DataSnapshot snapshot) {
            for (DataSnapshot child : snapshot.getChildren()) {
              Integer currentPoints = child.child("points").getValue(Integer.class);
              int newPoints = (currentPoints != null ? currentPoints : 0) + pointsEarned;
              child.getRef().child("points").setValue(newPoints);
            }
          }

          @Override
          public void onCancelled(@NonNull DatabaseError error) {
            // Optionally handle error
          }
        });

    // Show discovery dialog
    showDiscoveryDialog(marker, info, DiscoveryDialogType.FIRST_DISCOVERY, 0, pointsEarned);

    // Change marker icon to discovered
    marker.setIcon(getScaledBitmapDescriptor(R.drawable.ic_treasure_discovered));
  }

  /**
   * Saves the discovered treasure to the user's bookmarks.
   *
   * @param treasureId The ID of the treasure to bookmark
   */
  private void saveTreasureToBookmarks(String treasureId) {
    String bookmarkId = savedTreasuresRef.push().getKey();
    if (bookmarkId != null) {
      Map<String, Object> bookmark = new HashMap<>();
      bookmark.put("userId", user.getUid());
      bookmark.put("treasureId", treasureId);

      // Save the bookmark to Firebase Realtime Database
      savedTreasuresRef.child(bookmarkId).setValue(bookmark)
          .addOnSuccessListener(aVoid -> showToast("Treasure Saved"))
          .addOnFailureListener(e -> showToast("Failed to save bookmark"));
    }
  }

  /**
   * Starts a big bounce animation for the marker to indicate discovery.
   *
   * @param marker The marker to animate
   */
  private void startBigBounceAnimation(final Marker marker) {
    final Handler handler = new Handler();
    final long duration = 800;
    final double bounceHeight = 0.00015;
    final Interpolator interpolator = new BounceInterpolator();

    final LatLng markerPos = marker.getPosition();
    final Runnable[] bounceRunnable = new Runnable[1];

    bounceRunnable[0] = new Runnable() {
      @Override
      public void run() {
        final long startTime = SystemClock.uptimeMillis();

        handler.post(new Runnable() {
          @Override
          public void run() {
            long elapsed = SystemClock.uptimeMillis() - startTime;
            float t = (float) elapsed / duration;

            if (t > 1) {
              t = 1;
            }

            double offset = Math.sin(t * Math.PI) * bounceHeight * interpolator.getInterpolation(t);
            marker.setPosition(new LatLng(markerPos.latitude + offset, markerPos.longitude));

            if (t < 1) {
              handler.postDelayed(this, 16);
            } else {
              handler.postDelayed(bounceRunnable[0], 300);
            }
          }
        });
      }
    };

    handler.post(bounceRunnable[0]);
  }

  /**
   * Shows a dialog with discovery information based on the type of discovery.
   *
   * @param marker              The marker representing the treasure
   * @param info                The TreasureInfo object containing treasure details
   * @param type                The type of discovery dialog to show
   * @param discoveryTimeMillis The time of discovery in milliseconds
   * @param pointsEarned        Points earned for discovering the treasure
   */
  private void showDiscoveryDialog(Marker marker, TreasureInfo info, DiscoveryDialogType type,
      long discoveryTimeMillis, int pointsEarned) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    View popupView = LayoutInflater.from(this).inflate(R.layout.dialog_discovery_info, null);
    builder.setView(popupView);

    // Initialize dialog views
    MaterialCardView imageCard = popupView.findViewById(R.id.imageCard);
    ImageView treasureImage = popupView.findViewById(R.id.discoveryImage);
    TextView treasureTitle = popupView.findViewById(R.id.tvTreasureTitle);
    TextView treasureDesc = popupView.findViewById(R.id.tvTreasureDesc);
    TextView discoveryInfo = popupView.findViewById(R.id.tvDiscoveryInfo);
    ImageView btnBookmark = popupView.findViewById(R.id.btnBookmark);
    ImageView btnClose = popupView.findViewById(R.id.btnClose);

    treasureTitle.setText(marker.getTitle());
    treasureDesc.setText(marker.getSnippet());

    LinearLayout infoBoxContainer = popupView.findViewById(R.id.infoBoxContainer);

    // Set discovery info based on the type of discovery
    switch (type) {
      case FIRST_DISCOVERY:
        discoveryInfo.setText(
            "🎉 You discovered a treasure!\nCongrats! You got " + pointsEarned + " points!");
        btnBookmark.setVisibility(View.VISIBLE);
        infoBoxContainer.setBackgroundResource(R.drawable.bg_info_green_box);
        break;
      case REVISIT:
        String formattedDate = DateFormat.format("MMM dd, yyyy 'at' hh:mm a",
            discoveryTimeMillis).toString();
        discoveryInfo.setText(
            "You discovered this treasure on " + formattedDate + (pointsEarned != 0 ? (
                "\nYou earned " + pointsEarned
                    + " points!") : ""));
        btnBookmark.setVisibility(View.VISIBLE);
        infoBoxContainer.setBackgroundResource(R.drawable.bg_info_blue_box);
        break;
      case OWN_TREASURE:
        discoveryInfo.setText("This is your own treasure!");
        btnBookmark.setVisibility(View.INVISIBLE);
        infoBoxContainer.setBackgroundResource(R.drawable.bg_info_blue_box);
        break;
    }

    // Check if already bookmarked
    String currentUserId = user.getUid();
    savedTreasuresRef.orderByChild("userId").equalTo(currentUserId)
        .addListenerForSingleValueEvent(new ValueEventListener() {
          @Override
          public void onDataChange(@NonNull DataSnapshot snapshot) {
            boolean isBookmarked = false;
            for (DataSnapshot child : snapshot.getChildren()) {
              String tid = child.child("treasureId").getValue(String.class);
              if (info.treasureId.equals(tid)) {
                isBookmarked = true;
                break;
              }
            }
            btnBookmark.setImageResource(isBookmarked
                ? android.R.drawable.btn_star_big_on
                : android.R.drawable.btn_star_big_off);
          }

          @Override
          public void onCancelled(@NonNull DatabaseError error) {
            btnBookmark.setImageResource(android.R.drawable.btn_star_big_off);
          }
        });

    // Set bookmark button click listener
    btnBookmark.setOnClickListener(v -> toggleBookmark(info, btnBookmark));

    // Hide image by default
    imageCard.setVisibility(View.GONE);

    // Load treasure image if available
    treasuresRef.child(info.treasureId).child("imageUrl")
        .addListenerForSingleValueEvent(new ValueEventListener() {
          @Override
          public void onDataChange(@NonNull DataSnapshot snapshot) {
            String imageUrl = snapshot.getValue(String.class);
            if (imageUrl != null && !imageUrl.isEmpty()) {
              imageCard.setVisibility(View.VISIBLE);
              Glide.with(TreasureHuntActivity.this)
                  .load(imageUrl)
                  .placeholder(R.drawable.ic_image_placeholder)
                  .error(R.drawable.ic_image_placeholder)
                  .listener(
                      new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                            Target<android.graphics.drawable.Drawable> target,
                            boolean isFirstResource) {
                          imageCard.setVisibility(View.GONE);
                          return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                            Object model, Target<Drawable> target, DataSource dataSource,
                            boolean isFirstResource) {
                          imageCard.setVisibility(View.VISIBLE);
                          return false;
                        }
                      })
                  .into(treasureImage);
            } else {
              imageCard.setVisibility(View.GONE);
            }
          }

          @Override
          public void onCancelled(@NonNull DatabaseError error) {
            imageCard.setVisibility(View.GONE);
          }
        });

    // Create and show the dialog
    AlertDialog dialog = builder.create();
    btnClose.setOnClickListener(v -> dialog.dismiss());
    Objects.requireNonNull(dialog.getWindow())
        .setBackgroundDrawableResource(android.R.color.transparent);
    dialog.show();
  }

  /**
   * Toggles the bookmark state of a treasure.
   *
   * @param info        The TreasureInfo object containing treasure details
   * @param btnBookmark The ImageView representing the bookmark button
   */
  private void toggleBookmark(TreasureInfo info, ImageView btnBookmark) {
    String currentUserId = user.getUid();
    savedTreasuresRef.orderByChild("userId").equalTo(currentUserId)
        .addListenerForSingleValueEvent(new ValueEventListener() {
          @Override
          public void onDataChange(@NonNull DataSnapshot snapshot) {
            boolean isBookmarked = false;
            String bookmarkKey = null;
            for (DataSnapshot child : snapshot.getChildren()) {
              String tid = child.child("treasureId").getValue(String.class);
              if (info.treasureId.equals(tid)) {
                isBookmarked = true;
                bookmarkKey = child.getKey();
                break;
              }
            }

            // Toggle bookmark state
            if (isBookmarked && bookmarkKey != null) {
              savedTreasuresRef.child(bookmarkKey).removeValue()
                  .addOnSuccessListener(aVoid -> {
                    btnBookmark.setImageResource(android.R.drawable.btn_star_big_off);
                    showToast("Treasure Removed");
                  });
            } else {
              saveTreasureToBookmarks(info.treasureId);
              btnBookmark.setImageResource(android.R.drawable.btn_star_big_on);
            }
          }

          @Override
          public void onCancelled(@NonNull DatabaseError error) {
            showToast("Treasure save action failed");
          }
        });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    // Handle image selection from gallery or camera
    if (resultCode == RESULT_OK) {
      if (requestCode == Constants.PICK_IMAGE_REQUEST && data != null) {
        selectedImageUri = data.getData();
        if (imgPreview != null) {
          imgPreview.setImageURI(selectedImageUri);
        }
      } else if (requestCode == Constants.CAMERA_REQUEST && data != null) {
        Bitmap photo = (Bitmap) data.getExtras().get("data");
        selectedImageUri = Uri.parse(
            MediaStore.Images.Media.insertImage(getContentResolver(), photo, "NewTreasure", null));
        if (imgPreview != null) {
          imgPreview.setImageURI(selectedImageUri);
        }
      }
    }
  }

  /**
   * Shows a message indicating that location and camera permissions are required.
   */
  private void showPermissionRequiredMessage() {
    new AlertDialog.Builder(this)
        .setTitle("Permissions Needed")
        .setMessage("Location, camera, and storage permissions are required for this feature.")
        .setPositiveButton("Settings", (dialog, which) -> {
          startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
        })
        .setNegativeButton("Exit", (dialog, which) -> finish())
        .setCancelable(false)
        .show();
  }

  /**
   * Displays a toast message to the user.
   *
   * @param message The message to display
   */
  private void showToast(String message) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == Constants.LOCATION_PERMISSION_REQUEST_CODE) {
      handlePermissionResult(grantResults);
    }
  }

  /**
   * Handles the result of permission requests.
   *
   * @param grantResults Array of grant results for the requested permissions
   */
  private void handlePermissionResult(int[] grantResults) {
    boolean allPermissionsGranted = true;

    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        allPermissionsGranted = false;
        break;
      }
    }

    // Check if all required permissions are granted
    if (allPermissionsGranted) {
      hasLocationPermission = true;
      if (isMapReady) {
        enableMyLocationOnMap();
      }
      showToast("Permissions granted!");
    } else {
      hasLocationPermission = false;
      new AlertDialog.Builder(this)
          .setTitle("Permissions Required")
          .setMessage(
              "This app cannot function without all required permissions. Please grant them in settings.")
          .setCancelable(false)
          .setPositiveButton("Go to Settings", (dialog, which) -> {
            Intent intent = new Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            finish();
          })
          .setNegativeButton("Exit", (dialog, which) -> finish())
          .show();
    }
  }

  /**
   * This method shows a loading dialog while the treasure is being created.
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

  @FunctionalInterface
  private interface LocationCallback {

    void onLocationReceived(@Nullable Location location);
  }

  /**
   * Callback interface for image upload completion.
   */
  private interface OnImageUploadCallback {

    /**
     * Called when the image is successfully uploaded.
     *
     * @param imageUrl The URL of the uploaded image
     */
    void onImageUploaded(String imageUrl);
  }

  /**
   * Helper class to store marker info
   */
  private static class TreasureInfo {

    // Fields to store treasure information
    String treasureId;
    String createdByUserId;
    String totalDiscoveries;

    /**
     * Constructor for TreasureInfo.
     *
     * @param treasureId       The ID of the treasure
     * @param createdByUserId  The user ID of the creator
     * @param totalDiscoveries Total discoveries of the treasure
     */
    TreasureInfo(String treasureId, String createdByUserId, String totalDiscoveries) {
      this.treasureId = treasureId;
      this.createdByUserId = createdByUserId;
      this.totalDiscoveries = totalDiscoveries;
    }
  }
}
