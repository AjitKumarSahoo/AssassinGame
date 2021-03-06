package mobileappdev.assassingame;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LogInActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private static final String TAG = "Assassin_Game";

    public static final String USER_NAME = "UserName";
    public static final String IS_USER_LOGGED_IN = "IsLoggedIn";
    public static final String MY_PREFERENCES = "mobileappdev.assassingame.mypref";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        if (isUserAlreadyLoggedIn()) {
            startActivity(new Intent(LogInActivity.this, MainActivity.class));
        }

        Log.i(TAG, "OnCreate method is invoked.");
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
                // ...
            }
        };

        Button signUpButton = (Button) findViewById(R.id.login_signUpButton);
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LogInActivity.this, SignUpPageActivity.class));
            }
        });

        Button signInButton = (Button) findViewById(R.id.login_loginButton);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditText emailValue = (EditText) findViewById(R.id.login_emailEditText);
                String email = emailValue.getText().toString();

                EditText passwordValue = (EditText) findViewById(R.id.login_passwordEditText);
                String password = passwordValue.getText().toString();

                if (email.length() > 0 && password.length() > 0) {
                    signIn(email, password);
                } else {
                    Toast.makeText(LogInActivity.this, "Sign in failed.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean isUserAlreadyLoggedIn() {
        SharedPreferences sharedpreferences = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        return sharedpreferences.getBoolean(IS_USER_LOGGED_IN, false);
    }

    private void signIn(final String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d("FB", "signInWithEmail:onComplete:" + task.isSuccessful());
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail:failed", task.getException());
                            Toast.makeText(LogInActivity.this, "Sign in failed.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        fetchPlayerNameByEmailID(email);
                        startActivity(new Intent(LogInActivity.this, MainActivity.class));
                        //removes current activity from stack, so that if user presses back button from MainActivity, the app will exit
                        finish();
                    }
                });
    }

    private void persistUserLogInState(String playerName) {
        SharedPreferences sharedpreferences = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sharedpreferences.edit();

        edit.putBoolean(IS_USER_LOGGED_IN, true);
        edit.putString(USER_NAME, playerName);
        edit.apply();
    }

    public void fetchPlayerNameByEmailID(String emailID) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference();

        // Get rid of periods for valid Firebase keys
        String fmtEmail = emailID.replaceAll("\\.", "");

        Query gameQuery = ref.child("emails").equalTo(fmtEmail);

        gameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                persistUserLogInState(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("GAMES", "loadGames:onCancelled", databaseError.toException());
            }
        });

    }



    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }



}
