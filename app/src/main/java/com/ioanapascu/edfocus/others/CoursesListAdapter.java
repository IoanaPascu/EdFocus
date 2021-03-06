package com.ioanapascu.edfocus.others;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.ioanapascu.edfocus.R;
import com.ioanapascu.edfocus.model.Course;
import com.ioanapascu.edfocus.model.ScheduleEntry;
import com.ioanapascu.edfocus.utils.FirebaseUtils;

import java.util.ArrayList;

public class CoursesListAdapter extends ArrayAdapter<Course> {

    private static final String TAG = "ClassesListAdapter";
    private final FirebaseUtils firebase;
    // variables
    private Context mContext;
    private int mResource;
    private String mClassId, mUserType;

    public CoursesListAdapter(Context context, int resource, ArrayList<Course> objects, String classId,
                              String userType) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
        mClassId = classId;
        mUserType = userType;
        firebase = new FirebaseUtils(mContext);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // get course information
        final String courseId = getItem(position).getId();
        String name = getItem(position).getName();
        String teacher = getItem(position).getTeacher();
        String description = getItem(position).getDescription();

        // create the course object with the information
        Course course = new Course(courseId, name, teacher, description);

        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mResource, parent, false);
            holder = new ViewHolder();
            holder.mName = convertView.findViewById(R.id.txt_name);
            holder.mTeacher = convertView.findViewById(R.id.txt_teacher);
            holder.mDescription = convertView.findViewById(R.id.txt_description);
            holder.mDelete = convertView.findViewById(R.id.img_delete);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.mName.setText(course.getName());
        holder.mTeacher.setText(course.getTeacher());
        holder.mDescription.setText(course.getDescription());

        // only teacher can delete a course
        if (mUserType.equals("teacher")) {
            holder.mDelete.setVisibility(View.VISIBLE);
        }

        // delete icon click
        holder.mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show confirmation dialog
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

                // set dialog message
                alertDialogBuilder
                        .setMessage("Are you sure you want to delete this course?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                deleteCourseFromDb(courseId);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                // create and show alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();

            }
        });

        return convertView;
    }

    private void deleteCourseFromDb(final String id) {
        // delete course with id
        firebase.mClassCoursesRef.child(mClassId).child(id).removeValue();

        // delete all entries in schedule with this course
        firebase.mClassScheduleRef.child(mClassId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshotDays : dataSnapshot.getChildren()) { // iterate Mon, Tue, ..
                    for (DataSnapshot snapshotEntries : snapshotDays.getChildren()) { // iterate each entry for current day
                        ScheduleEntry entry = snapshotEntries.getValue(ScheduleEntry.class);

                        if (entry.getCourseId().equals(id)) {
                            snapshotEntries.getRef().removeValue();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Holds variables in a View
     */
    private static class ViewHolder {
        ImageView mPhoto, mEdit, mDelete;
        TextView mName, mTeacher, mDescription;
    }

}
