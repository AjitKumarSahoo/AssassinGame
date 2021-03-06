package mobileappdev.assassingame;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Map;

/**
 * @author: Ajit Ku. Sahoo
 * @Date: 3/13/2017.
 */

public class InvitedPlayersFragment extends Fragment {

    private RecyclerView mInvitedPlayersRecyclerView;
    private IPAdapter mIPAdapter;
//    private DatabaseHandler mDatabaseHandler;
    private List<String> mPlayers;
//    private MyReceiver mMyReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_invited_players, container, false);
        mInvitedPlayersRecyclerView = (RecyclerView) view.findViewById(R.id.player_recycler_view);
        mInvitedPlayersRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
//        mDatabaseHandler = new DatabaseHandler(this.getActivity(), Game.getInstance().getGameName());
//        mPlayers = mDatabaseHandler.getAllPlayers();
//        mMyReceiver = new MyReceiver();
        updateUI();
        return view;
    }

    private void updateUI() {
//        mPlayers = mDatabaseHandler.getAllPlayers();
        mPlayers = Game.getInstance().getAllPlayerNames();
        mIPAdapter = new IPAdapter(mPlayers);
        mInvitedPlayersRecyclerView.setAdapter(mIPAdapter);
//        updateInvitationStatusOnGUI();
    }

    private void updateInvitationStatusOnGUI(TextView textView) {
        Map<String, Player> map = Game.getInstance().getName2PlayerMap();
        Player player = map.get(textView.getText().toString());
        if (player == null)
            return;
        InvitationStatus invitationStatus = player.getInvitationStatus();
        updateInvitationStatusOnGUI(textView, invitationStatus);
    }

    private void updateInvitationStatusOnGUI(TextView textView, InvitationStatus invitationStatus) {
        switch (invitationStatus) {
            case INVITED:
                textView.setTextColor(Color.BLUE);
                break;
            case ACCEPTED:
                textView.setTextColor(Color.GREEN);
                break;
            case UNDEFINED:
                textView.setTextColor(Color.BLACK);
                break;
            case DECLINED:
                textView.setTextColor(Color.RED);
                break;
        }
    }

    public void update() {
        updateUI();
    }



    private class IPHolder extends RecyclerView.ViewHolder {
        private TextView mNameTextView;
        private Button mRemoveButton;

        IPHolder(View itemView) {
            super(itemView);
            /*itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getActivity(), "Oops !!", Toast.LENGTH_LONG).show();
                }
            });*/
            mNameTextView = (TextView) itemView.findViewById(R.id.player_name);
            mRemoveButton = (Button) itemView.findViewById(R.id.remove_button);
            mRemoveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removePlayer(getAdapterPosition());
                }
            });
        }
    }

    private void removePlayer(int position) {
        String playerName = mPlayers.get(position);
        //we don't need to handle such temporary add/remove operation to database. we will only add
        //the players to the database when game is created. check GameBoardActivity#createGame()
//        DatabaseHandler databaseHandler = new DatabaseHandler(this.getActivity(), Game.getInstance().getGameName());
//        databaseHandler.deletePlayer(playerName);
        mPlayers.remove(position);
        Game.getInstance().removePlayer(playerName);
        updateUI();
    }

    private class IPAdapter extends RecyclerView.Adapter<IPHolder> {
        private List<String> mPlayers;

        IPAdapter(List<String> players) {
            mPlayers = players;
        }

        @Override
        public IPHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.fragment_player_selection, parent, false);
            return new IPHolder(view);
        }

        @Override
        public void onBindViewHolder(IPHolder holder, int position) {
            String player = mPlayers.get(position);
            holder.mNameTextView.setText(player);
            holder.mRemoveButton.setEnabled(true);
            updateInvitationStatusOnGUI(holder.mNameTextView);

        }

        @Override
        public int getItemCount() {
            return mPlayers.size();
        }
    }

    public int getNoOfPlayersInGame() {
        return mPlayers.size();
    }

    @Override
    public void onResume() {
        super.onResume();
//        getActivity().registerReceiver(mMyReceiver, new IntentFilter(BroadcastHelper.INVITE_RESPONSE));
    }

    @Override
    public void onPause() {
        super.onPause();
//        getActivity().unregisterReceiver(mMyReceiver);
    }

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            Log.i("Receiver", "Invitation Response received. Action: " + action);

            //TODO: 3/18/2017 need to broadcast a msg with INVITE_RESPONSE action when a response is received through Firebase
            if (action.equals(BroadcastHelper.INVITE_RESPONSE)) {
                String userName = intent.getExtras().getString(BroadcastHelper.PLAYER_NAME);
                String response = intent.getExtras().getString(BroadcastHelper.INVITATION_RESPONSE);
//                mDatabaseHandler.updatePlayerInviationStatus(userName, InvitationStatus.getStatusFrom(response));
                Game.getInstance().getName2PlayerMap().get(userName).setInvitationStatus(InvitationStatus.getStatusFrom(response));
                for (int i = 0; i < mIPAdapter.getItemCount(); i++) {
                    IPHolder viewHolder = (IPHolder)mInvitedPlayersRecyclerView.findViewHolderForLayoutPosition(i);
                    if (viewHolder.mNameTextView.getText().equals(userName)) {
                        Log.d("IPF", "Invitation response detected; updating UI");
                        updateInvitationStatusOnGUI(viewHolder.mNameTextView, InvitationStatus.getStatusFrom(response));
                        break;
                    }
                }
            }
        }
    }


}
