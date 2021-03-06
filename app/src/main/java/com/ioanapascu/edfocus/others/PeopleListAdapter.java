package com.ioanapascu.edfocus.others;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ioanapascu.edfocus.R;
import com.ioanapascu.edfocus.model.Person;

import java.util.ArrayList;

/**
 * Created by ioana on 11/3/2017.
 * Used for Search Activity.
 */

public class PeopleListAdapter extends ArrayAdapter<Person> {

    private static final String TAG = "PeopleListAdapter";

    private ArrayList<Person> people;
    private Context mContext;
    private int mResource;

    /**
     * Default constructor for the PersonListAdapter
     * @param context
     * @param resource
     * @param objects
     */
    public PeopleListAdapter(Context context, int resource, ArrayList<Person> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // get person information
        String id = getItem(position).getId();
        String name = getItem(position).getName();
        String userType = getItem(position).getUserType();
        String profilePhoto = getItem(position).getProfilePhoto();

        // create the person object with the information
        Person person = new Person(id, name, userType, profilePhoto);

        ViewHolder holder;

        if (convertView == null){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mResource, parent, false);
            holder = new ViewHolder();
            holder.mPersonName = convertView.findViewById(R.id.text_person_name);
            holder.mPersonUserType = convertView.findViewById(R.id.text_person_user_type);
            holder.mPersonProfilePhoto = convertView.findViewById(R.id.image_person_profile_photo);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.mPersonName.setText(person.getName());
        String capitalizedUserType = person.getUserType().substring(0, 1).toUpperCase() + person.getUserType().substring(1);
        holder.mPersonUserType.setText(capitalizedUserType);
        UniversalImageLoader.setImage(person.getProfilePhoto(), holder.mPersonProfilePhoto, null);

        return convertView;
    }

    /**
     * Holds variables in a View
     */
    private static class ViewHolder {
        ImageView mPersonProfilePhoto, mAddContactImageView;
        TextView mPersonName, mPersonUserType;
    }

}
