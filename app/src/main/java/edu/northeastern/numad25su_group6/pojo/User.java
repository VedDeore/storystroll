package edu.northeastern.numad25su_group6.pojo;

/**
 * User represents a user in the application. It contains user information such as firstname,
 * lastname, and email.
 */
public class User {

  // User information
  private String firstname;
  private String lastname;
  private String email;
  private String profileImageUrl;

  /**
   * Default constructor required for Firebase Realtime Database. It is needed to deserialize the
   * object from DataSnapshot.
   */
  public User() {
  }

  /**
   * Constructor to create a User with firstname, lastname, and email.
   *
   * @param firstname The first name of the user.
   * @param lastname  The last name of the user.
   * @param email     The email address of the user.
   */
  public User(String firstname, String lastname, String email, String profileImageUrl) {
    this.firstname = firstname;
    this.lastname = lastname;
    this.email = email;
    this.profileImageUrl = profileImageUrl;
  }

  /**
   * Getter for firstname.
   *
   * @return The first name of the user.
   */
  public String getFirstname() {
    return firstname;
  }

  /**
   * Setter for firstname.
   *
   * @param firstname The first name of the user.
   */
  public void setFirstname(String firstname) {
    this.firstname = firstname;
  }

  /**
   * Getter for lastname.
   *
   * @return The last name of the user.
   */
  public String getLastname() {
    return lastname;
  }

  /**
   * Setter for lastname.
   *
   * @param lastname The last name of the user.
   */
  public void setLastname(String lastname) {
    this.lastname = lastname;
  }

  /**
   * Getter for email.
   *
   * @return The email address of the user.
   */
  public String getEmail() {
    return email;
  }

  /**
   * Setter for email.
   *
   * @param email The email address of the user.
   */
  public void setEmail(String email) {
    this.email = email;
  }

  public String getProfileImageUrl() {
    return profileImageUrl;
  }

  public void setProfileImageUrl(String profileImageUrl) {
    this.profileImageUrl = profileImageUrl;
  }
}
