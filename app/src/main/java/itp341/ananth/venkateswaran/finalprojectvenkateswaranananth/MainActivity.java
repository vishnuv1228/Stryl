package itp341.ananth.venkateswaran.finalprojectvenkateswaranananth;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.SortedList;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.squareup.okhttp.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.AlbumSimple;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TracksPager;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;


public class MainActivity extends AppCompatActivity implements
        PlayerNotificationCallback, ConnectionStateCallback{

    // Replace with your client ID
    private static final String CLIENT_ID = "03811ad50cb64b2189131012180de7a7";
    //  Replace with your redirect URI
    private static final String REDIRECT_URI = "my-first-android-app-login://callback";


    private Player mPlayer;
    private SpotifyService spotify;

    // Request code that will be used to verify if the result comes from correct activity
// Can be any integer
    private static final int REQUEST_CODE = 1337;

    private final ArrayList<String> trackInfo = new ArrayList<>();
    private final ArrayList<Track> trackObjs = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "playlist-modify-public", "playlist-modify-private", "playlist-read-private", "streaming"});
        AuthenticationRequest request = builder.build();


        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);






    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);

                final String accessToken = response.getAccessToken();

                RestAdapter restAdapter = new RestAdapter.Builder()
                        .setEndpoint(SpotifyApi.SPOTIFY_WEB_API_ENDPOINT)
                        .setRequestInterceptor(new RequestInterceptor() {
                            @Override
                            public void intercept(RequestFacade request) {
                                request.addHeader("Authorization", "Bearer " + accessToken);
                            }
                        })
                        .build();

                final SpotifyService spotify = restAdapter.create(SpotifyService.class);

                // search for songs
                searchTracks(spotify, "Orchard");


                // Find the constant USER_ID value
                spotify.getMe(new SpotifyCallback<UserPrivate>() {
                    @Override
                    public void failure(SpotifyError spotifyError) {
                        Log.d("MainActivity", "Error in getting current user: " + spotifyError);

                    }

                    @Override
                    public void success(UserPrivate userPrivate, retrofit.client.Response response) {
                        Log.d("MainActivity", "Successfully found user id: " + userPrivate.id);
                        // Create the auto-generated playlist based on tracks searched before
                        final String USER_ID = userPrivate.id;
                        Map<String, Object> body = new HashMap<>();
                        body.put("name", "Auto-Generated");
                        body.put("public", false);
                        spotify.createPlaylist(USER_ID, body, new SpotifyCallback<Playlist>() {
                            @Override
                            public void failure(SpotifyError spotifyError) {
                                Log.d("MainActivity", "Error in creating new playlist: " + spotifyError);
                            }

                            @Override
                            public void success(Playlist playlist, retrofit.client.Response response) {
                                final String PLAYLIST_ID = playlist.id;
                                Log.d("MainActivity", "Successfully created new playlist");
                                // Add tracks to auto-generated playlist
                                Log.d("MainActivity", "Track object size: " + trackObjs.size());
                                for (int i = 0; i < trackObjs.size(); i++) {
                                    Map<String, Object> parameters = new HashMap<>();
                                    parameters.put("uris", trackObjs.get(i).uri);
                                    spotify.addTracksToPlaylist(USER_ID, PLAYLIST_ID, parameters, parameters, new SpotifyCallback<Pager<PlaylistTrack>>() {
                                        @Override
                                        public void failure(SpotifyError spotifyError) {
                                            Log.d("MainActivity", "Error in adding tracks to auto-generated playlist: " + spotifyError);
                                        }

                                        @Override
                                        public void success(Pager<PlaylistTrack> playlistTrackPager, retrofit.client.Response response) {
                                            Log.d("MainActivity", "Success in adding track to auto-generated playlist");
                                            //Toast.makeText(getApplicationContext(), "Saved to Auto-generated Playlist", Toast.LENGTH_LONG).show();

                                        }
                                    });
                                }
                                // print out contents of playlists
                                spotify.getPlaylist(USER_ID, PLAYLIST_ID, new Callback<Playlist>() {
                                    @Override
                                    public void success(Playlist playlist, retrofit.client.Response response) {
                                        Log.d("MainActivity", "Found playlist");
                                        Pager<PlaylistTrack> playlistTracks = playlist.tracks;

                                        for (PlaylistTrack pt : playlistTracks.items) {
                                            Log.d("MainActivity:", "Tracks in playlist: " + pt.track.name);
                                        }

                                    }

                                    @Override
                                    public void failure(RetrofitError error) {
                                        Log.d("MainActivity", "Failed to find playlist: " + error);
                                    }
                                });

                            }
                        });


                    }
                });





                mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addPlayerNotificationCallback(MainActivity.this);
                        //mPlayer.play("spotify:track:0xYcCzw9Bu4DtCqAUriVuA");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }
    public void searchTracks(SpotifyService spotify, String streetName) {
        spotify.searchTracks(streetName, new Callback<TracksPager>() {
            @Override
            public void success(TracksPager tracksPager, retrofit.client.Response response) {
                Log.d("MainActivity", "Successfully searched for tracks");
                Pager<Track> pt = tracksPager.tracks;
                int size = 10;
                if (pt.items.size() < 10) {
                    size = pt.items.size();
                }
                for (int i = 0; i < size; i++) {
                    Track track = pt.items.get(i);
                    AlbumSimple album = track.album;
                    ArtistSimple artist = track.artists.get(0);
                    trackObjs.add(track);
                }

            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("MainActivity", "Error in search tracks: " + error);
            }
        });

    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("MainActivity", "Playback event received: " + eventType.name());
        switch (eventType) {
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d("MainActivity", "Playback error received: " + errorType.name());
        switch (errorType) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        // VERY IMPORTANT! This must always be called or else you will leak resources
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

}
