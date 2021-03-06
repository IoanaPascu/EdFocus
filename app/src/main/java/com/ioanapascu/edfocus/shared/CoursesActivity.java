package com.ioanapascu.edfocus.shared;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.ioanapascu.edfocus.BaseActivity;
import com.ioanapascu.edfocus.R;
import com.ioanapascu.edfocus.model.Course;
import com.ioanapascu.edfocus.others.CoursesListAdapter;
import com.ioanapascu.edfocus.utils.Utils;

import java.util.ArrayList;

public class CoursesActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "CoursesActivity";

    // widgets
    FloatingActionButton mAddCourseFab;
    ListView mCoursesListView;
    RelativeLayout mNoCoursesLayout;

    // variables
    private String mClassId, mUserType;
    private ArrayList<Course> mCourses;
    private CoursesListAdapter mCoursesListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarGradient(CoursesActivity.this, false);
        setContentView(R.layout.activity_courses);

        mCourses = new ArrayList<>();
        mUserType = firebase.getCurrentUserType();

        // get id of class to display courses for
        Intent myIntent = getIntent();
        mClassId = myIntent.getStringExtra("classId");

        // widgets
        mAddCourseFab = findViewById(R.id.fab_add_course);
        mCoursesListView = findViewById(R.id.list_courses);
        mNoCoursesLayout = findViewById(R.id.layout_no_courses);

        mCoursesListAdapter = new CoursesListAdapter(CoursesActivity.this, R.layout.row_course,
                mCourses, mClassId, mUserType);
        mCoursesListView.setAdapter(mCoursesListAdapter);

        // listeners
        mAddCourseFab.setOnClickListener(this);

        // only teacher can add and edit courses
        if (mUserType.equals("teacher")) {
            mCoursesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // clicking on a course opens dialog for editing it
                    showEditDialog(position);
                }
            });
            mAddCourseFab.setVisibility(View.VISIBLE);
        }

        displayCourses();
    }

    private void displayCourses() {
        firebase.mClassCoursesRef.child(mClassId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mCourses.clear();
                mCoursesListAdapter.notifyDataSetChanged();

                if (dataSnapshot.getChildrenCount() > 0) {
                    mNoCoursesLayout.setVisibility(View.GONE);

                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        Course course = data.getValue(Course.class);
                        mCourses.add(course);
                    }
                } else {
                    mNoCoursesLayout.setVisibility(View.VISIBLE);
                }

                mCoursesListAdapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void showEditDialog(int position) {
        final Course course = mCourses.get(position);

        final Dialog dialog = new Dialog(CoursesActivity.this);
        dialog.setContentView(R.layout.dialog_add_course);

        // dialog widgets
        final TextView titleText = dialog.findViewById(R.id.txt_title);
        final EditText nameText = dialog.findViewById(R.id.text_name);
        final TextInputLayout nameTil = dialog.findViewById(R.id.til_name);
        final EditText teacherText = dialog.findViewById(R.id.text_teacher);
        final TextInputLayout teacherTil = dialog.findViewById(R.id.til_teacher);
        final EditText descriptionText = dialog.findViewById(R.id.text_description);
        final TextInputLayout descriptionTil = dialog.findViewById(R.id.til_description);
        Button createBtn = dialog.findViewById(R.id.btn_create);
        ImageView cancelImg = dialog.findViewById(R.id.img_cancel);

        titleText.setText("Edit Course Info");
        createBtn.setText("Save");

        // set old values
        nameText.setText(course.getName());
        teacherText.setText(course.getTeacher());
        descriptionText.setText(course.getDescription());

        // create course button click
        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get info introduced by user
                String name = nameText.getText().toString();
                String teacher = teacherText.getText().toString();
                String description = descriptionText.getText().toString();

                // validation
                boolean valid = Utils.toggleFieldError(nameTil, name, "Please enter a name for the course.");
                valid = Utils.toggleFieldError(teacherTil, teacher, "Please enter a name for the course's teacher.") && valid;
                if (!valid) {
                    return;
                }

                // save info at course id in firebase
                Course newCourse = new Course(course.getId(), name, teacher, description);

                // save to firebase
                firebase.mClassCoursesRef.child(mClassId).child(course.getId()).setValue(newCourse);
                dialog.dismiss();
            }
        });

        // x button click (cancel)
        cancelImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showAddDialog() {
        final Dialog dialog = new Dialog(CoursesActivity.this);
        dialog.setTitle("Add Course");
        dialog.setContentView(R.layout.dialog_add_course);

        // dialog widgets
        final EditText nameText = dialog.findViewById(R.id.text_name);
        final TextInputLayout nameTil = dialog.findViewById(R.id.til_name);
        final EditText teacherText = dialog.findViewById(R.id.text_teacher);
        final TextInputLayout teacherTil = dialog.findViewById(R.id.til_teacher);
        final EditText descriptionText = dialog.findViewById(R.id.text_description);
        final TextInputLayout descriptionTil = dialog.findViewById(R.id.til_description);
        Button createBtn = dialog.findViewById(R.id.btn_create);
        ImageView cancelImg = dialog.findViewById(R.id.img_cancel);

        // create course button click
        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get info introduced by user
                String name = nameText.getText().toString();
                String teacher = teacherText.getText().toString();
                String description = descriptionText.getText().toString();

                // validation
                boolean valid = Utils.toggleFieldError(nameTil, name, "Please enter a name for the course.");
                valid = Utils.toggleFieldError(teacherTil, name, "Please enter a name for the course's teacher.") && valid;
                if (!valid) {
                    return;
                }

                // get id where to put the new course in firebase
                String courseId = firebase.mClassCoursesRef.child(firebase.getCurrentUserId()).push().getKey();
                Course course = new Course(courseId, name, teacher, description);

                // save to firebase
                firebase.mClassCoursesRef.child(mClassId).child(courseId).setValue(course);
                dialog.dismiss();
            }
        });

        // x button click (cancel)
        cancelImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }

    @Override
    public void onClick(View view) {
        if (view == mAddCourseFab) {
            showAddDialog();
        }
    }
}
