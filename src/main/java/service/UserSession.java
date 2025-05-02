package model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.prefs.Preferences;

public class UserSession {
    // singleton instance
    private static volatile UserSession instance;

    // lcok for thread safety
    private static final ReentrantLock lock = new ReentrantLock();


    private String username;
    private LocalDateTime loginTime;
    private AtomicBoolean loggedIn = new AtomicBoolean(false);


    private Preferences prefs;


    private static final String PREF_USERNAME = "username";
    private static final String PREF_PASSWORD = "password";


    private UserSession() {

        prefs = Preferences.userNodeForPackage(UserSession.class);
    }

    public static UserSession getInstance() {

        if (instance == null) {



            // Acquire lock
            lock.lock();
            try {

                if (instance == null) {
                    instance = new UserSession();
                }
            } finally {

                lock.unlock();
            }
        }
        return instance;
    }


    public void startSession(String username, String password, boolean rememberMe) {
        lock.lock();
        try {
            this.username = username;
            this.loginTime = LocalDateTime.now();
            this.loggedIn.set(true);


            if (rememberMe) {
                prefs.put(PREF_USERNAME, username);


                prefs.put(PREF_PASSWORD, password);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * End the current user session
     */
    public void endSession() {
        lock.lock();
        try {
            this.username = null;
            this.loginTime = null;
            this.loggedIn.set(false);
        } finally {
            lock.unlock();
        }
    }


    public String getUsername() {
        lock.lock();
        try {
            return username;
        } finally {
            lock.unlock();
        }
    }


    public LocalDateTime getLoginTime() {
        lock.lock();
        try {
            return loginTime;
        } finally {
            lock.unlock();
        }
    }
    public boolean isLoggedIn() {
        return loggedIn.get();
    }

    public String getSavedUsername() {
        return prefs.get(PREF_USERNAME, "");
    }


    public String getSavedPassword() {
        return prefs.get(PREF_PASSWORD, "");
    }


    public boolean hasSavedCredentials() {
        return !getSavedUsername().isEmpty() && !getSavedPassword().isEmpty();
    }


    public void clearSavedCredentials() {
        prefs.remove(PREF_USERNAME);
        prefs.remove(PREF_PASSWORD);
    }


    public boolean registerUser(String username, String password) {


        try {

            Preferences userPrefs = Preferences.userNodeForPackage(UserSession.class).node("registeredUsers");


            if (!userPrefs.get(username, "").isEmpty()) {
                return false; // User already exists
            }


            userPrefs.put(username, password);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     *
     *
     * @param username the username to authenticate
     * @param password the password to authenticate
     * @return true if authentication was successful
     */
    public boolean authenticateUser(String username, String password) {
        Preferences userPrefs = Preferences.userNodeForPackage(UserSession.class).node("registeredUsers");
        String storedPassword = userPrefs.get(username, null);


        return storedPassword != null && storedPassword.equals(password);
    }
}