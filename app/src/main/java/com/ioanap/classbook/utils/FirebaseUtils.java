package com.ioanap.classbook.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ioanap.classbook.R;
import com.ioanap.classbook.SignInActivity;
import com.ioanap.classbook.child.ChildProfileActivity;
import com.ioanap.classbook.model.User;
import com.ioanap.classbook.model.UserAccountSettings;
import com.ioanap.classbook.parent.ParentProfileActivity;
import com.ioanap.classbook.teacher.TeacherDrawerActivity;

import static android.content.Context.MODE_PRIVATE;

public class FirebaseUtils {

    private static final String TAG = "FirebaseUtils";

    private FirebaseAuth mAuth;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mRootRef, mUserRef, mSettingsRef;
    private String userID;

    private Context mContext;

    private ProgressDialog mProgressDialog;

    public FirebaseUtils(Context context) {
        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mRootRef = mFirebaseDatabase.getReference();
        mUserRef = mRootRef.child("users");
        mSettingsRef = mRootRef.child("user_account_settings");
        mContext = context;
        mProgressDialog = new ProgressDialog(mContext);

        if(mAuth.getCurrentUser() != null){
            userID = mAuth.getCurrentUser().getUid();
        }
    }

    /**
     * Registers new user and on success adds his information to the database and sends a verification
     * link to his email address. The user will be then redirected to the login page.
     *
     * @param user      information to add to the database if register is successful
     * @param email     email for the user that will be registered
     * @param password  password for the user that will be registered
     */
    public void registerNewUser(final User user, String email, String password){
        mProgressDialog.setMessage("Signing Up...");
        mProgressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        mProgressDialog.dismiss();
                        if (task.isSuccessful()) {
                            // save info to firebase
                            FirebaseUser currentUser = task.getResult().getUser();
                            user.setId(currentUser.getUid());
                            addUserInfo(user, new UserAccountSettings());

                            // send verification link to the email address
                            sendEmailVerification();

                            Toast.makeText(mContext, "User registered", Toast.LENGTH_SHORT).show();

                            mAuth.signOut();

                            // redirect to login
                            mContext.startActivity(new Intent(mContext, SignInActivity.class));
                        }
                        else {
                            Toast.makeText(mContext, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }

                    }
                });
    }

    /**
     * Adds data to Firebase for the user with ID user.getId().
     *
     * @param user
     * @param settings
     */
    public void addUserInfo(User user, UserAccountSettings settings) {
        mUserRef.child(user.getId()).setValue(user);

        settings.setId(user.getId());
        settings.setEmail(user.getEmail());
        settings.setUserType(user.getUserType());
        mSettingsRef.child(user.getId()).setValue(settings);
    }

    public UserAccountSettings getUserAccountSettings(DataSnapshot dataSnapshot) {
        Log.d(TAG, "getting user account settings");

        Log.d(TAG, "dataSnapshot: " + dataSnapshot);

        UserAccountSettings settings = dataSnapshot.child(userID).getValue(UserAccountSettings.class);
        Log.d(TAG, "settings: " + settings);

        return settings;
    }

    /**
     * Update "user_account_settings" node for current user.
     *
     * @param name
     * @param description
     * @param location
     * @param phoneNumber
     */
    public void updateUserAccountSettings(String name, String description, String location, String phoneNumber, String profilePhoto) {
        DatabaseReference ref = mSettingsRef.child(userID);

        if (name != null) {
            ref.child(mContext.getString(R.string.field_name)).setValue(name);
        }
        if (description != null) {
            ref.child(mContext.getString(R.string.field_description)).setValue(description);
        }
        if (location != null) {
            ref.child(mContext.getString(R.string.field_location)).setValue(location);
        }
        if (phoneNumber != null) {
            ref.child(mContext.getString(R.string.field_phone_number)).setValue(phoneNumber);
        }
        if (profilePhoto != null) {
            ref.child(mContext.getString(R.string.field_profile_photo)).setValue(profilePhoto);
        }
    }

    /**
     * Sends a verification link to the email address of the currently logged user.
     */
    public void sendEmailVerification(){
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if(currentUser != null){
            currentUser.sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(mContext, "Verification email sent to " + currentUser.getEmail(), Toast.LENGTH_SHORT).show();
                            } else{
                                Toast.makeText(mContext, "Failed to send verification email", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    /**
     * Saves the current state to SharedPreferences (whether there is or not a logged in user and his type)
     *
     * @param logged    tells whether there is a logged in user
     * @param userType  type of the logged in user
     */
    public void saveToSharedPreferences(Boolean logged, String userType) {
        SharedPreferences.Editor editor = mContext.getSharedPreferences("LoginInfo", MODE_PRIVATE).edit();
        editor.putBoolean("logged", logged);
        editor.putString("userType", userType);
        editor.apply();
    }

    /**
     * Redirects the currently logged in user to his profile according to his type i.e Teacher,
     * Parent or Child.
     */
    public void userRedirect() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        mUserRef.child(currentUser.getUid()).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.getValue(User.class);
                        String userType = user.getUserType();

                        // save user type to Shared Preferences for future login
                        saveToSharedPreferences(true, userType);

                        // redirect user to corresponding type of profile and delete all previous
                        // activities from stack
                        Intent intent;
                        if (userType.equals("teacher")) {
                            intent = new Intent(mContext, TeacherDrawerActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            mContext.startActivity(intent);
                        } else if (userType.equals("parent")) {
                            intent = new Intent(mContext, ParentProfileActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            mContext.startActivity(intent);
                        } else {
                            intent = new Intent(mContext, ChildProfileActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            mContext.startActivity(intent);
                        }

                        Log.d(TAG, "redirecting as" + userType);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "failed to redirect user");
                    }
                });

    }

    /**
     * Signs currently logged user out and clears all activities from stack (except from the first
     * one i.e. SignInActivity)
     */
    public void signOut() {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(mContext, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivity(intent);
    }

}
