package edu.northeastern.numad25su_group6.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import edu.northeastern.numad25su_group6.R;
import edu.northeastern.numad25su_group6.constants.DiscoveryDialogType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * BaseActivity is a class that extends AppCompatActivity. It sets up a toolbar with a profile icon
 * that, when clicked, shows a popup dialog with user profile information.
 */
public class BaseActivity extends AppCompatActivity {

  /**
   * This method is called when the activity is created. It sets up the toolbar with a profile icon.
   * It sets an onClickListener on the profile icon to show a popup dialog with user profile
   * information.
   */
  protected void setupToolbarWithProfile() {
    Toolbar toolbar = findViewById(R.id.profile_toolbar);
    if (toolbar != null) {
      setSupportActionBar(toolbar);
      ImageView profileIcon = toolbar.findViewById(R.id.profile_icon);
      if (profileIcon != null) {
        // Fetch and set profile image
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
          String uid = user.getUid();
          DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users")
              .child(uid);
          userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
              String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
              if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                Glide.with(toolbar.getContext())
                    .load(profileImageUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(profileIcon);
              } else {
                profileIcon.setImageResource(R.drawable.ic_profile);
              }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
              profileIcon.setImageResource(R.drawable.ic_profile);
            }
          });
        }

        // Set onClickListener for the profile icon
        profileIcon.setOnClickListener(v -> showProfilePopup());
      }
    }
  }

  /**
   * This method is called when the user clicks the profile icon in the toolbar. It shows a popup
   * dialog with the user's profile information.
   */
  private void showProfilePopup() {
    Dialog dialog = new Dialog(this);
    dialog.setContentView(R.layout.profile_info_popup);
    Objects.requireNonNull(dialog.getWindow())
        .setBackgroundDrawableResource(android.R.color.transparent);
    dialog.setCanceledOnTouchOutside(true);

    // Set up the close button in the popup dialog
    ImageView closePopup = dialog.findViewById(R.id.close_popup);
    closePopup.setOnClickListener(v -> dialog.dismiss());

    // Initialize UI components in the dialog
    TextView nameView = dialog.findViewById(R.id.name);
    TextView emailView = dialog.findViewById(R.id.email);
    TextView pointsView = dialog.findViewById(R.id.total_points);
    TextView rankingView = dialog.findViewById(R.id.ranking);
    ImageView profileImageView = dialog.findViewById(R.id.profile_image);

    // Get the current user from Firebase Authentication
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user != null) {
      String email = user.getEmail();
      emailView.setText(email);
      String uid = user.getUid();

      // Fetch the user's first and last name from Firebase Realtime Database
      DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
      userRef.addListenerForSingleValueEvent(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
          String firstName = snapshot.child("firstname").getValue(String.class);
          String lastName = snapshot.child("lastname").getValue(String.class);
          String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
          if (firstName != null && lastName != null) {
            nameView.setText(firstName + " " + lastName);
          } else if (firstName != null) {
            nameView.setText(firstName);
          } else {
            nameView.setText("User");
          }

          // Load profile image if available
          if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(BaseActivity.this)
                .load(profileImageUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_error)
                .into(profileImageView);
          } else {
            profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
          }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
          nameView.setText("User");
          profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
        }
      });

      // Fetch points and calculate ranking
      DatabaseReference statsRef = FirebaseDatabase.getInstance().getReference("userstats");
      statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
          int currentUserPoints = 0;
          for (DataSnapshot userSnapshot : snapshot.getChildren()) {
            if (Objects.equals(userSnapshot.child("userid").getValue(String.class), uid)) {
              Integer points = userSnapshot.child("points").getValue(Integer.class);
              if (points != null) {
                currentUserPoints = points;
                pointsView.setText(String.valueOf(points));
              }
              break;
            }
          }
          // Now, calculate ranking
          List<Integer> allPoints = new ArrayList<>();
          for (DataSnapshot userSnapshot : snapshot.getChildren()) {
            Integer points = userSnapshot.child("points").getValue(Integer.class);
            if (points != null) {
              allPoints.add(points);
            }
          }
          // Sort points descending
          allPoints.sort(Collections.reverseOrder());
          int rank = 1;
          int prevPoints = -1;
          for (int i = 0; i < allPoints.size(); i++) {
            int pts = allPoints.get(i);
            if (pts != prevPoints) {
              rank = i + 1;
              prevPoints = pts;
            }
            if (pts == currentUserPoints) {
              break;
            }
          }
          rankingView.setText("#" + rank);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
          pointsView.setText("0");
          rankingView.setText("#-");
        }
      });

      // Fetch total treasures discovered by the user
      TextView treasuresView = dialog.findViewById(R.id.total_treasures);
      DatabaseReference discoveriesRef = FirebaseDatabase.getInstance().getReference("discoveries");
      discoveriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
          int totalTreasures = 0;
          for (DataSnapshot discoverySnap : snapshot.getChildren()) {
            String discoveryUserId = discoverySnap.child("userId").getValue(String.class);
            if (uid.equals(discoveryUserId)) {
              totalTreasures++;
            }
          }
          treasuresView.setText(String.valueOf(totalTreasures));
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
          treasuresView.setText("0");
        }
      });
    }

    // Set up the logout button in the popup dialog
    Button logoutButton = dialog.findViewById(R.id.logout_button);
    logoutButton.setOnClickListener(v -> {
      FirebaseAuth.getInstance().signOut();
      Intent intent = new Intent(this, SignInActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      startActivity(intent);
      finish();
    });

    // Show the dialog
    dialog.show();
  }

  /**
   * This method is called when the activity is created. It sets up the drawer layout with a
   * hamburger icon that opens the navigation drawer when clicked.
   */
  protected void setupDrawerWithHamburger() {
    ImageView hamburgerIcon = findViewById(R.id.hamburger_icon);
    DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
    if (hamburgerIcon != null && drawerLayout != null) {
      hamburgerIcon.setOnClickListener(
          v -> drawerLayout.openDrawer(androidx.core.view.GravityCompat.START));
    }
  }

  /**
   * This is a helper method to set up the navigation view with menu items. It sets an
   * onNavigationItemSelectedListener on the navigation view to handle item clicks.
   */
  protected void setupNavigationView() {
    // Set up the toolbar with window insets for status bar padding
    Toolbar toolbar = findViewById(R.id.profile_toolbar);
    if (toolbar != null) {
      ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
        int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
        v.setPadding(
            v.getPaddingLeft(),
            statusBarHeight,
            v.getPaddingRight(),
            v.getPaddingBottom()
        );
        return insets;
      });
    }

    // Find the drawer layout and navigation view
    DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
    NavigationView navigationView = findViewById(R.id.navigation_view);

    // Navigates to different activities based on the selected menu item
    if (drawerLayout != null && navigationView != null) {
      navigationView.setNavigationItemSelectedListener(item -> {
        int id = item.getItemId();
        Intent intent = null;
        if (id == R.id.nav_home) {
          intent = new Intent(this, TreasureHuntActivity.class);
        } else if (id == R.id.nav_my_treasures) {
          intent = new Intent(this, MyTreasuresActivity.class);
        } else if (id == R.id.nav_saved_treasures) {
          intent = new Intent(this, SavedTreasuresActivity.class);
        } else if (id == R.id.nav_leaderboard) {
          intent = new Intent(this, LeaderboardActivity.class);
        }
        if (intent != null) {
          startActivity(intent);
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
      });
    }
  }

  /**
   * Utility method to show a dialog with treasure discovery information.
   *
   * @param activity     The activity context to show the dialog in.
   * @param type         The type of discovery (e.g., REVISIT, OWN_TREASURE).
   * @param treasureId   The ID of the treasure to show info for.
   * @param showBookmark Whether to show the bookmark button.
   */
  protected void showTreasureInfoDialog(Activity activity, DiscoveryDialogType type,
      String treasureId, boolean showBookmark) {
    DatabaseReference treasuresRef = FirebaseDatabase.getInstance().getReference("treasures");
    DatabaseReference savedTreasuresRef = FirebaseDatabase.getInstance()
        .getReference("savedTreasures");
    DatabaseReference discoveriesRef = FirebaseDatabase.getInstance().getReference("discoveries");
    String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

    // Fetch discovery info for this user and treasure
    discoveriesRef.orderByChild("userId").equalTo(userId)
        .addListenerForSingleValueEvent(new ValueEventListener() {
          @Override
          public void onDataChange(@NonNull DataSnapshot snapshot) {
            long discoveryTimeMillis = 0;
            int pointsEarned = 0;
            for (DataSnapshot child : snapshot.getChildren()) {
              String tid = child.child("treasureId").getValue(String.class);
              if (treasureId.equals(tid)) {
                Long ts = child.child("discoveryTime").getValue(Long.class);
                Integer pts = child.child("pointsEarned").getValue(Integer.class);
                if (ts != null) {
                  discoveryTimeMillis = ts;
                }
                if (pts != null) {
                  pointsEarned = pts;
                }
                break;
              }
            }

            // Fetch treasure details and show dialog
            long finalDiscoveryTimeMillis = discoveryTimeMillis;
            int finalPointsEarned = pointsEarned;
            treasuresRef.child(treasureId).addListenerForSingleValueEvent(new ValueEventListener() {
              @Override
              public void onDataChange(@NonNull DataSnapshot snap) {
                String title = snap.child("title").getValue(String.class);
                String desc = snap.child("description").getValue(String.class);
                String imageUrl = snap.child("imageUrl").getValue(String.class);

                // Create the dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                View popupView = LayoutInflater.from(activity)
                    .inflate(R.layout.dialog_discovery_info, null);
                builder.setView(popupView);

                // Initialize UI components in the dialog
                MaterialCardView imageCard = popupView.findViewById(R.id.imageCard);
                ImageView treasureImage = popupView.findViewById(R.id.discoveryImage);
                TextView treasureTitle = popupView.findViewById(R.id.tvTreasureTitle);
                TextView treasureDesc = popupView.findViewById(R.id.tvTreasureDesc);
                TextView discoveryInfo = popupView.findViewById(R.id.tvDiscoveryInfo);
                ImageView btnBookmark = popupView.findViewById(R.id.btnBookmark);
                ImageView btnClose = popupView.findViewById(R.id.btnClose);

                treasureTitle.setText(title);
                treasureDesc.setText(desc);
                LinearLayout infoBoxContainer = popupView.findViewById(R.id.infoBoxContainer);

                // Set discovery info based on the type of discovery
                switch (type) {
                  case REVISIT:
                    String formattedDate = finalDiscoveryTimeMillis > 0
                        ? DateFormat.format("MMM dd, yyyy 'at' hh:mm a", finalDiscoveryTimeMillis)
                        .toString()
                        : "N/A";
                    discoveryInfo.setText(
                        "You discovered this treasure on " + formattedDate + (finalPointsEarned != 0
                            ?
                            "\nYou earned " + finalPointsEarned + " points!" : ""));
                    btnBookmark.setVisibility(View.VISIBLE);
                    infoBoxContainer.setBackgroundResource(R.drawable.bg_info_blue_box);
                    break;
                  case OWN_TREASURE:
                    discoveryInfo.setText("This is your own treasure!");
                    btnBookmark.setVisibility(View.INVISIBLE);
                    infoBoxContainer.setBackgroundResource(R.drawable.bg_info_blue_box);
                    break;
                }

                // Hide image by default
                imageCard.setVisibility(View.GONE);

                // Load the treasure image
                if (imageUrl != null && !imageUrl.isEmpty()) {
                  imageCard.setVisibility(View.VISIBLE);
                  Glide.with(BaseActivity.this)
                      .load(imageUrl)
                      .placeholder(R.drawable.ic_image_placeholder)
                      .error(R.drawable.ic_image_placeholder)
                      .listener(
                          new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                Target<Drawable> target, boolean isFirstResource) {
                              imageCard.setVisibility(View.GONE);
                              return false;
                            }

                            @Override
                            public boolean onResourceReady(
                                android.graphics.drawable.Drawable resource, Object model,
                                Target<Drawable> target, DataSource dataSource,
                                boolean isFirstResource) {
                              imageCard.setVisibility(View.VISIBLE);
                              return false;
                            }
                          })
                      .into(treasureImage);
                } else {
                  imageCard.setVisibility(View.GONE);
                }

                // Set visibility of bookmark button
                btnBookmark.setVisibility(showBookmark ? View.VISIBLE : View.INVISIBLE);

                // Set bookmark icon state
                if (showBookmark) {
                  savedTreasuresRef.orderByChild("userId").equalTo(userId)
                      .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                          boolean isBookmarked = false;
                          for (DataSnapshot child : snapshot.getChildren()) {
                            String tid = child.child("treasureId").getValue(String.class);
                            if (treasureId.equals(tid)) {
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
                        }
                      });

                  // Set onClickListener for the bookmark button
                  btnBookmark.setOnClickListener(v -> {
                    savedTreasuresRef.orderByChild("userId").equalTo(userId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                          @Override
                          public void onDataChange(@NonNull DataSnapshot snapshot) {
                            boolean isBookmarked = false;
                            String bookmarkKey = null;
                            for (DataSnapshot child : snapshot.getChildren()) {
                              String tid = child.child("treasureId").getValue(String.class);
                              if (treasureId.equals(tid)) {
                                isBookmarked = true;
                                bookmarkKey = child.getKey();
                                break;
                              }
                            }

                            // Toggle bookmark state
                            if (isBookmarked && bookmarkKey != null) {
                              savedTreasuresRef.child(bookmarkKey).removeValue()
                                  .addOnCompleteListener(task -> {
                                    btnBookmark.setImageResource(
                                        android.R.drawable.btn_star_big_off);
                                    if (activity instanceof SavedTreasuresActivity) {
                                      ((SavedTreasuresActivity) activity).fetchSavedTreasures();
                                    }
                                  });
                            } else {
                              String newKey = savedTreasuresRef.push().getKey();
                              if (newKey != null) {
                                Map<String, Object> bookmark = new HashMap<>();
                                bookmark.put("userId", userId);
                                bookmark.put("treasureId", treasureId);
                                savedTreasuresRef.child(newKey).setValue(bookmark)
                                    .addOnCompleteListener(task -> {
                                      btnBookmark.setImageResource(
                                          android.R.drawable.btn_star_big_on);
                                      if (activity instanceof SavedTreasuresActivity) {
                                        ((SavedTreasuresActivity) activity).fetchSavedTreasures();
                                      }
                                    });
                              }
                            }
                          }

                          @Override
                          public void onCancelled(@NonNull DatabaseError error) {
                          }
                        });
                  });
                }

                // Create and show the dialog
                AlertDialog dialog = builder.create();
                btnClose.setOnClickListener(v -> dialog.dismiss());
                Objects.requireNonNull(dialog.getWindow())
                    .setBackgroundDrawableResource(android.R.color.transparent);
                dialog.show();
              }

              @Override
              public void onCancelled(@NonNull DatabaseError error) {
              }
            });
          }

          @Override
          public void onCancelled(@NonNull DatabaseError error) {
          }
        });
  }
}