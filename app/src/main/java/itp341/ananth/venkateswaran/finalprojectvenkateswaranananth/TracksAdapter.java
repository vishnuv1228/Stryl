package itp341.ananth.venkateswaran.finalprojectvenkateswaranananth;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Track;


public class TracksAdapter extends ArrayAdapter<Track> {
    public TracksAdapter(Context context, ArrayList<Track> tracks) {
        super(context, 0, tracks);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Track track = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_track, parent, false);
        }
        // Lookup view for data population
        TextView tvName = (TextView) convertView.findViewById(R.id.tvName);
        TextView tvHome = (TextView) convertView.findViewById(R.id.tvPopularity);
        // Populate the data into the template view using the data object
        tvName.setText(track.name);
        tvHome.setText(Integer.toString(track.popularity));
        // Return the completed view to render on screen
        return convertView;
    }
}
