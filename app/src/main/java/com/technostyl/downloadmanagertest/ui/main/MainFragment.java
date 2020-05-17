package com.technostyl.downloadmanagertest.ui.main;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.technostyl.downloadmanagertest.R;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class MainFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = MainFragment.class.getSimpleName();
    //UI components
    private EditText urlEditText;
    private Button dldStartButton;
    private TextView statusTextView;
    private Button refreshStatusButton;
    private Button showDownloadsButton;
    private Button grantExternalWritePermissionsButton;

    private MainViewModel mViewModel;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.main_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dldStartButton = view.findViewById(R.id.download_button);
        urlEditText = view.findViewById(R.id.urlText);
        statusTextView = view.findViewById(R.id.statusMessage);
        refreshStatusButton = view.findViewById(R.id.refresh_button);
        showDownloadsButton = view.findViewById(R.id.show_downloads_button);
        grantExternalWritePermissionsButton = view.findViewById(R.id.grant_permission_button);

        dldStartButton.setOnClickListener(this);
        refreshStatusButton.setOnClickListener(this);
        showDownloadsButton.setOnClickListener(this);
        grantExternalWritePermissionsButton.setOnClickListener(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);



        mViewModel = ViewModelProviders.of(this).get(MainViewModel.class);
        DownloadManager downloadManager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        // TODO: Use the ViewModel
        mViewModel.setDownloadManager(downloadManager);
        mViewModel.getDownloadStatusHolder().observe(this, new Observer<DownloadStatus>() {
            @Override
            public void onChanged(DownloadStatus downloadStatus) {
                // update UI
                updateDownloadStatusInStatusTextView(downloadStatus);
            }
        });
    }

    private void updateDownloadStatusInStatusTextView(DownloadStatus downloadStatus) {

        downloadStatus.getLastModifiedTimestamp();


        StringBuilder sb = new StringBuilder();
        // id
        sb.append("Download ID = ").append(downloadStatus.getDownloadId()).append('\n');
        // url
        String urlToDisplay = downloadStatus.getDownloadUrl().length() >200 ? downloadStatus.getDownloadUrl().substring(0, 200) : downloadStatus.getDownloadUrl();
        sb.append("Download URL = ").append(urlToDisplay).append('\n');
        // media type
        sb.append("Media Type = ").append(downloadStatus.getMediaType()).append("\n\n");
        // current status
        sb.append("Current State = ").append(downloadStatus.getCurrentDownloadState()).append('\n');
        if(downloadStatus.getCurrentDownloadState() == DownloadStatus.DownloadState.DOWNLOAD_FAILED || downloadStatus.getCurrentDownloadState() == DownloadStatus.DownloadState.DOWNLOAD_PAUSED){
            sb.append("Reason = ").append(downloadStatus.getHumanReadableReason()).append('\n');
        }
        else if (downloadStatus.getCurrentDownloadState() == DownloadStatus.DownloadState.DOWNLOAD_COMPLETE){
            sb.append("Target Location = ").append(downloadStatus.getTargetUri());
        }


        // progress
        String downloadedBytesReadableStr = android.text.format.Formatter.formatShortFileSize(getContext(), downloadStatus.getBytesDownloaded());
        String totalBytesReadableStr = android.text.format.Formatter.formatShortFileSize(getContext(), downloadStatus.getTotalBytes());
        String percentageDownloaded = "" + (downloadStatus.getProgress() * 100f) + " %";

        sb.append("Downloaded ").append(downloadedBytesReadableStr).append( " of ").append(totalBytesReadableStr).append(" - ").append(percentageDownloaded).append('\n');
        String formattedLastModfiedDateTime = android.text.format.DateUtils.formatDateTime(getContext(), downloadStatus.getLastModifiedTimestamp(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
        sb.append("as on ").append(formattedLastModfiedDateTime);

        String finalStringToDisplay = sb.toString();
        statusTextView.setText(finalStringToDisplay);

    }

    @Override
    public void onResume() {
        super.onResume();
        // lets ask for permissions if we still dont have
        // we keep asking continuously until we get it
        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onClick(View v) {
        if(v == dldStartButton){
            if(urlEditTextHasValidUrl()){
                String urlString = urlEditText.getText().toString();
                mViewModel.startDownloadWithUrl(urlString, this.getContext());
            }
            else{
                Toast.makeText(this.getContext(), "Invalid URL", Toast.LENGTH_LONG).show();
            }
        }
        else if(v == refreshStatusButton){
            mViewModel.queryAndUpdateStatus();
        }
        else if (v == showDownloadsButton){
            if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                try {
                    Intent i = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
                    startActivity(i);
                } catch (Exception e) {
                    Log.e(TAG, "exception when opening all downloads", e);
                }
            }
            else {
                File f = getContext().getExternalFilesDir(null);
                Uri uri = Uri.fromFile(f);
                // in this case we just say where we are downloading
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext());
                alertBuilder.setTitle("Download Location")
                        .setCancelable(false)
                        .setMessage("Downloads are in the location - " + uri.toString())
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                //finish();
                            }
                        }).show();
            }
        }
        else if(v == grantExternalWritePermissionsButton){
            // lets ask for permissions if we still dont have
            if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
            else {
                Toast.makeText(getContext(), "Write External Storage permisison already granted", Toast.LENGTH_SHORT);
            }
        }
    }

    private boolean urlEditTextHasValidUrl() {

        String urlString = urlEditText.getText().toString();

        boolean retVal = true;
        try {
            new URL(urlString).toURI();
        } catch (MalformedURLException | URISyntaxException e){
            retVal = false;
        }
        return retVal;
    }
}
