package edu.northeastern.numad25su_group6.activities;

import android.os.Bundle;

import android.view.View;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import edu.northeastern.numad25su_group6.R;
import edu.northeastern.numad25su_group6.adapters.TreasureListAdapter;
import edu.northeastern.numad25su_group6.constants.DiscoveryDialogType;
import edu.northeastern.numad25su_group6.pojo.Treasure;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * MyTreasuresActivity displays a list of treasures created by the user. It extends BaseActivity to
 * inherit common functionality such as toolbar and navigation setup.
 */
public class MyTreasuresActivity extends BaseActivity {

  // UI components
  private RecyclerView recyclerView;
  private TextView noRecordsText;
  private TreasureListAdapter adapter;
  private final ArrayList<Treasure> treasures = new ArrayList<>();
  private final List<String> treasureIds = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_my_treasures);
    setupToolbarWithProfile();
    setupDrawerWithHamburger();
    setupNavigationView();

    // Setup UI components
    recyclerView = findViewById(R.id.my_treasures_recycler);
    noRecordsText = findViewById(R.id.no_records_text);
    recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
    adapter = new TreasureListAdapter(treasures, treasureIds, treasureId -> {
      showTreasureInfoDialog(this, DiscoveryDialogType.OWN_TREASURE, treasureId, false);
    });
    recyclerView.setAdapter(adapter);

    // Fetch user's treasures from the database
    fetchMyTreasures();
  }

  /**
   * Fetches the treasures created by the current user from Firebase Realtime Database. The
   * treasures are filtered by the user's ID and displayed in the RecyclerView.
   */
  private void fetchMyTreasures() {
    String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
    DatabaseReference treasuresRef = FirebaseDatabase.getInstance().getReference("treasures");
    treasuresRef.orderByChild("createdByUserId").equalTo(userId)
        .addListenerForSingleValueEvent(new ValueEventListener() {
          @Override
          public void onDataChange(@NonNull DataSnapshot snapshot) {
            treasures.clear();
            treasureIds.clear();
            for (DataSnapshot snap : snapshot.getChildren()) {
              String treasureId = snap.getKey();
              String title = snap.child("title").getValue(String.class);
              String imageUrl = snap.child("imageUrl").getValue(String.class);
              String description = snap.child("description").getValue(String.class);
              treasures.add(new Treasure(title, description, imageUrl));
              treasureIds.add(treasureId);
            }
            adapter.notifyDataSetChanged();

            // Show or hide the no records text based on whether treasures are found
            if (treasures.isEmpty()) {
              noRecordsText.setVisibility(View.VISIBLE);
              recyclerView.setVisibility(View.GONE);
            } else {
              noRecordsText.setVisibility(View.GONE);
              recyclerView.setVisibility(View.VISIBLE);
            }
          }

          @Override
          public void onCancelled(@NonNull DatabaseError error) {
          }
        });
  }
}