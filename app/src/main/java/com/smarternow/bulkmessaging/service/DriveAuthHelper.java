package com.smarternow.bulkmessaging.service;

import android.app.Activity;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.drive.DriveScopes;

/**
 * DriveAuthHelper — Google Sign-In with Drive appDataFolder scope.
 * Call signIn() to launch chooser, handle result in onActivityResult.
 */
public class DriveAuthHelper {

    public static final int RC_SIGN_IN = 9002;

    public static GoogleSignInClient client(Activity activity) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA))
                .build();
        return GoogleSignIn.getClient(activity, gso);
    }

    public static void signIn(Activity activity) {
        activity.startActivityForResult(client(activity).getSignInIntent(), RC_SIGN_IN);
    }

    public static String accountEmailFromResult(Intent data) {
        GoogleSignInAccount acct = GoogleSignIn.getSignedInAccountFromIntent(data).getResult();
        return acct != null ? acct.getEmail() : null;
    }

    public static String lastSignedInEmail(Activity activity) {
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(activity);
        return acct != null ? acct.getEmail() : null;
    }

    public static void signOut(Activity activity, Runnable onDone) {
        client(activity).signOut().addOnCompleteListener(t -> { if (onDone != null) onDone.run(); });
    }
}