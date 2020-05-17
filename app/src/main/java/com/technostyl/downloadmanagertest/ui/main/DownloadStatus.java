package com.technostyl.downloadmanagertest.ui.main;

class DownloadStatus {

    static enum DownloadState{
        DOWNLOAD_GIVEN_TO_DOWNLOAD_MANAGER,
        DOWNLOAD_PENDING_WITH_DOWNLOAD_MANAGER,
        DOWNLOAD_RUNNING,
        DOWNLOAD_PAUSED,
        DOWNLOAD_COMPLETE,
        DOWNLOAD_FAILED
    };

    private long downloadId;
    private String downloadUrl;
    private DownloadState currentDownloadState;
    private long totalBytes;
    private long bytesDownloaded;
    private float progress;
    private long lastModifiedTimestamp;
    private String mediaType;
    private String humanReadableReason;
    private String targetUri;

    public String getTargetUri() {
        return targetUri;
    }

    public void setTargetUri(String targetUri) {
        this.targetUri = targetUri;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

    public void setBytesDownloaded(long bytesDownloaded) {
        this.bytesDownloaded = bytesDownloaded;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public void setLastModifiedTimestamp(long lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getHumanReadableReason() {
        return humanReadableReason;
    }

    public void setHumanReadableReason(String humanReadableReason) {
        this.humanReadableReason = humanReadableReason;
    }

    public long getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(long downloadId) {
        this.downloadId = downloadId;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public DownloadState getCurrentDownloadState() {
        return currentDownloadState;
    }

    public void setCurrentDownloadState(DownloadState currentDownloadState) {
        this.currentDownloadState = currentDownloadState;
    }
}
