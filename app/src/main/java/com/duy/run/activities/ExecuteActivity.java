package com.duy.run.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.duy.JavaApplication;
import com.duy.ide.CompileManager;
import com.duy.android.compiler.java.Java;
import com.duy.ide.R;
import com.duy.ide.activities.AbstractAppCompatActivity;
import com.duy.android.compiler.file.java.JavaProject;
import com.duy.run.view.ConsoleEditText;

import java.io.File;
import java.io.InputStream;

/**
 * Created by Duy on 30-Jul-17.
 */

public class ExecuteActivity extends AbstractAppCompatActivity {
    private static final int RUN_TIME_ERR = 1;
    private static final String TAG = "ExecuteActivity";
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case RUN_TIME_ERR:
                    Exception exception = (Exception) msg.obj;
                    showDialogError(exception.getMessage());
                    break;
            }
        }
    };
    private ConsoleEditText mConsoleEditText;
    public static final int RUN_DEX = 1;

    private void showDialogError(String message) {
        if (isFinishing()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setPositiveButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exec);
        setupToolbar();
        bindView();
        initInOut();
        final Intent intent = getIntent();
        if (intent != null) {
            final JavaProject projectFile = (JavaProject) intent.getSerializableExtra(CompileManager.PROJECT_FILE);
            if (projectFile == null) {
                finish();
                return;
            }
            final int action = intent.getIntExtra(CompileManager.ACTION, -1);
            setTitle(projectFile.getMainClass().getSimpleName());

            Thread runThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        runProgram(projectFile, action, intent);
                    } catch (Error error) {
                        error.printStackTrace(mConsoleEditText.getErrorStream());
                    } catch (Exception e) {
                        e.printStackTrace(mConsoleEditText.getErrorStream());
                    } catch (Throwable e) {
                        e.printStackTrace(mConsoleEditText.getErrorStream());
                    }
                }
            });
            runThread.start();
        } else {
            finish();
        }
    }

    private void initInOut() {
        JavaApplication application = (JavaApplication) getApplication();
        application.addStdErr(mConsoleEditText.getErrorStream());
        application.addStdOut(mConsoleEditText.getOutputStream());
    }

    @WorkerThread
    private void runProgram(JavaProject projectFile, int action, Intent intent) throws Exception {
        InputStream in = mConsoleEditText.getInputStream();

        File tempDir = getDir("dex", MODE_PRIVATE);
        switch (action) {
            case RUN_DEX: {
                File dex = (File) intent.getSerializableExtra(CompileManager.DEX_FILE);
                if (dex != null) {
                    String mainClass = projectFile.getMainClass().getName();
                    executeDex(in, dex, tempDir, mainClass);
                }
                break;
            }
        }
    }

    private void executeDex(InputStream in, File outDex, File tempDir, String mainClass) {
        String[] args = new String[]{"-jar", outDex.getPath(), mainClass};
        Java.run(args, tempDir.getPath(), in);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");

        mConsoleEditText.stop();
        JavaApplication application = (JavaApplication) getApplication();
        application.removeErrStream(mConsoleEditText.getErrorStream());
        application.removeOutStream(mConsoleEditText.getOutputStream());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void bindView() {
        mConsoleEditText = (ConsoleEditText) findViewById(R.id.console_view);
    }
}
