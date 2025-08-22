package edu.northeastern.numad25su_group6.pojo;

/**
 * LeaderboardItem represents a single entry in the leaderboard. It contains user information such
 * as userId, username, points, and rank.
 */
public class LeaderboardItem {

  // Item information
  public String userId;
  public String username;
  public int points;
  public int rank;
  public String profileImageUrl;

  /**
   * Default constructor required for Firebase. It is needed to deserialize the object from Firebase
   * Realtime Database.
   */
  public LeaderboardItem() {
  }

  /**
   * Constructor to create a LeaderboardItem with userId, username, points, and rank.
   *
   * @param userId   The unique identifier for the user.
   * @param username The name of the user.
   * @param points   The points scored by the user.
   * @param rank     The rank of the user in the leaderboard.
   */
  public LeaderboardItem(String userId, String username, int points, int rank,
      String profileImageUrl) {
    this.userId = userId;
    this.username = username;
    this.points = points;
    this.rank = rank;
    this.profileImageUrl = profileImageUrl;
  }
}