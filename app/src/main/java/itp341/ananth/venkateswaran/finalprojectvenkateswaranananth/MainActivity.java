package itp341.ananth.venkateswaran.finalprojectvenkateswaranananth;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import android.location.Geocoder;
import android.os.Handler;
import android.os.ResultReceiver;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
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
        PlayerNotificationCallback, ConnectionStateCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    // Replace with your client ID
    private static final String CLIENT_ID = "03811ad50cb64b2189131012180de7a7";
    //  Replace with your redirect URI
    private static final String REDIRECT_URI = "my-first-android-app-login://callback";
    private Location mLastLocation;
    protected boolean mAddressRequested;
    protected String mAddressOutput;
    private AddressResultReceiver mResultReceiver;
    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";
    private  LocationRequest mLocationRequest;
    protected LocationManager locationManager;

    protected TextView mLocationAddressTextView;

    private Player mPlayer;

    Button startButton;
    private String tempStreet;
    // Request code that will be used to verify if the result comes from correct activity
// Can be any integer
    private static final int REQUEST_CODE = 1337;
    private final ArrayList<Track> trackObjs = new ArrayList<>();
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mResultReceiver = new AddressResultReceiver(new Handler());
        mAddressOutput = "";
        mAddressRequested = false;

        updateValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "playlist-modify-public", "playlist-modify-private", "playlist-read-private", "streaming"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
        getLocation();
        startButton = (Button) findViewById(R.id.startButton);
        mLocationAddressTextView = (TextView) findViewById(R.id.location_address_view);

    }

    private void getLocation() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10);
        mLocationRequest.setFastestInterval(10);
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Check savedInstanceState to see if the address was previously requested.
            if (savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)) {
                mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
            }
            // Check savedInstanceState to see if the location address string was previously found
            // and stored in the Bundle. If it was found, display the address string in the UI.
            if (savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
                displayAddressOutput();
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
    @Override
    public void onConnected(Bundle connectionHint) {
        // Connected to Google Play services!
        // The good stuff goes here.
        Log.d("MainActivity", "Connected");

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        }
    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        Geocoder geocoder;
        List<Address> addresses = null;
        geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1); //
        } catch(IOException ioe) {
            Log.d("MainActivity", ioe.getMessage());
        }
        if  (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
           tempStreet = address.getThoroughfare();
        }
        startIntentService();

    }
    protected void startIntentService() {
        // Create an intent for passing to the intent service responsible for fetching the address.
        Intent intent = new Intent(this, FetchAddressIntentService.class);

        // Pass the result receiver as an extra to the service.
        intent.putExtra(Constants.RECEIVER, mResultReceiver);

        // Pass the location data as an extra to the service.
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        startService(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
    protected void displayAddressOutput() {
       // Log.d("MainActivity", mAddressOutput);
        String [] split = tempStreet.split(" ");
       tempStreet = split[0];
        mLocationAddressTextView.setText(tempStreet);


    }
    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        mGoogleApiClient.connect();
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save whether the address has been requested.
        savedInstanceState.putBoolean(ADDRESS_REQUESTED_KEY, mAddressRequested);

        // Save the address string.
        savedInstanceState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
        super.onSaveInstanceState(savedInstanceState);
    }
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        //
        // More about this in the 'Handle Connection Failures' section.
        Log.i("MainActivity", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }
    @SuppressLint("ParcelCreator")
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            Log.d("MainActivity","Address: " + mAddressOutput);
           displayAddressOutput();
            // Show a toast message if an address was found.
            /*if (resultCode == Constants.SUCCESS_RESULT) {
                showToast(getString(R.string.address_found));
            }*/

        }
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
                final String streetName = "Orchard";
                searchTracks(spotify, streetName);


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
                        body.put("name", "Auto-Generated: " + streetName);
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
                for (int i = 0; i < pt.items.size(); i++) {
                    Track track = pt.items.get(i);
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
    protected void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }



}

