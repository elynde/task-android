package com.task;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxException;

import java.util.List;

public class TaskListFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v =  inflater.inflate(R.layout.fragment_task_list, container, false);
        return v;

    }

    public void onActivityCreated(Bundle b) {
        View v = getView();
        mHelper = new TaskHelpers(getActivity());

        mDbxAcctMgr = mHelper.getAccountManager();

        if (mHelper.hasLinkedAccount()) {
            showTasksView();
        }

        Button linkButton = (Button) v.findViewById(R.id.link_button);
        linkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDbxAcctMgr.startLink((Activity)getActivity(), REQUEST_LINK_TO_DBX);
            }
        });
    }

    private static final String APP_KEY = "mssnaktycwvrb8q";
    private static final String APP_SECRET = "tehdvvzr4qmswt0";

    private static final int REQUEST_LINK_TO_DBX = 0;

    private DbxAccountManager mDbxAcctMgr;
    private DbxDatastore mDatastore;
    private TaskTable mTaskTable;
    private TaskHelpers mHelper;

    private DbxDatastore.SyncStatusListener mDatastoreListener = new DbxDatastore.SyncStatusListener() {
        @Override
        public void onDatastoreStatusChange(DbxDatastore ds) {
            if (ds.getSyncStatus().hasIncoming) {
                try {
                    mDatastore.sync();
                } catch (DbxException e) {
                    mHelper.handleException(e);
                }
            }
            updateList();
        }
    };

    private void newTask(String text) {
        getNewTaskInput().setText("");
        mHelper.addTask(text);
    }

    private EditText getNewTaskInput() {
        return  (EditText) getView().findViewById(R.id.new_task_name);

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mDbxAcctMgr.hasLinkedAccount()) {
            // already have an account linked
            showTasksView();
        } else {
            // Hide the add-task UI and show the link button
            getView().findViewById(R.id.link_button).setVisibility(View.VISIBLE);
            getView().findViewById(R.id.new_task_ui).setVisibility(View.GONE);
            ((ViewGroup) getView().findViewById(R.id.task_table)).removeAllViews();
        }
    }

    public void onPause() {
        super.onPause();
        if (mDatastore != null) {
            mDatastore.removeSyncStatusListener(mDatastoreListener);
            mDatastore.close();
        }
    }

    //@Override
    /*public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LINK_TO_DBX) {
            if (resultCode == RESULT_OK) {
                showTasksView();
            } else {
                // ... Link failed or was cancelled by the user.
                Toast.makeText(this, "Link to Dropbox failed.", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    } */

    private void showTasksView() {
        try {
            mDatastore = mHelper.getDatastore();
            mTaskTable = mHelper.getTaskTable();

            // Hide the Link button and show the add-task UI
            getView().findViewById(R.id.link_button).setVisibility(View.GONE);
            getView().findViewById(R.id.new_task_ui).setVisibility(View.VISIBLE);

            mDatastore.addSyncStatusListener(mDatastoreListener);
            mDatastore.sync();
            updateList();

            mDatastore.sync();
        } catch (DbxException e) {
            mHelper.handleException(e);
        }

    }

    private void updateList() {
        List<TaskTable.Task> tasks;
        try {
            tasks = mTaskTable.getTasksSorted();
        } catch (DbxException e) {
            mHelper.handleException(e);
            return;
        }

        TableLayout tableView = (TableLayout) getView().findViewById(R.id.task_table);
        tableView.removeAllViews();

        for (final TaskTable.Task task: tasks) {
            TableRow row = (TableRow)getActivity().getLayoutInflater().inflate(R.layout.task_row, null);
            tableView.addView(row);

            CheckBox toggleComplete = (CheckBox)row.findViewById(R.id.completed_checkbox);
            toggleComplete.setChecked(task.isCompleted());
            toggleComplete.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        task.toggleCompleted();
                    } catch (DbxException e) {
                        mHelper.handleException(e);
                    }
                }
            });

            TextView text = (TextView)row.findViewById(R.id.task_text);
            text.setText(task.getName());
            getNewTaskInput().setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                        newTask(getNewTaskInput().getText().toString());
                    }
                    return true;
                }
            });
            if (task.isCompleted()) {
                text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }
    }
}
