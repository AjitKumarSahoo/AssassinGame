package mobileappdev.assassingame;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Ajit Ku. Sahoo
 * @Date: 3/13/2017
 */

public class SearchPlayerFragment extends Fragment {

    private SearchOpListener mListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (SearchOpListener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        return super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_search_player, container, false);
        Button searchButton = (Button)view.findViewById(R.id.search_button);
        final EditText searchBoxValue = (EditText) view.findViewById(R.id.search_player_box);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetSearchPlayer();
                String player = searchBoxValue.getText().toString();
                if (player.equals(""))
                    return;
                Log.i("TESTING", "WE ARE ENTERING THE GETPLAYERLIST() FUNCITON");
                searchPlayer(player, player.contains("@"));
                searchBoxValue.setText("");
            }
        });
        return view;
    }

    private void resetSearchPlayer() {
        Game.getInstance().setSearchedPlayer(null);
    }

    private void searchPlayer(String playerInfo, boolean byEmail) {
        List<Player> searchedPlayers = new ArrayList<>();
        if (byEmail) {
            Player player = FirebaseHelper.getPlayerByEmailID(playerInfo);
            searchedPlayers = new ArrayList<>();
            searchedPlayers.add(player);
        } else {
            Log.i("PLAYERS", searchedPlayers.toString());
            searchedPlayers = FirebaseHelper.getPlayerListContainingUserName(playerInfo);
            Log.i("PLAYERS", searchedPlayers.toString());
        }

        Game.getInstance().setSearchedPlayer(searchedPlayers);
        mListener.updateSearchResult();
    }
}