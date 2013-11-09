package com.task;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxException;

public class TaskList extends Activity {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHelper = new TaskHelpers(getApplicationContext());

        mDbxAcctMgr = mHelper.getAccountManager();
        setContentView(R.layout.activity_datastore_task);

        if (mHelper.hasLinkedAccount()) {
            showTasksView();
        }

        Button linkButton = (Button) findViewById(R.id.link_button);
        linkButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDbxAcctMgr.startLink((Activity)TaskList.this, REQUEST_LINK_TO_DBX);
            }
        });
    }

    private void newTask(String text) {
        getNewTaskInput().setText("");
        mHelper.addTask(text);
    }

    private EditText getNewTaskInput() {
        return  (EditText) findViewById(R.id.new_task_name);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDbxAcctMgr.hasLinkedAccount()) {
            // already have an account linked
            showTasksView();
        } else {
            // Hide the add-task UI and show the link button
            findViewById(R.id.link_button).setVisibility(View.VISIBLE);
            findViewById(R.id.new_task_ui).setVisibility(View.GONE);
            ((ViewGroup) findViewById(R.id.task_table)).removeAllViews();
        }
    }

    protected void onPause() {
        super.onPause();
        if (mDatastore != null) {
            mDatastore.removeSyncStatusListener(mDatastoreListener);
            mDatastore.close();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
    }

    private void showTasksView() {
        try {
            mDatastore = mHelper.getDatastore();
            mTaskTable = mHelper.getTaskTable();

            // Hide the Link button and show the add-task UI
            findViewById(R.id.link_button).setVisibility(View.GONE);
            findViewById(R.id.new_task_ui).setVisibility(View.VISIBLE);

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

        TableLayout tableView = (TableLayout) findViewById(R.id.task_table);
        tableView.removeAllViews();

        for (final TaskTable.Task task: tasks) {
            TableRow row = (TableRow)getLayoutInflater().inflate(R.layout.task_row, null);
            tableView.addView(row);

            CheckBox toggleComplete = (CheckBox)row.findViewById(R.id.completed_checkbox);
            toggleComplete.setChecked(task.isCompleted());
            toggleComplete.setOnClickListener(new OnClickListener() {
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
