package com.esr.esense_recorder;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/**
 * Yet another logger.
 */
public class SimpleLogger {
    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "eSenseRecorder-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    private String logFolderName;
    private String logFileName;

    private File logFile;
    private FileOutputStream logFileOutputStream;
    private BufferedWriter logFileWriter;

    public SimpleLogger(String logFolderName, String logFileName) {
        this.logFolderName = logFolderName;
        this.logFileName = logFileName;
    }

    private boolean createLogFile(Context context) {
        try {
            // Close previous log
            closeLog(context);
            // Check storage
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                Log.e(DEBUG_TAG, "SimpleLogger: No external storage for log file.");
                return false;
            }
            // Create log file and directory
            File logDirectory = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), logFolderName);
            if (!logDirectory.exists()) {
                if (!logDirectory.mkdirs()) {
                    Log.e(DEBUG_TAG, "SimpleLogger: Unable to create log directory.");
                    return false;
                }
            }
            logFile = new File(logDirectory, logFileName+".txt");
            int logIdx = 0;
            while (logFile.exists()) {
                logIdx++;
                logFile = new File(logDirectory, logFileName
                        +"("+Integer.toString(logIdx)+").txt");
            }
            logFile.createNewFile();
            logFileOutputStream = new FileOutputStream(logFile);
            logFileWriter = new BufferedWriter(new OutputStreamWriter(logFileOutputStream));
            indexLogFile(context);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "SimpleLogger: Unable to create log file.", e);
            logFileWriter = null;
            logFileOutputStream = null;
            return false;
        }
        return true;
    }

    private void indexLogFile(Context context) {
        if (logFile != null && logFile.exists()) {
            MediaScannerConnection.scanFile(context,
                    new String[]{logFile.getAbsolutePath()},
                    new String[]{"text/plain"}, null);
        }
    }

    public boolean isLogging() {
        return logFileWriter != null;
    }

    public void flushLog() {
        try {
            if (logFileWriter != null) {
                logFileWriter.flush();
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "SimpleLogger: Unable to flush to log file.", e);
        }
    }

    public void closeLog(Context context) {
        try {
            if (logFileWriter != null) {
                logFileWriter.flush();
                logFileWriter.close();
                logFileOutputStream.close();
                indexLogFile(context);
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "SimpleLogger: Unable to close log file.", e);
        }
        logFileWriter = null;
        logFileOutputStream = null;
    }

    public boolean log(Context context, String separator, String terminator, Object... elements) {
        if (logFileWriter == null) {
            if (!createLogFile(context)) {
                return false;
            }
        }
        try {
            for (int i=0; i<elements.length; i++) {
                Object o = elements[i];
                if (o instanceof Number) {
                    logFileWriter.write(((Number)o).toString());
                } else if (o instanceof String) {
                    logFileWriter.write((String)o);
                }
                if (i<elements.length-1) {
                    logFileWriter.write(separator);
                }
            }
            logFileWriter.write(terminator);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "SimpleLogger: Unable to write line.", e);
            return false;
        }
        return true;
    }
}
