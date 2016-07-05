package com.example.jaineek.meeplemain;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.phenotype.Configuration;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;

public class LoginActivity extends AppCompatActivity {

    public static final String TAG = "LoginActivity";

    private TextView mDontHaveAccountClickable;
    private TextView mForgotPasswordClickable;
    private Button mLoginButton;
    private EditText mUsername;
    private EditText mEmailAddress;
    private EditText mPassword;
    private Context mContext;


    // Declaring Firebase Variables
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        mDontHaveAccountClickable = (TextView) findViewById(R.id.login_dont_have_account_clickable);

        // Setting onClickListener for create new account clickable
        mDontHaveAccountClickable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //creating an intent to change to register screen
                Intent changeToRegisterScreen = new Intent(v.getContext(), MeepleMain.class);
                startActivity(changeToRegisterScreen);
                finish();
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

                            // If sign in fails, display a message to the user. If sign in succeeds
                            // the auth state listener will be notified and logic to handle the
                            // signed in user can be handled in the listener.
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "signInWithEmail", task.getException());
                                Toast.makeText(LoginActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                // Login successful!
                                Toast.makeText(mContext, "Username: " +
                                        mAuth.getCurrentUser().getDisplayName() + " " +
                                        mAuth.getCurrentUser().getEmail(),
                                        Toast.LENGTH_SHORT).show();
                                // TODO: change this to real login
//                                Intent changeToRegister = new Intent(LoginActivity.this,
//                                          MeepleMain.class);
//                                startActivity(changeToRegister);
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
    

}
