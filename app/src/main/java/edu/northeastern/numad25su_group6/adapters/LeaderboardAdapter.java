package edu.northeastern.numad25su_group6.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import edu.northeastern.numad25su_group6.pojo.LeaderboardItem;
import java.util.List;
import edu.northeastern.numad25su_group6.R;

/**
 * LeaderboardAdapter is a RecyclerView adapter that displays a list of leaderboard items. It
 * supports showing a loading indicator at the end of the list.
 */
public class LeaderboardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  // View types for the RecyclerView
  private static final int VIEW_TYPE_ITEM = 0;
  private static final int VIEW_TYPE_LOADING = 1;

  // Current user ID to highlight their item in the leaderboard
  private final String currentUserId;

  // List of leaderboard items to display
  private final List<LeaderboardItem> items;
  private boolean isLoading = false;

  /**
   * Constructor for LeaderboardAdapter.
   *
   * @param items         List of leaderboard items to display.
   * @param currentUserId ID of the current user to highlight their item.
   */
  public LeaderboardAdapter(List<LeaderboardItem> items, String currentUserId) {
    this.items = items;
    this.currentUserId = currentUserId;
  }

  /**
   * Show or hide the loading indicator at the end of the list.
   *
   * @param show true to show the loading indicator, false to hide it.
   */
  public void showLoading(boolean show) {
    if (isLoading == show) {
      return;
    }
    isLoading = show;
    if (show) {
      notifyItemInserted(getItemCount());
    } else {
      notifyItemRemoved(getItemCount());
    }
  }

  @Override
  public int getItemCount() {
    return items.size() + (isLoading ? 1 : 0);
  }

  @Override
  public int getItemViewType(int position) {
    if (isLoading && position == getItemCount() - 1) {
      return VIEW_TYPE_LOADING;
    }
    return VIEW_TYPE_ITEM;
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == VIEW_TYPE_LOADING) {
      View v = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.item_loading, parent, false);
      return new LoadingViewHolder(v);
    } else {
      View v = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.item_leaderboard, parent, false);
      return new ItemViewHolder(v);
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    if (holder instanceof ItemViewHolder) {
      LeaderboardItem item = items.get(position);
      ItemViewHolder vh = (ItemViewHolder) holder;
      vh.rank.setText("#" + item.rank);
      vh.username.setText(item.username);
      vh.points.setText(String.valueOf(item.points));

      // Set special background for current user
      if (item.userId.equals(currentUserId)) {
        vh.itemView.setBackgroundResource(R.drawable.bg_current_user_leaderboard);
      } else {
        vh.itemView.setBackgroundResource(android.R.color.transparent);
      }

      // Load profile image
      if (item.profileImageUrl != null && !item.profileImageUrl.isEmpty()) {
        Glide.with(vh.profile.getContext())
            .load(item.profileImageUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .error(R.drawable.ic_error)
            .into(vh.profile);
      } else {
        vh.profile.setImageResource(R.drawable.ic_profile_placeholder);
      }
    }
  }


  /**
   * Inner class for the item view holder.
   */
  static class ItemViewHolder extends RecyclerView.ViewHolder {

    // Views for the leaderboard item
    TextView rank, username, points;
    ImageView profile;

    /**
     * Constructor for ItemViewHolder.
     *
     * @param v The view for the leaderboard item.
     */
    ItemViewHolder(View v) {
      super(v);
      rank = v.findViewById(R.id.rank);
      username = v.findViewById(R.id.username);
      points = v.findViewById(R.id.tv_points);
      profile = v.findViewById(R.id.profile);
    }
  }

  /**
   * Inner class for the loading view holder. This holder is used to display a loading indicator at
   * the end of the list.
   */
  static class LoadingViewHolder extends RecyclerView.ViewHolder {

    /**
     * Constructor for LoadingViewHolder.
     *
     * @param v The view for the loading indicator.
     */
    LoadingViewHolder(View v) {
      super(v);
    }
  }
}