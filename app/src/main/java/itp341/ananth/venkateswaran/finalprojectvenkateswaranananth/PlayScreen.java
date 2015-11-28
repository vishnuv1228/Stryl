package itp341.ananth.venkateswaran.finalprojectvenkateswaranananth;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;
import com.spotify.sdk.android.player.Spotify;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.Response;

public class PlayScreen extends AppCompatActivity implements PlayerNotificationCallback, ConnectionStateCallback {

    private static String SPOTIFY_ACCESS_TOKEN;
    private static String SPOTIFY_CLIENT_ID;
    private static final String TAG = "PlayScreen";
    private Player mPlayer;
    private ArrayList<String> tracksInPlaylist;
    private ArrayList<Track> trackObjs;
    private SpotifyService spotify;
    TextView street;
    Button previous;
    Button play;
    Button pause;
    Button next;
    TextView songTitle;
    TextView albumTitle;
    TextView artistTitle;
    Button playlist;
    ImageView albumArt;

    private String TRACK_NAME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_screen);

        tracksInPlaylist = new ArrayList<>();
        trackObjs = new ArrayList<>();

        SPOTIFY_ACCESS_TOKEN = getIntent().getStringExtra("ACCESS_TOKEN");
        final String SPOTIFY_USER_ID = getIntent().getStringExtra("USER_ID");
        String SPOTIFY_PLAYLIST_ID = getIntent().getStringExtra("PLAYLIST_ID");
        SPOTIFY_CLIENT_ID = getIntent().getStringExtra("CLIENT_ID");
        String STREET_NAME = getIntent().getStringExtra("STREET");

        // Grab an instance of spotify
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(SpotifyApi.SPOTIFY_WEB_API_ENDPOINT)
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("Authorization", "Bearer " + SPOTIFY_ACCESS_TOKEN);
                    }
                })
                .build();
        spotify = restAdapter.create(SpotifyService.class);

        street = (TextView) findViewById(R.id.streetView);
        street.append(" " + STREET_NAME);

        albumArt = (ImageView) findViewById(R.id.albumArt);
        previous = (Button) findViewById(R.id.prevBtn);
        play = (Button) findViewById(R.id.playBtn);
        pause = (Button) findViewById(R.id.pauseBtn);
        next = (Button) findViewById(R.id.nextBtn);

        songTitle = (TextView) findViewById(R.id.songName);
        albumTitle = (TextView) findViewById(R.id.albumName);
        artistTitle = (TextView) findViewById(R.id.artistName);

        playlist = (Button) findViewById(R.id.viewPlaylistBtn);


    // Get current playlist and add into list of tracks
        spotify.getPlaylist(SPOTIFY_USER_ID, SPOTIFY_PLAYLIST_ID, new SpotifyCallback<Playlist>() {

            @Override
            public void failure(SpotifyError spotifyError) {
                Log.d(TAG, "Error in getting playlist: " + spotifyError);
               // Toast.makeText(getApplicationContext(), "Could not query the spotify servers", Toast.LENGTH_LONG);
                finish();
            }

            @Override
            public void success(Playlist playlist, Response response) {
                Log.d(TAG, "Success -  got playlist");
                for (PlaylistTrack track : playlist.tracks.items) {
                    tracksInPlaylist.add(track.track.uri);
                    trackObjs.add(track.track);
                }
                Config playerConfig = new Config(getApplicationContext(), SPOTIFY_ACCESS_TOKEN, SPOTIFY_CLIENT_ID);

                mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(final Player player) {
                        player.getPlayerState(new PlayerStateCallback() {
                            @Override
                            public void onPlayerState(PlayerState playerState) {
                                if (!playerState.playing)  {

                                    mPlayer.addConnectionStateCallback(PlayScreen.this);
                                    mPlayer.addPlayerNotificationCallback(PlayScreen.this);
                                    mPlayer.play(tracksInPlaylist);
                                }
                                else {



                                    for(int i = 0; i < tracksInPlaylist.size(); i++) {
                                        if (tracksInPlaylist.get(i).equals(playerState.trackUri)) {
                                            // Update titles
                                            songTitle.setText(trackObjs.get(i).name);
                                            albumTitle.setText(trackObjs.get(i).album.name);
                                            artistTitle.setText(trackObjs.get(i).artists.get(0).name);
                                            // Update album art
                                            Image image = trackObjs.get(i).album.images.get(0);
                                            Picasso.with(getApplicationContext()).load(image.url).into(albumArt);

                                            TRACK_NAME = trackObjs.get(i).name;
                                        }

                                    }
                                }
                            }
                        });



                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e(TAG, "Could not initialize player: " + throwable.getMessage());
                    }
                });


            }
        });



        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.skipToPrevious();
            }
        });
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.resume();
            }
        });
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.pause();
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.skipToNext();
            }
        });

        // Playlist button listener
        playlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), PlaylistScreen.class);
                i.putExtra("USER_ID", SPOTIFY_USER_ID);
                i.putExtra("ACCESS_TOKEN", SPOTIFY_ACCESS_TOKEN);
                startActivity(i);
            }
        });


    }
    @Override
    public void onLoggedIn() {
        Log.d(TAG, "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d(TAG, "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d(TAG, "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d(TAG, "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d(TAG, "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d(TAG, "Playback event received: " + eventType.name());
        if (eventType.equals(EventType.TRACK_CHANGED)) {
            for(int i = 0; i < tracksInPlaylist.size(); i++) {
                if (tracksInPlaylist.get(i).equals(playerState.trackUri)) {
                    // Update titles
                    songTitle.setText(trackObjs.get(i).name);
                    albumTitle.setText(trackObjs.get(i).album.name);
                    artistTitle.setText(trackObjs.get(i).artists.get(0).name);
                    // Update album art
                    Image image = trackObjs.get(i).album.images.get(0);
                    Picasso.with(getApplicationContext()).load(image.url).into(albumArt);

                    TRACK_NAME = trackObjs.get(i).name;
                }

            }
        }

    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d(TAG, "Playback error received: " + errorType.name());
        switch (errorType) {
            // Handle error type as necessary
            default:
                break;
        }
    }
    public void finish() {
        Intent intent = new Intent();
        intent.putExtra("TRACK_NAME", TRACK_NAME);
        this.setResult(Activity.RESULT_OK, intent);
        Spotify.destroyPlayer(this);
        super.finish();
    }
    @Override
    protected void onDestroy() {
        // VERY IMPORTANT! This must always be called or else you will leak resources
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }
}
