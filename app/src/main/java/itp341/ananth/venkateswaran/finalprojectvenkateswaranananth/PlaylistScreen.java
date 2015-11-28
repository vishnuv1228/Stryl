package itp341.ananth.venkateswaran.finalprojectvenkateswaranananth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.Response;

public class PlaylistScreen extends Activity {

    private static final String TAG = "PlaylistScreen";
    private static String ACCESS_TOKEN;

    ListView listView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_screen);

        listView = (ListView) findViewById(R.id.playlistView);

        ACCESS_TOKEN = getIntent().getStringExtra("ACCESS_TOKEN");
        final String USER_ID = getIntent().getStringExtra("USER_ID");

        // Grab an instance of spotify
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

        final List<String> playlistNames = new ArrayList<>();
        final List<PlaylistSimple> playlistsObjs = new ArrayList<>();
        // Find all of the users playlists that have the keyword Auto-Generated
        spotify.getPlaylists(USER_ID, new SpotifyCallback<Pager<PlaylistSimple>>() {
            @Override
            public void failure(SpotifyError spotifyError) {
                Log.d(TAG, "Error in getting user's playlists: " + spotifyError);
            }

            @Override
            public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                Log.d(TAG, "Success in getting user's playlists");
                List<PlaylistSimple> playlists = playlistSimplePager.items;
                for (PlaylistSimple playlist : playlists) {
                    Log.d(TAG, "Retrieved Playlist: " + playlist.name);
                    if (playlist.name.startsWith("Auto-Generated:")) {
                        // populate listview with the playlists
                        playlistNames.add(playlist.name);
                        playlistsObjs.add(playlist);

                    }
                }
                if (playlistNames.isEmpty()) {
                    playlistNames.add("Listen to some music to see more playlists");
                }
                // Adapter
                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, playlistNames) {

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {

                        View view = super.getView(position, convertView, parent);
                        TextView text = (TextView) view.findViewById(android.R.id.text1);
                        text.setTextColor(Color.BLACK);
                        return view;
                    }
                };
                listView.setAdapter(adapter);
                // Listener
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        // launch into detail activity
                        Intent i = new Intent(getApplicationContext(), PlaylistDetailActivity.class);
                        i.putExtra("PLAYLIST_ID", playlistsObjs.get(position).id);
                        i.putExtra("USER_ID", USER_ID);
                        i.putExtra("ACCESS_TOKEN", ACCESS_TOKEN);
                        startActivity(i);
                    }
                });
            }
        });


    }

}
