package mobileappdev.assassingame;

import android.location.Location;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Ajit Ku. Sahoo
 * @Date: 3/17/2017
 */

public class FirebaseHelper {

    /**
     * The primary logic that is fired when a user says "Create game." All sorts of things are
     * handled here; specifically, the backend creates a new instance in our "games/" object and
     * fills it with all of the necessary information.
     * @param newGame
     */
    public static void createGame(Game newGame) {

        /*
         * This method is going to have a bit more logic; we will need to format and include the
         * data in a cleaner way and also send notifications to users who were invited to the game.
         * TODO: Integreate sendInvite() calls and so on.
         */

        String gameReference = "games/" + newGame.getGameName();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference typeRef = database.getReference(gameReference + "/type");
        DatabaseReference creatorRef = database.getReference(gameReference + "/creator");
        DatabaseReference playersRef = database.getReference(gameReference + "/players");
        //TODO:SAM: add the admin to the players list of the game. use 'newGame.getGameAdmin()' to get adminName
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String creatorName = user.getDisplayName();
            creatorRef.setValue(creatorName);
        }

        // Public or private
        if (newGame.isPublic()) {
            typeRef.setValue("public");
        } else {
            typeRef.setValue("private");
        }

        // Adding player names to our newly-created game!
        DatabaseReference newPlayerRef = playersRef.push();
        playersRef.setValue(newGame.getSearchedPlayer());
    }

    /**
     * This function tells us whether a particular game is public or private. This will determine
     * whether it shows up in game search results for our users.
     * @param gameName
     * @return boolean value indicating whether or not the game is public (true) or private (false).
     */
    public static boolean isGamePublic(String gameName) {

        /*
         * Because the game type at this point is already determined, we don't need to waste
         * resources by listening for changes. The reference method addListenerForSingleValueEvent()
         * handles this for us, and we use a StringBuffer to store the result of that query for use
         * in this function. A similar approach is used in other "get" methods so that I don't have
         * to change the FirebaseHelper class.
         */

        // Establish reference to Firebase based on gameName attribute
        String gameTypeReference = "games/" + gameName + "/type";
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference(gameTypeReference);
        final StringBuffer result = new StringBuffer();

        // Listen for single value then destroy listener
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Boolean value = (Boolean) dataSnapshot.getValue();
                result.append(value);
                Log.d("FIREBASE HELPER", "Value is: " + value);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("FIREBASE HELPER", "Failed to read value.", databaseError.toException());
            }
        });

        // Implementing the hacky workaround
        Boolean isPublic = result.toString().equals("public");

        // Resetting string buffer
        result.setLength(0);

        return isPublic;
    }

    /**
     * This takes a particular game and makes it public to all users of the app.
     * @param gameName: game to be made public.
     */
    public static void setGamePublic(String gameName) {

        // Establishing reference to Firebase based on gameName attribute
        String gameTypeReference = "games/" + gameName + "/type";
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference(gameTypeReference);

        // Much simpler than isGamePublic, right?
        ref.setValue("public");
    }

    /**
     * Returns all game names from our Firebase backend.
     * @return a list of strings referring to unique game names.
     */
    public static List<String> getAllGameNames() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference();
        Query gameQuery = ref.child("games");
        final List<String> gameNames = new ArrayList<String>();

        gameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot gameSnapshot: dataSnapshot.getChildren()) {
                    gameNames.add(gameSnapshot.getKey()); // Because game names are used as keys
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w("FirebaseHelper", "loadGames:onCancelled", databaseError.toException());
                // ...
            }
        });

        return gameNames;
    }


    public static void sendInvite(String player, String admin) {
        // TODO: 3/17/2017 broadcast the msg
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference inviteRef = database.getReference("users/" + player + "/invites");
        DatabaseReference newInviteRef = inviteRef.push();

        newInviteRef.setValue(admin + " @ " + Long.toString(System.currentTimeMillis()));
    }

    /**
     * Find a player based on their email address. Note: the result must be an exact match in
     * order to protect the privacy of our users. ALSO NOTE: We are doing some silly formatting
     * of the email because Firebase keys cannot contain certain characters (like '.').
     *
     * TODO: SAM Push formatted email to "emails/" to facilitate search. See SignUpPage.
     *
     * @param emailID: email address of player we are looking for
     * @return Newly-created Player object referring to queried user
     */
    public static Player getPlayerByEmailID(String emailID) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference();

        // Get rid of periods for valid Firebase keys
        String fmtEmail = emailID.replaceAll("\\.", "");
        final String originalEmail = emailID;

        Query gameQuery = ref.child("emails").equalTo(fmtEmail);
        final Player playerQueryResult = Player.getDummyPlayer();

        gameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot userSnapshot: dataSnapshot.getChildren()) {
                    playerQueryResult.setEmailID(originalEmail);
                    playerQueryResult.setName(userSnapshot.getValue().toString());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w("GAMES", "loadGames:onCancelled", databaseError.toException());
                // ...
            }
        });

        return playerQueryResult;
    }


    /**
     * Find a player based on their username. Note: unlike email addresses, this does not have
     * to be an exact match. It would be fairly easy to change that though.
     *
     * @param userName: username snippet of player(s) we are looking for
     * @return List<Player>: Newly-created Player list containing the userName string
     */
    public static List<Player> getPlayerListContainingUserName(String userName) {

        /*
         * Our methodology here is to retrieve all names from Firebase and then search them
         * one by one. This doesn't really scale well but it provides us with SQL-like functionality
         * that will improve the UX.
         */

        List<Player> playerList = new ArrayList<>();    // To store our player objects.
        List<String> allNames = getAllPlayerNames();    // Getting all the names from Firebase.

        for (int i = 0; i < allNames.size(); i++) {
            if (allNames.get(i).contains(userName)) {
                // Here we are creating "default" player objects because we only need their names.
                Player newPlayerResult = new Player(allNames.get(i), "", null, false);
                playerList.add(newPlayerResult);
            }
        }

        return playerList;
    }


    /**
     * Returns a list of all player names for the purpose of searching.
     * @return List<String> containing all user names.
     */
    public static List<String> getAllPlayerNames() {
        final List<String> playerNames = new ArrayList<>();
        String usersReference = "users/";
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference(usersReference);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Log.i("FirebaseHelper", "getAllPlayerNames:onDataChange:"
                //     + dataSnapshot.getChildrenCount());

                // Push the names to our result List.
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    playerNames.add(snapshot.getKey());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i("FirebaseHelper", "getAllPlayerNames:onCancelled");
            }
        });

        return playerNames;
    }

    public static List<String> getAllPlayerNames(String gameName) {
        // TODO: 3/17/2017 return actual list
        // TODO: 3/20/2017 What do we need this for?
        return new ArrayList<>();
    }

    /**
     * Pushes your current location to the game's backend
     * @param location
     * @param gameName
     * @param myself
     */
    public static void sendLocation(Location location, String gameName, String myself) {
        // TODO: 3/18/2017 need to send location to all of the players of the given game
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myLocationRef = database.getReference("games/" + gameName + "/" + myself + "/location");

        String fmtLocation = Double.toString(location.getLatitude()) + " "
                           + Double.toString(location.getLongitude());

        myLocationRef.setValue(fmtLocation);

    }

    // TODO: I added the gameName parameter to this method because we will need it.
    public static void sendRejectionResponse(String gameName, String sender) {
        // TODO: 3/18/2017 need to send out a reject message through firebase to the sender

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference inviteRef = database.getReference("games/" + gameName + "/invites/" + sender);
        DatabaseReference inviteDeclineRef = inviteRef.push();

        inviteDeclineRef.setValue("declined");
    }

    /**
     * @param fromPlayer: The user that accepted the invitation.
     * @param gameName: The game that the user has accepted the invitation for
     */
    public static void sendAcceptResponse(String fromPlayer, String gameName) {
        // TODO: 3/18/2017 add the player to the game in firebase
        // TODO: 3/18/2017 send acceptance response to the admin

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference inviteRef = database.getReference("games/" + gameName + "/invites/" + fromPlayer);
        DatabaseReference inviteAcceptRef = inviteRef.push();

        inviteAcceptRef.setValue("accepted");
    }

    public static void sendGameStartMessage(String gameName) {
        // TODO: 3/18/2017 1. set status of game as started. use enum GameStatus
        // TODO: 3/18/2017 2. need to send a message to all the players of this game that game is started

        // On the Firebase backend we have to store the game status as a string.

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference gameStatusRef = database.getReference("games/" + gameName + "/status");


        gameStatusRef.setValue("started");
    }

    /**
     * Retrieves the game status as a string from the Firebase realtime database. Converts this
     * result to a GameStatus object.
     * @param gameName: game whose status we are checking
     * @return GameStatus object corresponding to queried game
     */
    public static GameStatus getGameStatus(String gameName) {
        String gameTypeReference = "games/" + gameName + "/status";
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference(gameTypeReference);
        final StringBuffer status = new StringBuffer();

        // Listen for single value then destroy listener
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String queriedGameStatus = (String) dataSnapshot.getValue();
                status.append(queriedGameStatus);
                Log.d("FIREBASE HELPER", "Value is: " + queriedGameStatus);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("FIREBASE HELPER", "Failed to read value.", databaseError.toException());
            }
        });

        return GameStatus.getCharacterFrom(status.toString());
    }

    /**
     * Helper method to determine if the game has started.
     * @param gameName: game in question
     * @return whether or not game has started
     */
    public static boolean isGameStarted(String gameName) {
        return GameStatus.STARTED.equals(getGameStatus(gameName));
    }

    /**
     * Helper method to determine if the game has finished.
     * @param gameName: game in question
     * @return whether or not game has finished
     */
    public static boolean isGameFinished(String gameName) {
        return GameStatus.FINISHED.equals(getGameStatus(gameName));
    }

    /**
     * This returns all players of a game in a nice orderly fashion. It expects the fields
     * /character and /isAlive to be filled with appropriate string and boolean values.
     * @param gameName: the game we want the players for
     * @return a map containing all the game player info
     */
    public static Map<String, Player> getAllPlayers(String gameName) {
        //TODO:Sam: return all the players (playerName --> Player object) from firebase database for the given GameName

        final Map<String, Player> playerMap = new HashMap<String, Player>();

        String gamePlayerReference = "games/" + gameName + "/players";
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference(gamePlayerReference);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    playerMap.put(snapshot.getKey(),
                            new Player(snapshot.getKey(),
                                    "test@doWeNeedThisInfo.com",
                                    GameCharacter.getCharacterFrom(snapshot.child("character").getValue().toString()),
                                    Boolean.parseBoolean(snapshot.child("isAlive").getValue().toString())));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("FirebaseHelper", "getAllPlayers:onCancelled");
            }
        });

        return playerMap;
    }

    public static void updatePlayerStatus(String gameName, String playerName, PlayerStatus status, boolean shouldUpdateCiviliansCounter) {
        //TODO:Sam: update the player to be dead/alive/left
        //TODO:Sam: decrease alive civlians counter only if the boolean flag is true,
        // which represents no of civilians left (excluding detective). when it becomes zero, Assassin wins the game.

        String strStatus = status.toString();
        String gamePlayerReference = "games/" + gameName + "/players/" + playerName + "/status";
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference(gamePlayerReference);
        ref.setValue(strStatus);

        if (shouldUpdateCiviliansCounter) {
            // I am making the assumption that we would want to decrement here.
            // TODO: Would there ever be a scenario we'd increment it?
            decreaseNoOfAliveCiviliansBy1(gameName);
        }
    }

    public static int getNoOfAliveCivilians(String gameName) {
        String gameReference = "games/" + gameName;
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference aliveRef = database.getReference(gameReference + "/alive");

        final StringBuffer aliveCivilians = new StringBuffer();

        aliveRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                aliveCivilians.append(dataSnapshot.getValue());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("FirebaseHelper", "getNoOfAliveCivilians:onCancelled");
            }
        });

        return Integer.parseInt(aliveCivilians.toString());
    }

    public static void initializeNoOfAliveCivilians(final String gameName) {
        String gameReference = "games/" + gameName;
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference aliveRef = database.getReference(gameReference + "/alive");
        DatabaseReference playersRef = database.getReference(gameReference + "/players");

        aliveRef.setValue(0);

        playersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        if (snapshot.child(snapshot.getKey()).child("role").toString().equals("civilian")) {
                            increaseNoOfAliveCiviliansBy1(gameName);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("FIREBASE HELPER", "Failed to read value.", databaseError.toException());
            }
        });
    }

    /**
     * This is a helper function that allows us to update the backend as new users join a game.
     * @param gameName: the game to be updated
     */
    public static void increaseNoOfAliveCiviliansBy1(String gameName) {
        String gameReference = "games/" + gameName;
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference aliveRef = database.getReference(gameReference + "/alive");
        int numberAlive = getNoOfAliveCivilians(gameName);

        // That was easy!
        aliveRef.setValue(numberAlive + 1);
    }

    public static void decreaseNoOfAliveCiviliansBy1(String gameName) {
        String gameReference = "games/" + gameName;
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference aliveRef = database.getReference(gameReference + "/alive");
        int numberAlive = getNoOfAliveCivilians(gameName);

        // That was easy!
        aliveRef.setValue(numberAlive - 1);
    }

    public static void updateGameStatus(String gameName, boolean assassinWon, String description) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference gameStatusRef = database.getReference("games/" + gameName + "/status");
        gameStatusRef.setValue("finished");

        DatabaseReference resultRef = database.getReference("games/" + gameName + "/result");

        if (assassinWon) {
            resultRef.setValue("The assassin won!");
        } else {
            resultRef.setValue("The civillians won!");
        }
    }

    public static void newPlayerAddedUp(String userName, String gameName) {
        //TODO:Sam: send this message to everyone in the game
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference gamePlayersRef = database.getReference("games/" + gameName + "/players");
        DatabaseReference newGamePlayerRef = gamePlayersRef.push();

        newGamePlayerRef.push().setValue(userName);
        // TODO: On the listener side we need to react to this event (broadcast msg to others)
    }

    public static void sendPlayerNotLoggedInResponse(String fromPlayer, String toAdmin) {
        //TODO:Sam: send this message to admin with info about fromPlayer
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference msgRef = database.getReference("users/" + toAdmin + "/messages");
        DatabaseReference newMsgRef = msgRef.push();

        newMsgRef.setValue(fromPlayer + " not logged in");
    }

    public static void updateCharactersOfPlayers(String gameName, String assassin, String detective,
                                                 String doctor, List<String> civilians) {
        //TODO:SAM: get the players of the game with gameName and update the character of the players
        //assassin argument is the name of the assassin. similarly detective and doctor
    }
}
