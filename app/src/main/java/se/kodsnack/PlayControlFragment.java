package se.kodsnack;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.app.Fragment;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import se.kodsnack.ui.PlayPauseButton;

/**
 * A {@link Fragment} subclass that handles the play control at the bottom
 * of the screen.
 */
public class PlayControlFragment extends Fragment implements PlayerService.PlayerCallback,
        PlayPauseButton.OnClickListener {
    /* Logger tag. */
    private static final String TAG = PlayControlFragment.class.getSimpleName();

    private ImageView       logo;               // Image of playing stream.
    private PlayPauseButton playPauseButton;    // The play/pause button.
    private TextView        titleText;          // Title of the stream.
    private PlayerService   playerService;      // Service actually playing the stream.

    /**
     * The connection to the PlayerService.
     */
    private final ServiceConnection playerConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            playerService = ((PlayerService.LocalBinder) service).getService();
            playerService.registerPlayerCallback(PlayControlFragment.this);
        }

        public void onServiceDisconnected(ComponentName className) {
            // TODO: Some error handling?
            playerService = null;
        }
    };

    public PlayControlFragment() {
        // Required empty public constructor.
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Bind to player service.
        final Activity  activity    = getActivity();
        final Intent    i           = new Intent(activity, PlayerService.class);
        activity.bindService(i, playerConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root       = inflater.inflate(R.layout.fragment_play_controls, container, false);

        // Find views.
        logo            = (ImageView)       root.findViewById(R.id.small_logo);
        playPauseButton = (PlayPauseButton) root.findViewById(R.id.play_pause_button);
        titleText       = (TextView)        root.findViewById(R.id.stream_title);

        // Register OnClickListener.
        playPauseButton.setOnClickListener(this);
        // Initialize play/pause button to be disabled (until we have a stream to play).
        playPauseButton.setEnabled(false);

        return root;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (playerService != null) {
            playerService.unregisterPlayerCallback(this);
            getActivity().unbindService(playerConnection);
        }
    }

    public void updateUI(String title) {
        titleText.setText(title);
        // Replace logo with appsnack's if title says so.
        title = title.toLowerCase();
        int res = title.contains("appsnack") ? R.drawable.appsnack : R.drawable.kodsnack;
        logo.setImageResource(res);
    }

    /* Callbacks from player service. */

    @Override
    public void onPrepared() {
        // Enable play button when prepared.
        updateUI("");
        playPauseButton.setEnabled(true);
    }

    @Override
    public void onPlaying() {
        playPauseButton.setPlaying(true);
    }

    @Override
    public void onPaused() {
        playPauseButton.setPlaying(false);
    }

    @Override
    public void onStopped() {
        playPauseButton.setPlaying(false);
        playPauseButton.setEnabled(false);
    }

    @Override
    public void onBuffering() {
        updateUI(getString(R.string.buffering));
        playPauseButton.setPlaying(false);
    }

    @Override
    public void onError(Throwable t) {
//        playPauseButton.setPlaying(false);
//        playPauseButton.setEnabled(false);
        // TODO: Should probably change this to be a bit smarter.
    }

    @Override
    public void onJsonInfo(JSONObject info) {
        try {
            JSONObject icestats = info.getJSONObject("icestats");
            if (icestats.has("source")) {
                JSONObject source = icestats.getJSONObject("source");

                // Try to find a title or fall back to the server name as title.
                String title = source.has("title") ? source.getString("title")
                                                   : source.getString("server_name");

                // Update UI (replacing logo with appsnack's if title says so).
                updateUI(title);
            }
        } catch (JSONException e) {
            Log.w(TAG, e.toString());
        }
    }

    @Override
    public void onClick(boolean playing) {
        playerService.togglePlaying();
    }
}
