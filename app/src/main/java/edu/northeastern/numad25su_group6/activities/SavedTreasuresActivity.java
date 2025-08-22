package edu.northeastern.numad25su_group6.activities;

import android.os.Bundle;

import android.view.View;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
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
 * SavedTreasuresActivity displays a list of saved treasures for the user. It extends BaseActivity
 * to inherit common functionality such as toolbar and navigation setup.
 */
public class SavedTreasuresActivity extends BaseActivity {

  // UI components and data references
  private RecyclerView recyclerView;
  private TextView noRecordsText;
  private TreasureListAdapter adapter;
  private final List<Treasure> treasures = new ArrayList<>();
  private DatabaseReference savedRef;
  private DatabaseReference treasuresRef;
  private String userId;
  private final List<String> treasureIds = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_saved_treasures);
    setupToolbarWithProfile();
    setupDrawerWithHamburger();
    setupNavigationView();

    // Setup UI components
    recyclerView = findViewById(R.id.saved_treasures_recycler);
    noRecordsText = findViewById(R.id.no_records_text);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    adapter = new TreasureListAdapter(treasures, treasureIds, treasureId -> {
      showTreasureInfoDialog(this, DiscoveryDialogType.REVISIT, treasureId, true);
    });
    recyclerView.setAdapter(adapter);

    // Get current user ID and database references
    userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
    savedRef = FirebaseDatabase.getInstance().getReference("savedTreasures");
    treasuresRef = FirebaseDatabase.getInstance().getReference("treasures");

    // Load saved treasures from the database
    fetchSavedTreasures();
  }

  /**
   * Fetches the saved treasures for the current user from Firebase Realtime Database. The treasures
   * are filtered by the user's ID and displayed in the RecyclerView.
   */
  protected void fetchSavedTreasures() {
    savedRef.orderByChild("userId").equalTo(userId)
        .addListenerForSingleValueEvent(new ValueEventListener() {
          @Override
          public void onDataChange(@NonNull DataSnapshot snapshot) {
            treasures.clear();
            treasureIds.clear();
            if (!snapshot.hasChildren()) {
              noRecordsText.setVisibility(View.VISIBLE);
              recyclerView.setVisibility(View.GONE);
              adapter.notifyDataSetChanged();
              return;
            }
            for (DataSnapshot savedSnap : snapshot.getChildren()) {
              String treasureId = savedSnap.child("treasureId").getValue(String.class);
              if (treasureId == null) {
                continue;
              }
              treasuresRef.child(treasureId)
                  .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot treasureSnap) {
                      String title = treasureSnap.child("title").getValue(String.class);
                      String imageUrl = treasureSnap.child("imageUrl").getValue(String.class);
                      String description = treasureSnap.child("description").getValue(String.class);
                      treasures.add(new Treasure(title, description, imageUrl));
                      treasureIds.add(treasureId);
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

          @Override
          public void onCancelled(@NonNull DatabaseError error) {
          }
        });
  }
}