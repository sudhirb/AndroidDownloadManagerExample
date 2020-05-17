package com.technostyl.downloadmanagertest.ui.main;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {

    private static final String TAG = MainViewModel.class.getSimpleName();
    private DownloadManager downloadManager;
    private Context mContext;

    private MutableLiveData<DownloadStatus> downloadStatusHolder = new MutableLiveData<DownloadStatus>();
    private Cursor mDownloadManagerCursor;
    private ContentObserver mDownloadFileObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            queryAndUpdateStatus();


        }

    };

    public void queryAndUpdateStatus() {
        // first do we have an active download being observed
        DownloadStatus dldStatus = downloadStatusHolder.getValue();
        if(dldStatus != null) {
            Cursor cursor = queryDownloadManagerForCurrentDownload();
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        long downloadId = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
                        if (dldStatus.getDownloadId() == downloadId) {
                            long bytesDownloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            long totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                            int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                            long lastModifiedTimestamp = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP));
                            String mediaType = "" + cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));
                            int reasonCode = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                            String uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI));

                            dldStatus.setDownloadUrl(uri);
                            dldStatus.setBytesDownloaded(bytesDownloaded);
                            dldStatus.setTotalBytes(totalBytes);
                            dldStatus.setLastModifiedTimestamp(lastModifiedTimestamp);
                            dldStatus.setMediaType(mediaType);

                            float progress = -1;
                            if (totalBytes > 0) {
                                progress = bytesDownloaded / totalBytes;
                            }

                            dldStatus.setProgress(progress);

                            if (progress >= 1.0 || downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {
                                dldStatus.setTargetUri(downloadManager.getUriForDownloadedFile(downloadId).toString());
                                updateDownloadStatusAsComplete(downloadId, dldStatus);
                            } else if (downloadStatus == DownloadManager.STATUS_FAILED) {
                                dldStatus.setHumanReadableReason(getHumanReadableDownloadFailReason(reasonCode));
                                updateDownloadStatusAsFailed(downloadId, dldStatus);
                            } else if (downloadStatus == DownloadManager.STATUS_PAUSED) {
                                dldStatus.setHumanReadableReason(getHumanReadableDownloadPauseReason(reasonCode));
                                updateDownloadStatusAsPaused(downloadId, dldStatus);
                            } else if(downloadStatus == DownloadManager.STATUS_PENDING){
                                updateDownloadStatusAsPending(downloadId, dldStatus);
                            } else if(downloadStatus == DownloadManager.STATUS_RUNNING){
                                updateDownloadStatusAsRunning(downloadId, dldStatus);
                            }

                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        }
    }

    private String getHumanReadableDownloadPauseReason(int reasonCode) {
        String retVal = "NONE";
        switch (reasonCode) {
            case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
                retVal = "large file, paused and waiting for wifi";
                break;
            case DownloadManager.PAUSED_UNKNOWN:
                retVal = "paused due to unknown reason";
                break;
            case DownloadManager.PAUSED_WAITING_FOR_NETWORK:
                retVal = "paused, waiting for entwork connectivity";
                break;
            case DownloadManager.PAUSED_WAITING_TO_RETRY:
                retVal = "network error, paused and waiting to retry";
                break;
            default:
                break;
        }
        return retVal;
    }

    private String getHumanReadableDownloadFailReason(int reasonCode) {
        String retVal = "NONE";

        switch (reasonCode) {
            case DownloadManager.ERROR_CANNOT_RESUME:
                retVal = "Cannot resume the download";
                break;
            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                retVal = "external strorage not found";
                break;
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                retVal = "destination file already exsits";
                break;
            case DownloadManager.ERROR_FILE_ERROR:
                retVal = "unknown disk issue";
                break;
            case DownloadManager.ERROR_HTTP_DATA_ERROR:
                retVal = "error receiving or porcessing data at http level";
                break;
            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                retVal = "storage space not available";
                break;
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                retVal = "too many redirects";
                break;
            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                retVal = "stupid http code obtained";
                break;
            case DownloadManager.ERROR_UNKNOWN:
                retVal = "unknown error";
                break;
            default:
                // if the reason code is a valid http status code
                if(reasonCode >= 100 && reasonCode <=599) {
                    retVal = "HTTP status code " + reasonCode;
                }
                break;
        }

        return retVal;
    }

    private Cursor queryDownloadManagerForCurrentDownload() {


        Cursor cursor = null;

        if (downloadStatusHolder.getValue() != null) {
            long currentDownloadId = downloadStatusHolder.getValue().getDownloadId();
            cursor = downloadManager.query(new DownloadManager.Query()
                    .setFilterById(currentDownloadId));
            Log.d(TAG,"queryDownloadManagerForCurrentDownload currentDownloadIds = "+currentDownloadId);
        }

        return cursor;
    }

    private void setupContentObserver() {
        tearDownContentObserver();

        mDownloadManagerCursor = queryDownloadManagerForCurrentDownload();
        if (mDownloadManagerCursor != null) {
            mDownloadManagerCursor.registerContentObserver(mDownloadFileObserver);
            Log.d(TAG,"setupContentObserver registerContentObserver called ");
        }
    }

    private void tearDownContentObserver() {
        if (mDownloadManagerCursor != null) {
            try {
                mDownloadManagerCursor.unregisterContentObserver(mDownloadFileObserver);
                Log.d(TAG,"tearDownContentObserver unregisterContentObserver called ");
            } catch (Exception ignored) {
            }
            mDownloadManagerCursor.close();
            mDownloadManagerCursor = null;
        }
        if(mContext != null){
            try{
                mContext.unregisterReceiver(downloadReceiver);
            }
            catch (Exception e){
                Log.e(TAG, "Excption when unregistering broadcast reeciver", e);
            }
        }
    }

    public void setDownloadManager(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    public void startDownloadWithUrl(String urlString, Context context) {
        Log.d(TAG, "Downloading-^^^"+urlString+"^^^END");
        context.registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        Uri uri = Uri.parse(urlString);
        final DownloadManager.Request dldRequest= new DownloadManager.Request(uri);
        dldRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        dldRequest.setDescription("Test Download");

        String subPath = uri.getLastPathSegment() == null ? "file" : uri.getLastPathSegment();
        dldRequest.setTitle(subPath);
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // we have external write permission, lets download in the default downloads dor
            dldRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, subPath);
        }
        else {
            // we dont have external write permissions, lets download in our own private external dir
            dldRequest.setDestinationInExternalFilesDir(context, null, subPath);
        }
        long downloadId = downloadManager.enqueue(dldRequest);
        DownloadStatus dldStatus = new DownloadStatus();
        dldStatus.setDownloadId(downloadId);
        dldStatus.setCurrentDownloadState(DownloadStatus.DownloadState.DOWNLOAD_GIVEN_TO_DOWNLOAD_MANAGER);
        dldStatus.setDownloadUrl(urlString);
        downloadStatusHolder.setValue(dldStatus);
        setupContentObserver();
        mContext = context.getApplicationContext();
    }

    public MutableLiveData<DownloadStatus> getDownloadStatusHolder() {
        return downloadStatusHolder;
    }

    private void checkAndCompleteDownloadIfDone(long downloadRefenceId, DownloadStatus dldStatus){
        if(dldStatus == null) {
            dldStatus = downloadStatusHolder.getValue();
        }

        if(dldStatus != null) {

            if (downloadRefenceId == dldStatus.getDownloadId()) {
                queryAndUpdateStatus();
                /*
                dldStatus.setTargetUri(downloadManager.getUriForDownloadedFile(downloadRefenceId).toString());
                dldStatus.setCurrentDownloadState(DownloadStatus.DownloadState.DOWNLOAD_COMPLETE);
                downloadStatusHolder.postValue(dldStatus);
                tearDownContentObserver();
                */
                Log.d(TAG, "download completed");
            }
        }
    }

    private void updateDownloadStatusAsFailed(long downloadRefenceId, DownloadStatus dldStatus){
        if(dldStatus == null) {
            dldStatus = downloadStatusHolder.getValue();
        }
        if(dldStatus != null) {
            if (downloadRefenceId == dldStatus.getDownloadId()) {
                dldStatus.setCurrentDownloadState(DownloadStatus.DownloadState.DOWNLOAD_FAILED);
                downloadStatusHolder.postValue(dldStatus);
                tearDownContentObserver();
                Log.d(TAG, "download failed");
            }
        }
    }

    private void updateDownloadStatusAsPending(long downloadId, DownloadStatus dldStatus) {
        if(dldStatus == null) {
            dldStatus = downloadStatusHolder.getValue();
        }
        if(dldStatus != null) {
            dldStatus.setCurrentDownloadState(DownloadStatus.DownloadState.DOWNLOAD_PENDING_WITH_DOWNLOAD_MANAGER);
            downloadStatusHolder.postValue(dldStatus);
        }
    }

    private void updateDownloadStatusAsPaused(long downloadId, DownloadStatus dldStatus) {
        if(dldStatus == null) {
            dldStatus = downloadStatusHolder.getValue();
        }
        if(dldStatus != null) {
            dldStatus.setCurrentDownloadState(DownloadStatus.DownloadState.DOWNLOAD_PAUSED);
            downloadStatusHolder.postValue(dldStatus);
        }
    }

    private void updateDownloadStatusAsRunning(long downloadId, DownloadStatus dldStatus) {
        if(dldStatus == null) {
            dldStatus = downloadStatusHolder.getValue();
        }
        if(dldStatus != null) {
            dldStatus.setCurrentDownloadState(DownloadStatus.DownloadState.DOWNLOAD_RUNNING);
            downloadStatusHolder.postValue(dldStatus);
        }
    }

    private void updateDownloadStatusAsComplete(long downloadId, DownloadStatus dldStatus) {
        if(dldStatus == null) {
            dldStatus = downloadStatusHolder.getValue();
        }
        if(dldStatus != null) {
            dldStatus.setCurrentDownloadState(DownloadStatus.DownloadState.DOWNLOAD_COMPLETE);
            dldStatus.setTargetUri(downloadManager.getUriForDownloadedFile(downloadId).toString());
            downloadStatusHolder.postValue(dldStatus);
        }
    }

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            //check if the broadcast message is for our enqueued download
            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

            checkAndCompleteDownloadIfDone(referenceId, null);

        }
    };
}
