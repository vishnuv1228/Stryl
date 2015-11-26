package itp341.ananth.venkateswaran.finalprojectvenkateswaranananth;
import android.annotation.SuppressLint;
import android.content.Intent;
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
import com.spotify.sdk.android.player.Spotify;

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
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.PlaylistTracksInformation;
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
    private static String PLAYLIST_ID;
    private static String TRACK_ID;
    private static String USER_ID;
    private static String ACCESS_TOKEN;
    private static String TAG = "MainActivity";


    protected TextView mLocationAddressTextView;



    Button startButton;
    private String tempStreet;
    // Request code that will be used to verify if the result comes from correct activity
// Can be any integer
    private static final int REQUEST_CODE = 1337;
    private final ArrayList<Track> trackObjs = new ArrayList<>();
    private GoogleApiClient mGoogleApiClient;
    private Track trackFinal;

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
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveToPlayScreen();


            }
        });
        mLocationAddressTextView = (TextView) findViewById(R.id.location_address_view);

    }

    private void moveToPlayScreen() {
        Intent intent = new Intent(getApplicationContext(), PlayScreen.class);
        intent.putExtra("ACCESS_TOKEN", ACCESS_TOKEN);
        intent.putExtra("USER_ID", USER_ID);
        intent.putExtra("PLAYLIST_ID", PLAYLIST_ID);
        //intent.putExtra("TRACK_ID", TRACK_ID);
        intent.putExtra("CLIENT_ID", CLIENT_ID);
        intent.putExtra("STREET", tempStreet);
        startActivity(intent);
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
                ACCESS_TOKEN = response.getAccessToken();
                searchOrMoveOn();

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

                String split [] = tempStreet.split(" ");
                tempStreet = split[0];

                final String auto = "Auto-Generated: " + tempStreet;
                //searchSong(spotify, tempStreet);

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
                                                        //Toast.makeText(getApplicationContext(), "Saved to Auto-generated Playlist", Toast.LENGTH_LONG).show();

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

   /* private void searchSong(SpotifyService spotify, String tempStreet) {
        spotify.searchTracks(tempStreet, new Callback<TracksPager>() {
            @Override
            public void success(TracksPager tracksPager, retrofit.client.Response response) {
                Log.d(TAG, "Successfully searched for tracks");
                Pager<Track> pt = tracksPager.tracks;
                trackFinal = pt.items.get(0);
                TRACK_ID = trackFinal.id;
                Log.d(TAG, "TRACK ID: " + TRACK_ID);

            }

            @Override
            public void failure(RetrofitError error) {
                Log.d(TAG, "Error in search tracks: " + error);
            }
        });

    }*/


    public void searchTracks(SpotifyService spotify, String streetName) {

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

