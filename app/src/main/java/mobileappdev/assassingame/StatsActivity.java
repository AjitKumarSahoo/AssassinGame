package mobileappdev.assassingame;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

/**
 * Created by kennyschmitt on 3/24/17.
 */

public class StatsActivity extends AppCompatActivity {

    private TextView mUsernameTextView;
    private TextView mWinsTextView;
    private TextView mLossesTextView;
    private String mUsername;
    private HashMap<String, Integer> stats = new HashMap<String, Integer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            mUsername = user.getDisplayName();
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference statsRef = database.getReference("users/" + mUsername + "/stats");
            statsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        stats.put(snapshot.getKey(), Integer.parseInt(snapshot.getValue().toString()));
                        Log.d("Firebase", snapshot.getKey() + snapshot.toString());
                    }

                    // This way, any time we have an update in this data, the view will be modified
                    // to reflect those changes. Test it out yourself in the Firebase console!

                    Integer wins = stats.get("wins");
                    if (wins != null) {
                        mWinsTextView = (TextView) findViewById(R.id.num_wins_view);
                        mWinsTextView.setText(String.valueOf(wins));
                    }

                    Integer losses = stats.get("losses");
                    if (wins != null) {
                        mLossesTextView = (TextView) findViewById(R.id.num_losses_view);
                        mLossesTextView.setText(String.valueOf(losses));
                    }

                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w("StatsActivity", "Failed to read value from Firebase.", error.toException());
                }
            });
        }

        mUsernameTextView = (TextView) findViewById(R.id.user_name_view);

        mUsernameTextView.setText(mUsername);
    }

    @NonNull
    private String getMyUserName() {
        SharedPreferences sharedPreferences = getSharedPreferences(LogInActivity.MY_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getString(LogInActivity.USER_NAME, "undefined");
    }

}
