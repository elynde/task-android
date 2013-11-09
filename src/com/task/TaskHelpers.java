package com.task;

import android.content.Context;
import android.widget.Toast;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxException;

public class TaskHelpers {
    private static final String APP_KEY = "mssnaktycwvrb8q";
    private static final String APP_SECRET = "tehdvvzr4qmswt0";

    private TaskTable taskTable;
    private DbxAccountManager accountManager;
    private DbxDatastore mDatastore;
    private Context context;

    public TaskHelpers(Context app_context) {
        context = app_context;
    }

    public boolean addTask(String task_name) {
        try {
        if (task_name.length() > 0) {
            getTaskTable().createTask(task_name);
            getDatastore().sync();
        }
        }
        catch (DbxException e) {
            handleException(e);
        }
        return false;
    }

    public TaskTable getTaskTable() {
        return new TaskTable(getDatastore());
    }

    public DbxDatastore getDatastore() {

        try {
        if (null == mDatastore || !mDatastore.isOpen()) {
            mDatastore = DbxDatastore.openDefault(getAccountManager().getLinkedAccount());
        }
        }
        catch (Exception e) {

        }

      return mDatastore;
    }

    public DbxAccountManager getAccountManager() {
        if (accountManager == null) {
            accountManager = DbxAccountManager.getInstance(context, APP_KEY, APP_SECRET);
        }
        return  accountManager;
    }

    public boolean hasLinkedAccount() {
        return getAccountManager().hasLinkedAccount();
    }



    public void handleException(DbxException e) {
        e.printStackTrace();
        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
    }

}
