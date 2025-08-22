package edu.northeastern.numad25su_group6.pojo;

/**
 * Treasure is a simple POJO class that represents a treasure item with a title and an image URL.
 */
public class Treasure {

  // Fields of the Treasure class
  public String title;
  public String description;
  public String imageUrl;

  /**
   * Default constructor required for calls to DataSnapshot.getValue(Treasure.class)
   */
  public Treasure(String title, String description, String imageUrl) {
    this.title = title;
    this.description = description;
    this.imageUrl = imageUrl;
  }

  /**
   * Gets the title of the treasure.
   *
   * @return the title of the treasure
   */
  public String getTitle() {
    return title;
  }

  /**
   * Gets the image URL of the treasure.
   *
   * @return the image URL of the treasure
   */
  public String getImageUrl() {
    return imageUrl;
  }

  /**
   * Gets the description of the treasure.
   *
   * @return the description of the treasure
   */
  public String getDescription() {
    return description;
  }
}