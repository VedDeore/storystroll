package edu.northeastern.numad25su_group6.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import edu.northeastern.numad25su_group6.R;
import edu.northeastern.numad25su_group6.adapters.LeaderboardAdapter;
import edu.northeastern.numad25su_group6.pojo.LeaderboardItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * LeaderboardActivity displays a leaderboard of users based on their points. It fetches user
 * statistics from Firebase and displays them in a RecyclerView.
 */
public class LeaderboardActivity extends BaseActivity {

  // UI components
  private RecyclerView recyclerView;
  private View currentUserItem;
  private LeaderboardAdapter adapter;

  // Leaderboard data
  private final List<LeaderboardItem> leaderboardList = new ArrayList<>();

  // Loading state
  private boolean isLoading = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_leaderboard);
    setupToolbarWithProfile();
    setupDrawerWithHamburger();
    setupNavigationView();

    // Initialize UI components
    currentUserItem = findViewById(R.id.current_user_item);
    recyclerView = findViewById(R.id.leaderboard_recycler);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    adapter = new LeaderboardAdapter(leaderboardList,
        Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());
    recyclerView.setAdapter(adapter);

    // Fetch the full leaderboard data from Firebase
    fetchFullLeaderboard();
  }

  /**
   * Fetches the full leaderboard data from Firebase and populates the RecyclerView. It retrieves
   * user statistics and user details, then combines them into LeaderboardItem objects.
   */
  private void fetchFullLeaderboard() {
    isLoading = true;
    adapter.showLoading(true);

    // Reference to the user statistics in Firebase
    DatabaseReference statsRef = FirebaseDatabase.getInstance().getReference("userstats");
    statsRef.orderByChild("points").addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot snapshot) {
        leaderboardList.clear();
        List<LeaderboardItem> tempList = new ArrayList<>();
        List<String> userIds = new ArrayList<>();
        Map<String, Integer> pointsMap = new HashMap<>();

        // Iterate through the user statistics snapshot to collect user IDs and points
        for (DataSnapshot child : snapshot.getChildren()) {
          String userId = child.child("userid").getValue(String.class);
          Integer pointsObj = child.child("points").getValue(Integer.class);
          int points = pointsObj != null ? pointsObj : 0;
          if (userId != null) {
            userIds.add(userId);
            pointsMap.put(userId, points);
          }
        }

        // Firebase returns ascending, reverse for descending
        Collections.reverse(userIds);

        // Fetch user details for each user ID
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
          @Override
          public void onDataChange(@NonNull DataSnapshot usersSnap) {
            for (String userId : userIds) {
              String firstName = usersSnap.child(userId).child("firstname").getValue(String.class);
              String lastName = usersSnap.child(userId).child("lastname").getValue(String.class);
              String fullName = (firstName != null && lastName != null) ? firstName + " " + lastName
                  : (firstName != null ? firstName : "User");
              int points = pointsMap.get(userId);
              String profileImageUrl = usersSnap.child(userId).child("profileImageUrl")
                  .getValue(String.class);
              tempList.add(new LeaderboardItem(userId, fullName, points, -1, profileImageUrl));
            }

            // Remove duplicates if any
            Set<String> seen = new HashSet<>();
            tempList.removeIf(item -> !seen.add(item.userId));

            leaderboardList.addAll(tempList);

            // Assign ranks to the leaderboard items
            assignRanksToLeaderboard();

            // Update the adapter with the new data
            adapter.showLoading(false);
            adapter.notifyDataSetChanged();
            isLoading = false;

            // Set the current user item from the fetched list
            setCurrentUserItemFromList();
          }

          @Override
          public void onCancelled(@NonNull DatabaseError error) {
            isLoading = false;
          }
        });
      }

      @Override
      public void onCancelled(@NonNull DatabaseError error) {
        isLoading = false;
      }
    });
  }

  /**
   * Assigns ranks to the leaderboard items based on their points. The items are sorted in
   * descending order of points, and ranks are assigned accordingly.
   */
  private void assignRanksToLeaderboard() {
    leaderboardList.sort((a, b) -> Integer.compare(b.points, a.points));

    int prevPoints = -1;
    int rank = 1;

    for (int i = 0; i < leaderboardList.size(); i++) {
      LeaderboardItem item = leaderboardList.get(i);

      if (item.points != prevPoints) {
        rank = i + 1;
        prevPoints = item.points;
      }

      item.rank = rank;
    }
  }

  /**
   * Sets the current user's item in the leaderboard from the fetched list. It finds the current
   * user by their Firebase user ID and updates the UI accordingly.
   */
  private void setCurrentUserItemFromList() {
    String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser())
        .getUid();
    LeaderboardItem currentUser = null;
    int currentUserRank = -1;

    for (LeaderboardItem item : leaderboardList) {
      if (item.userId.equals(currentUserId)) {
        currentUser = item;
        currentUserRank = item.rank;
        break;
      }
    }

    // Update the current user item view
    if (currentUser != null) {
      TextView rankView = currentUserItem.findViewById(R.id.rank);
      TextView nameView = currentUserItem.findViewById(R.id.username);
      TextView pointsView = currentUserItem.findViewById(R.id.tv_points);
      ImageView profileView = currentUserItem.findViewById(R.id.profile);

      // Set the current user's rank, name, and points
      rankView.setText("#" + currentUserRank);
      nameView.setText(currentUser.username);
      pointsView.setText(String.valueOf(currentUser.points));

      // Load profile image
      if (currentUser.profileImageUrl != null && !currentUser.profileImageUrl.isEmpty()) {
        Glide.with(profileView.getContext())
            .load(currentUser.profileImageUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .error(R.drawable.ic_profile_placeholder)
            .into(profileView);
      } else {
        profileView.setImageResource(R.drawable.ic_profile_placeholder);
      }

      // Show the current user item
      currentUserItem.setVisibility(View.VISIBLE);
    } else {
      // If the current user is not found in the leaderboard, hide the current user item
      currentUserItem.setVisibility(View.GONE);
    }
  }
}