package com.example.jaineek.meeplemain;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jaineek.meeplemain.model.Post;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Date;


public class LoginActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = "LoginActivity";
    public static final String KEY_EXTRA_LOCATION_LOGIN
            = "com.example.jaineek.meeplemain.extra_tag_location_login";

    private TextView mDontHaveAccountClickable;
    private TextView mForgotPasswordClickable;
    private Button mLoginButton;
    private SignInButton mGoogleSignInButton;
    private EditText mEmailAddress;
    private EditText mPassword;
    private ProgressBar mProgressBar;
    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;


    // Location variables
    Location mLastLocation;
    private GoogleApiClient mGoogleApiClientLocation;


    //TODO: remove test button once done
    private Button mTestButton;

    //Final static variables
    private static final int RC_SIGN_IN = 9001;


    // Declaring Firebase Variables
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabaseReference;
    private DatabaseReference mPostsReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSharedPreferences = getApplicationContext().getSharedPreferences("preferences", MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();
        // Check for dark theme
        if (mSharedPreferences.getBoolean("key_change_theme", false)) {
            setTheme(R.style.DarkAppTheme);
        } else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
        mAuth.addAuthStateListener(mAuthListener);

        mProgressBar = (ProgressBar) findViewById(R.id.login_progress_bar);
        mDontHaveAccountClickable = (TextView) findViewById(R.id.login_dont_have_account_clickable);

        // Setting onClickListener for create new account clickable
        mDontHaveAccountClickable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //creating an intent to change to register screen
                Intent changeToRegisterScreen = new Intent(v.getContext(), MeepleMain.class);
                startActivity(changeToRegisterScreen);
            }
        });


        mForgotPasswordClickable = (TextView) findViewById(R.id.login_forgot_password_clickable);

        //Setting OnClickListener for Forgot Password Clickable
        mForgotPasswordClickable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toForgotPasswordActivity = new Intent(v.getContext(),
                        ForgotPasswordActivity.class);
                startActivity(toForgotPasswordActivity);
            }
        });


        //Initializing other variables
        mEmailAddress = (EditText) findViewById(R.id.login_email_editText);
        mPassword = (EditText) findViewById(R.id.login_password_editText);

        // Wiring Login button
        mLoginButton = (Button) findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Authenticate login credentials
                mContext = v.getContext();
                authenticateUser();

            }
        });

        mGoogleSignInButton = (SignInButton) findViewById(R.id.login_google_signin_button);
        mGoogleSignInButton.setSize(SignInButton.SIZE_STANDARD);

        //Setting OnClickListener for Google Signin Button
        mGoogleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });


        //Requesting Google Sign in information
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.login_id_token))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        //Test button to login. Remove once done
        mTestButton = (Button) findViewById(R.id.login_test_button);
        mTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signInWithEmailAndPassword("test1@yahoo.com", "test1234");
                mProgressBar.setVisibility(View.VISIBLE);
                loginToFeedActivity();
            }
        });

        // Setup User location services
        setupGoogleLocationServices();

        // Constantly update the database with each login
        purgeDatabaseOfOldPosts();

    }

    public void purgeDatabaseOfOldPosts() {
        // Deletes old Posts from Firebase Database

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mPostsReference = mDatabaseReference.child(FeedActivity.PATH_TO_POSTS);

        mPostsReference.orderByChild("timestamp").addChildEventListener(
                new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                        Post post = dataSnapshot.getValue(Post.class);
                        String postKey = dataSnapshot.getKey();

                        // Get current date at midnight
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.HOUR_OF_DAY, 0);
                        c.set(Calendar.MINUTE, 0);
                        c.set(Calendar.SECOND, 0);
                        Date currentDate = c.getTime();

                        if (post.eventDate.compareTo(currentDate) < 0) {
                            // If post is in the past, delete it from Posts and GeoFire
                            mPostsReference.child(postKey).setValue(null);
                            mDatabaseReference.child(FeedActivity.PATH_TO_GEOFIRE).child(postKey)
                                    .setValue(null);
                        }
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                }
        );
    }


    public void authenticateUser() {
        // Attempts to login user with entered Email Address and Password
        String email = mEmailAddress.getText().toString();
        String password = mPassword.getText().toString();

        try {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                            String email = mEmailAddress.getText().toString();
                            String password = mPassword.getText().toString();

                            // If sign in fails, display a message to the user.
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "signInWithEmail", task.getException());
                                Toast.makeText(LoginActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                // Login successful! Go to FeedActivity
                                Toast.makeText(mContext, "Username: " +
                                                mAuth.getCurrentUser().getDisplayName() + " " +
                                                mAuth.getCurrentUser().getEmail(),
                                        Toast.LENGTH_SHORT).show();

                                loginToFeedActivity();
                            }
                        }
                    });

        } catch (Exception e) {
            Log.d(TAG, "Login tried with null entries");
            // If Email Address and Password are null entries
            if (TextUtils.isEmpty(email)) {
                mEmailAddress.setError(getString(R.string.error_field_required));
            }

            if (TextUtils.isEmpty(password)) {
                mPassword.setError(getString(R.string.error_field_required));
            }
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Google Sign in Intent
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            }
        }
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // The connection failed
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                        } else {
                            // Login successful! Go to FeedActivity
                            loginToFeedActivity();
                        }
                    }
                });
    }

    private void loginToFeedActivity() {
        // If successful login, go to FeedActivity
        Intent changeToFeedActivity = new Intent(LoginActivity.this,
                FeedActivity.class);
        // Send the user location to FeedActivity
        changeToFeedActivity.putExtra(KEY_EXTRA_LOCATION_LOGIN, mLastLocation);
        startActivity(changeToFeedActivity);
        finish();

    }
    private void setUsernameAlertDialog() {
        if (mAuth.getCurrentUser().getDisplayName() == null) {
            new AlertDialog.Builder(LoginActivity.this)
                    .setTitle("You don't have a username!")
                    .setMessage("You don't have a username! Set one now!")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with delete
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        recreate();
    }

    // Location Services
    private void setupGoogleLocationServices() {
        if (mGoogleApiClientLocation == null) {
            mGoogleApiClientLocation = new GoogleApiClient.Builder(LoginActivity.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Set the last location
        try {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClientLocation);
                if (lastLocation != null) {
                    // Use the last location in some way
                    mLastLocation = lastLocation;
                }
        } catch (SecurityException e) {
            // Indicate that location services are not allowed at this time
            Toast.makeText(LoginActivity.this, getString(R.string.error_location_not_supported),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        try {
            Location lastLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClientLocation);
            if (lastLocation != null) {
                // Use the last location in some way
                mLastLocation = lastLocation;
            }
        } catch (SecurityException e) {
            // Indicate that location services are not allowed at this time
            Toast.makeText(LoginActivity.this, getString(R.string.error_location_not_supported),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnectionSuspended(int connectionStuff) {
        // Left blank
    }

    @Override
    protected void onStart() {
        // Connect to location services
        super.onStart();
        mGoogleApiClientLocation.connect();
    }

    @Override
    protected void onStop() {
        // Disconnect to location services
        super.onStop();
        mGoogleApiClientLocation.disconnect();
    }

//    @Override
//    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
//        super.onSaveInstanceState(outState, outPersistentState);
//        mEmailAddress = (EditText) findViewById(R.id.login_email_editText);
//        String savedEmail = mEmailAddress.getText().toString();
//        outState.putString("savedEmail", savedEmail);
//    }
//
//    @Override
//    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
//        super.onRestoreInstanceState(savedInstanceState, persistentState);
//        mEmailAddress = (EditText) findViewById(R.id.login_email_editText);
//        String savedEmail = savedInstanceState.getString("savedEmail", null);
//        if (savedEmail != null) {
//            mEmailAddress.setText(savedEmail);
//        }
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        mEmailAddress = (EditText) findViewById(R.id.login_email_editText);
//        String savedEmail = mEmailAddress.getText().toString();
//        mEditor.putString("savedEmail", savedEmail);
//        mEditor.apply();
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        mEmailAddress = (EditText) findViewById(R.id.login_email_editText);
//        String savedEmail = mSharedPreferences.getString("savedEmail", null);
//        if (savedEmail != null) {
//            mEmailAddress.setText(savedEmail);
//        }
//    }
}
