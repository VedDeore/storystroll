package edu.northeastern.numad25su_group6.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import edu.northeastern.numad25su_group6.R;
import edu.northeastern.numad25su_group6.pojo.Treasure;
import java.util.List;

/**
 * TreasureListAdapter is a RecyclerView adapter that displays a list of treasures. Each treasure
 * item shows an image and a title.
 */
public class TreasureListAdapter extends RecyclerView.Adapter<TreasureListAdapter.ViewHolder> {

  // List of treasures to display in the RecyclerView
  private final List<Treasure> treasures;
  private final List<String> treasureIds;
  private final OnItemClickListener listener;

  /**
   * OnItemClickListener is an interface for handling item click events. It provides a method to
   * handle clicks on a treasure item.
   */
  public interface OnItemClickListener {

    void onItemClick(String treasureId);
  }

  /**
   * Constructor for TreasureListAdapter.
   *
   * @param treasures List of treasures to display in the RecyclerView.
   */
  public TreasureListAdapter(List<Treasure> treasures, List<String> treasureIds,
      OnItemClickListener listener) {
    this.treasures = treasures;
    this.treasureIds = treasureIds;
    this.listener = listener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_saved_treasure, parent, false);
    return new ViewHolder(v);
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    Treasure t = treasures.get(position);
    holder.title.setText(t.title);
    Glide.with(holder.image.getContext())
        .load(t.imageUrl)
        .placeholder(R.drawable.ic_image_placeholder)
        .error(R.drawable.ic_image_placeholder)
        .into(holder.image);
    holder.itemView.setOnClickListener(v -> {
      if (listener != null) {
        listener.onItemClick(treasureIds.get(position));
      }
    });
  }

  @Override
  public int getItemCount() {
    return treasures.size();
  }


  /**
   * ViewHolder is a static inner class that holds the views for each treasure item.
   */
  public static class ViewHolder extends RecyclerView.ViewHolder {

    // Views for the treasure item
    ImageView image;
    TextView title;

    /**
     * Constructor for ViewHolder.
     *
     * @param itemView The view for the treasure item.
     */
    public ViewHolder(View itemView) {
      super(itemView);
      image = itemView.findViewById(R.id.treasure_image);
      title = itemView.findViewById(R.id.treasure_title);
    }
  }
}