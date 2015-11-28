package itp341.ananth.venkateswaran.finalprojectvenkateswaranananth;

import android.graphics.Color;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.Response;

public class PlaylistDetailActivity extends Activity {

    ListView detailView;

    private static String ACCESS_TOKEN;
    private static final String TAG = "PlaylistDetailActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        detailView = (ListView) findViewById(R.id.listView);

        ACCESS_TOKEN = getIntent().getStringExtra("ACCESS_TOKEN");
        String USER_ID = getIntent().getStringExtra("USER_ID");
        String PLAYLIST_ID = getIntent().getStringExtra("PLAYLIST_ID");

        // create an instance of spotify
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

        final ArrayList<Track> tracks = new ArrayList<>();
        spotify.getPlaylist(USER_ID, PLAYLIST_ID, new SpotifyCallback<Playlist>() {
            @Override
            public void failure(SpotifyError spotifyError) {
                Log.d(TAG, "Error getting playlist: " + spotifyError);
            }

            @Override
            public void success(Playlist playlist, Response response) {
                Log.d(TAG, "Got playlist: " + playlist.name);
                for (PlaylistTrack track : playlist.tracks.items) {
                    tracks.add(track.track);
                }
                Collections.sort(tracks, new CustomComparator());
                // Now populate the custom listview with names and popularity using custom adapter
                TracksAdapter adapter = new TracksAdapter(getApplicationContext(), tracks) {

                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {

                        View view = super.getView(position, convertView, parent);
                        TextView text = (TextView) view.findViewById(R.id.tvName);
                        TextView text1 = (TextView) view.findViewById(R.id.tvPopularity);
                        text.setTextColor(Color.BLACK);
                        text1.setTextColor(Color.BLACK);
                        return view;
                    }
                };
                detailView.setAdapter(adapter);
            }
        });

    }
    public class CustomComparator implements Comparator<Track> {

        @Override
        public int compare(Track lhs, Track rhs) {
            return rhs.popularity.compareTo(lhs.popularity);
        }
    }

}
