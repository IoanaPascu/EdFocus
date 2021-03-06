package com.ioanapascu.edfocus.teacher;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.ioanapascu.edfocus.BaseActivity;
import com.ioanapascu.edfocus.R;
import com.ioanapascu.edfocus.model.Course;
import com.ioanapascu.edfocus.model.GradeDb;
import com.ioanapascu.edfocus.model.GradeRow;
import com.ioanapascu.edfocus.model.UserAccountSettings;
import com.ioanapascu.edfocus.others.MultipleGradesListAdapter;
import com.ioanapascu.edfocus.utils.Utils;

import java.util.ArrayList;

public class AddMultipleGradesActivity extends BaseActivity implements View.OnClickListener {

    // widgets
    private RecyclerView mGradesList;
    private ImageView mSaveImg;
    private Spinner mCoursesSpinner;
    private DatePicker mDatePicker;
    private EditText mNameText;
    TextInputLayout mNameTil;

    // variables
    private ArrayList<String> mStudentIds, mCourseNames, mCourseIds;
    private ArrayList<GradeRow> mGrades;
    private MultipleGradesListAdapter mAdapter;
    private String mClassId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarGradient(AddMultipleGradesActivity.this, false);
        setContentView(R.layout.activity_add_multiple_grades);

        mStudentIds = getIntent().getStringArrayListExtra("selectedStudentsIds");
        mClassId = getIntent().getStringExtra("classId");
        mGrades = new ArrayList<>();

        // widgets
        mGradesList = findViewById(R.id.recycler_grades);
        mCoursesSpinner = findViewById(R.id.spinner_courses);
        mSaveImg = findViewById(R.id.img_save);
        mDatePicker = findViewById(R.id.date_picker);
        mNameText = findViewById(R.id.text_name);
        mNameTil = findViewById(R.id.til_name);

        mSaveImg.setOnClickListener(this);
        mGradesList.setNestedScrollingEnabled(false);

        mAdapter = new MultipleGradesListAdapter(AddMultipleGradesActivity.this,
                mGrades, mClassId);
        mGradesList.setAdapter(mAdapter);
        mGradesList.setLayoutManager(new LinearLayoutManager(AddMultipleGradesActivity.this));

        populateSpinner();
        populateListView();

    }

    private void populateListView() {
        for (String studentId : mStudentIds) {
            // get student name from Firebase and create a row for him
            firebase.mUserAccountSettingsRef.child(studentId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    UserAccountSettings settings = dataSnapshot.getValue(UserAccountSettings.class);

                    GradeRow grade = new GradeRow(settings.getId(), settings.getFirstName()
                            + " " + settings.getLastName());
                    mGrades.add(grade);
                    mAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    private void populateSpinner() {
        mCourseNames = new ArrayList<>();
        mCourseIds = new ArrayList<>();

        // set adapter for spinner
        final ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(AddMultipleGradesActivity.this,
                android.R.layout.simple_spinner_item, mCourseNames);

        // spinner row layout style
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attach data adapter to spinner
        mCoursesSpinner.setAdapter(dataAdapter);

        // get courses from firebase
        firebase.mClassCoursesRef.child(mClassId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mCourseNames.clear();

                if (dataSnapshot.getChildrenCount() > 0) {
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        Course course = data.getValue(Course.class);
                        mCourseNames.add(course.getName());
                        mCourseIds.add(course.getId());

                        dataAdapter.notifyDataSetChanged();
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void saveAllGrades() {
        String courseId = mCourseIds.get(mCoursesSpinner.getSelectedItemPosition());
        Long date = Utils.yearMonthDayToMillis(mDatePicker.getYear(), mDatePicker.getMonth(),
                mDatePicker.getDayOfMonth());
        String name = mNameText.getText().toString();

        // validation
        if (!Utils.toggleFieldError(mNameTil, name, "Please enter a name/title for the grade.")) {
            return;
        }
        // check if teacher entered grade for every student
        boolean allFilledIn = true;
        for (int i = 0; i < mGradesList.getChildCount(); i++) {
            MultipleGradesListAdapter.GradeViewHolder holder =
                    (MultipleGradesListAdapter.GradeViewHolder) mGradesList.findViewHolderForAdapterPosition(i);
            allFilledIn = holder.checkGradeField() && allFilledIn;
        }
        if (!allFilledIn) {
            return;
        }

        ArrayList<GradeRow> gradesInfo = (ArrayList<GradeRow>) mAdapter.getGradesInfo();
        for (GradeRow gradeRow : gradesInfo) {
            // create grade to save in the database
            GradeDb grade = new GradeDb(null, name, gradeRow.getGrade(), date, gradeRow.getNotes(),
                    mClassId, courseId, gradeRow.getStudentId());

            String gradeId = firebase.mStudentGradesRef.child(mClassId).child(gradeRow.getStudentId()).push().getKey();
            grade.setId(gradeId);
            firebase.mStudentGradesRef.child(mClassId).child(gradeRow.getStudentId()).child(gradeId)
                    .setValue(grade);

        }

        finish();
    }

    @Override
    public void onClick(View view) {
        if (view == mSaveImg) {
            saveAllGrades();
        }
    }
}
