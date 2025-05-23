import { Event, EventBridge } from './createEventBridge.js';

/**
 * The download description.
 */
export interface Download {
  /**
   * The identifier for a particular download, unique across the system. Clients use this ID to make subsequent calls
   * related to the download.
   */
  id: number;

  /**
   * Current status of the download.
   */
  status: DownloadStatus;

  /**
   * Provides more detail on the status of the download. Its meaning depends on the value of {@link status}.
   *
   * When {@link status} is {@link DownloadStatus.FAILED FAILED}, this indicates the type of error that occurred. If an
   * HTTP error occurred, this will hold the HTTP status code as defined in
   * [RFC 2616](http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html#sec6.1.1). Otherwise, it will hold one of the
   * {@link DownloadReason ERROR_* constants}.
   *
   * When {@link status} is {@link DownloadStatus.PAUSED PAUSED}, this indicates why the download is paused. It will
   * hold one of the {@link DownloadReason PAUSED_* constants}.
   *
   * If {@link status} is neither {@link DownloadStatus.FAILED FAILED} nor
   * {@link DownloadStatus.PAUSED PAUSED}, this column's value is 0.
   */
  reason: DownloadReason | number;

  /**
   * The URI to be downloaded.
   */
  uri: string;

  /**
   * The URI where downloaded file will be stored. If a destination is supplied by client, that URI will be used here.
   * Otherwise, the value will initially be null and will be filled in with a generated URI once the download has
   * started.
   */
  localUri: string | null;

  /**
   * The `content:` URI of the downloaded file that can be passed to the {@link ActivityManager.startActivity intent}.
   */
  contentUri: string | null;

  /**
   * The MIME type of the downloaded file.
   */
  mimeType: string | null;

  /**
   * Total size of the download in bytes. This will initially be -1 and will be filled in once the download starts.
   */
  totalSize: number;

  /**
   * The number of bytes download so far.
   */
  downloadedSize: number;

  /**
   * A timestamp when the download was last modified (wall clock time in UTC).
   */
  lastModifiedTime: number;

  /**
   * The client-supplied title for this download.
   */
  title: string;
}

/**
 * The status of the managed download.
 */
export const DownloadStatus = {
  /**
   * The download is waiting to start.
   */
  PENDING: 1,

  /**
   * The download is currently running.
   */
  RUNNING: 2,

  /**
   * The download is waiting to retry or resume.
   */
  PAUSED: 4,

  /**
   * The download has successfully completed.
   */
  SUCCESSFUL: 8,

  /**
   * The download has failed (and will not be retried).
   */
  FAILED: 16,
} as const;

/**
 * The status of the managed download.
 */
export type DownloadStatus = (typeof DownloadStatus)[keyof typeof DownloadStatus];

/**
 * The status of the managed download.
 */
export const DownloadReason = {
  /**
   * Reason when a storage issue arises which doesn't fit under any other error code. Use the more specific
   * {@link ERROR_INSUFFICIENT_SPACE} and {@link ERROR_DEVICE_NOT_FOUND} when appropriate.
   */
  ERROR_FILE_ERROR: 1001,

  /**
   * Reason when an HTTP code was received that download manager can't handle.
   */
  ERROR_UNHANDLED_HTTP_CODE: 1002,

  /**
   * Reason when an error receiving or processing data occurred at the HTTP level.
   */
  ERROR_HTTP_DATA_ERROR: 1004,

  /**
   * Reason when there were too many redirects.
   */
  ERROR_TOO_MANY_REDIRECTS: 1005,

  /**
   * Reason when there was insufficient storage space. Typically, this is because the SD card is full.
   */
  ERROR_INSUFFICIENT_SPACE: 1006,

  /**
   * Reason when no external storage device was found. Typically, this is because the SD card is not mounted.
   */
  ERROR_DEVICE_NOT_FOUND: 1007,

  /**
   * Reason when some possibly transient error occurred, but we can't resume the download.
   */
  ERROR_CANNOT_RESUME: 1008,

  /**
   * Reason when the requested destination file already exists (the download manager will not overwrite an existing
   * file).
   */
  ERROR_FILE_ALREADY_EXISTS: 1009,

  /**
   * Reason when the download has failed because of network policy manager controls on the requesting application.
   */
  ERROR_BLOCKED: 1010,

  /**
   * Reason when the download is paused because some network error occurred and the download manager is waiting before
   * retrying the request.
   */
  PAUSED_WAITING_TO_RETRY: 1,

  /**
   * Reason when the download is waiting for network connectivity to proceed.
   */
  PAUSED_WAITING_FOR_NETWORK: 2,

  /**
   * Reason when the download exceeds a size limit for downloads over the mobile network and the download manager is
   * waiting for a Wi-Fi connection to proceed.
   */
  PAUSED_QUEUED_FOR_WIFI: 3,

  /**
   * Reason when the download is paused for some other reason.
   */
  PAUSED_UNKNOWN: 4,
} as const;

export type DownloadReason = (typeof DownloadReason)[keyof typeof DownloadReason];

/**
 * Options of the download.
 */
export interface DownloadOptions {
  /**
   * The file name that would be displayed to the user. If no file name is provided, then it would be derived from the
   * URI and/or {@link mimeType}.
   */
  fileName?: string;

  /**
   * The MIME type of the downloaded file. If the downloaded URI is a data URI which has a MIME type, then this option
   * is ignored.
   */
  mimeType?: string;

  /**
   * HTTP headers to be included with the download request.
   */
  headers?: HeadersInit;
}

export interface DownloadManager {
  /**
   * Adds a new download.
   *
   * The returned promise may be rejected if some of the download options are invalid, or if download cannot be
   * performed due to platform-dependent reasons.
   *
   * @param uri The full URI of the content that should be downloaded. Supports HTTP, HTTPS, and data URI.
   * @param options The download options.
   * @return The ID of the download.
   * @throws PermissionRequiredException
   * @throws IllegalStateException Unsupported URI.
   */
  addDownload(uri: string, options?: DownloadOptions): Promise<number>;

  /**
   * Returns a previously added download.
   *
   * @param id The ID of the download.
   * @returns The download or `null` if download doesn't exist.
   */
  getDownload(id: number): Download | null;

  /**
   * Returns all available downloads.
   */
  getAllDownloads(): Download[];

  /**
   * Cancel a download and remove it from the download manager. Download will be stopped if it was running, and it will
   * no longer be accessible through the download manager. If there is a downloaded file, partial or complete, it is
   * deleted.
   *
   * @param id The ID of the download.
   * @returns `true` if the download was deleted, or `false` otherwise.
   */
  removeDownload(id: number): boolean;
}

/**
 * Allows starting and monitoring file downloads.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createDownloadManager(eventBridge: EventBridge): DownloadManager {
  return {
    addDownload: (uri, options) =>
      new Promise<Event>(resolve => {
        const headers = Array.from(new Headers(options?.headers));
        const payload = Object.assign({}, options, { uri, headers });

        resolve(eventBridge.requestAsync({ type: 'org.racehorse.AddDownloadEvent', payload }));
      }).then(event => event.payload.id),

    getDownload: id =>
      eventBridge.request({ type: 'org.racehorse.GetDownloadEvent', payload: { id } }).payload.download,

    getAllDownloads: () => eventBridge.request({ type: 'org.racehorse.GetAllDownloadsEvent' }).payload.downloads,

    removeDownload: id =>
      eventBridge.request({ type: 'org.racehorse.RemoveDownloadEvent', payload: { id } }).payload.isRemoved,
  };
}
