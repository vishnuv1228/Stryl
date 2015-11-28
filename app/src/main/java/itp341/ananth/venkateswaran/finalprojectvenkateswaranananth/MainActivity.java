package itp341.ananth.venkateswaran.finalprojectvenkateswaranananth;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
import com.spotify.sdk.android.player.Spotify;

import java.io.IOException;
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
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TracksPager;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    // Replace with your client ID
    private static final String CLIENT_ID = "6d021f7f3c7b443da63f8362a22374d8";
    //  Replace with your redirect URI
    private static final String REDIRECT_URI = "stryl-login://callback";
    private Location mLastLocation;
    protected boolean mAddressRequested;
    protected String mAddressOutput;
    private AddressResultReceiver mResultReceiver;
    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";
    private  LocationRequest mLocationRequest;
    private static String PLAYLIST_ID;
    private static String USER_ID;
    private static String ACCESS_TOKEN;
    private static String TAG = "MainActivity";


    TextView mLocationAddressTextView;
    TextView lastPlayed;
    Button startButton;
    Button playlistButton;

    SharedPreferences sharedPreferences;
    public static final String MyPREFERENCES = "MyPrefs";
    public static final String TrackName = "TrackNameKey";

    private String tempStreet;
    // Request code that will be used to verify if the result comes from correct activity
    // Can be any integer
    private static final int REQUEST_CODE = 1337;
    private static final int TRACK_REQUEST = 12;
    private GoogleApiClient mGoogleApiClient;

    private String TRACK_NAME = "Nothing";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mResultReceiver = new AddressResultReceiver(new Handler());
        mAddressOutput = "";
        mAddressRequested = false;

        lastPlayed = (TextView) findViewById(R.id.lastPlayed);


        String value = getPrefString(getApplicationContext(), TrackName, "TrackNameKey");
        if (!value.equals("TrackNameKey")) {
            lastPlayed.setText("You last played " + value);
        }



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
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveToPlayScreen();


            }
        });
        playlistButton = (Button) findViewById(R.id.viewPlaylistsBtn);
        playlistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(getApplicationContext(), PlaylistScreen.class);
                i.putExtra("CLIENT_ID", CLIENT_ID);
                i.putExtra("USER_ID", USER_ID);
                i.putExtra("ACCESS_TOKEN", ACCESS_TOKEN);
                startActivity(i);
            }
        });
        mLocationAddressTextView = (TextView) findViewById(R.id.location_address_view);

    }
    public static void SaveInPreference(Context mContext, String key, String objString) {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(mContext.getString(R.string.app_name),
                Context.MODE_PRIVATE).edit();
        editor.putString(key, objString);
        editor.apply();
    }
    public static String getPrefString(Context mContext, final String key, final String defaultStr) {
        SharedPreferences pref = mContext.getSharedPreferences(mContext.getString(R.string.app_name),
                Context.MODE_PRIVATE);
        return pref.getString(key, defaultStr);
    }

    private void moveToPlayScreen() {
        Intent intent = new Intent(getApplicationContext(), PlayScreen.class);
        intent.putExtra("ACCESS_TOKEN", ACCESS_TOKEN);
        intent.putExtra("USER_ID", USER_ID);
        intent.putExtra("PLAYLIST_ID", PLAYLIST_ID);
        //intent.putExtra("TRACK_ID", TRACK_ID);
        intent.putExtra("CLIENT_ID", CLIENT_ID);
        intent.putExtra("STREET", tempStreet);
        startActivityForResult(intent, TRACK_REQUEST);
    }

    private void getLocation() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(100);
        mLocationRequest.setFastestInterval(100);
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
                tempStreet = mAddressOutput;
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
        Log.d(TAG, "Connected");

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
            Log.d(TAG, ioe.getMessage());
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
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
    protected void onResume() {
        super.onResume();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }
    protected void displayAddressOutput() {
       // Log.d(TAG, mAddressOutput);
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
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
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
            Log.d(TAG,"Address: " + mAddressOutput);
            displayAddressOutput();
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
        Log.d(TAG, "REQUEST CODE: " + requestCode);
        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                ACCESS_TOKEN = response.getAccessToken();
                searchOrMoveOn();

            }
        } // Check what the previous track was
        else if (requestCode == TRACK_REQUEST) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "IN HERE TRACK: " + intent.getStringExtra("TRACK_NAME"));
                TRACK_NAME = intent.getStringExtra("TRACK_NAME");
                // Shared Preferences
                SaveInPreference(getApplicationContext(), TrackName, TRACK_NAME);
                lastPlayed.setText("You last played " + TRACK_NAME);
            }
        }
    }

    private void searchOrMoveOn() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(SpotifyApi.SPOTIFY_WEB_API_ENDPOINT)
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("Authorization", "Bearer " + ACCESS_TOKEN);
                    }
                })
                .build();

        final SpotifyService spotify = restAdapter.create(SpotifyService.class);




        // Find the constant USER_ID value
        spotify.getMe(new SpotifyCallback<UserPrivate>() {
            @Override
            public void failure(SpotifyError spotifyError) {
                Log.d(TAG, "Error in getting current user: " + spotifyError);

            }

            @Override
            public void success(UserPrivate userPrivate, retrofit.client.Response response) {
                Log.d(TAG, "Successfully found user id: " + userPrivate.id);
                // Create the auto-generated playlist based on tracks searched before
                USER_ID = userPrivate.id;

                String split[] = tempStreet.split(" ");
                tempStreet = split[0];

                final String auto = "Auto-Generated: " + tempStreet;

                // Check if this playlist already exists

                spotify.getPlaylists(USER_ID, new SpotifyCallback<Pager<PlaylistSimple>>() {
                    @Override
                    public void failure(SpotifyError spotifyError) {
                        Log.d(TAG, "Error in getting user's playlists: " + spotifyError);
                    }

                    @Override
                    public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                        Boolean exists = false;
                        Log.d(TAG, "Success in getting user's playlists");
                        List<PlaylistSimple> playlists = playlistSimplePager.items;
                        for (PlaylistSimple playlist : playlists) {
                            Log.d(TAG, "Retrieved Playlist: " + playlist.name);
                            if (playlist.name.equals(auto)) {
                                Log.d(TAG, "Default Playlist EXISTS");
                                PLAYLIST_ID = playlist.id;
                                moveToPlayScreen();
                                exists = true;

                            }
                        }
                        if (!exists) {

                            Map<String, Object> body = new HashMap<>();
                            body.put("name", auto);
                            body.put("public", false);
                            spotify.createPlaylist(USER_ID, body, new SpotifyCallback<Playlist>() {
                                @Override
                                public void failure(SpotifyError spotifyError) {
                                    Log.d(TAG, "Error in creating new playlist: " + spotifyError);
                                }

                                @Override
                                public void success(Playlist playlist, retrofit.client.Response response) {
                                    PLAYLIST_ID = playlist.id;
                                    Log.d(TAG, "Successfully created new playlist");
                                    spotify.searchTracks(tempStreet, new Callback<TracksPager>() {
                                        @Override
                                        public void success(TracksPager tracksPager, retrofit.client.Response response) {
                                            Log.d(TAG, "Successfully searched for tracks");
                                            Pager<Track> pt = tracksPager.tracks;
                                            for (int i = 0; i < pt.items.size(); i++) {
                                                Map<String, Object> parameters = new HashMap<>();
                                                parameters.put("uris", pt.items.get(i).uri);
                                                spotify.addTracksToPlaylist(USER_ID, PLAYLIST_ID, parameters, parameters, new SpotifyCallback<Pager<PlaylistTrack>>() {
                                                    @Override
                                                    public void failure(SpotifyError spotifyError) {
                                                        Log.d(TAG, "Error in adding tracks to auto-generated playlist: " + spotifyError);
                                                    }

                                                    @Override
                                                    public void success(Pager<PlaylistTrack> playlistTrackPager, retrofit.client.Response response) {
                                                        Log.d(TAG, "Success in adding track to auto-generated playlist");

                                                    }
                                                });
                                            }

                                        }

                                        @Override
                                        public void failure(RetrofitError error) {
                                            Log.d(TAG, "Error in search tracks: " + error);
                                        }
                                    });
                                }
                            });
                        }
                    }
                });

            }
        });

    }

}

