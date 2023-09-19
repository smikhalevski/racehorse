import { EventBridge } from './createEventBridge';

/**
 * The managed download description.
 */
export interface Download {
  /**
   * An identifier for a particular download, unique across the system. Clients use this ID to make subsequent calls
   * related to the download.
   */
  id: number;

  /**
   * The client-supplied title for this download. This will be displayed in system notifications.
   *
   * @default ""
   */
  title: string;

  /**
   * The client-supplied description of this download. This will be displayed in system notifications.
   *
   * @default ""
   */
  description: string;

  /**
   * The URI to be downloaded.
   */
  uri: string;

  /**
   * Internet Media Type of the downloaded file. If no value is provided upon creation, this will initially be null and
   * will be filled in based on the server's response once the download has started.
   *
   * @see [RFC 1590, defining Media Types](http://www.ietf.org/rfc/rfc1590.txt)
   */
  mediaType: string;

  /**
   * Total size of the download in bytes. This will initially be -1 and will be filled in once the download starts.
   */
  totalSizeBytes: number;

  /**
   * The URI where downloaded file will be stored. If a destination is supplied by client, that URI will be used here.
   * Otherwise, the value will initially be null and will be filled in with a generated URI once the download has
   * started.
   */
  localUri: string;

  /**
   * Current status of the download.
   */
  status: DownloadStatus;

  /**
   * Provides more detail on the status of the download. Its meaning depends on the value of {@link status}.
   *
   * When {@link status} is {@link DownloadStatus.STATUS_FAILED STATUS_FAILED}, this indicates the type of error that
   * occurred. If an HTTP error occurred, this will hold the HTTP status code as defined in RFC 2616. Otherwise, it will
   * hold one of the {@link DownloadReason ERROR_* constants}.
   *
   * When {@link status} is {@link DownloadStatus.STATUS_PAUSED STATUS_PAUSED}, this indicates why the download is
   * paused. It will hold one of the {@link DownloadReason PAUSED_* constants}.
   *
   * If {@link status} is neither {@link DownloadStatus.STATUS_FAILED STATUS_FAILED} nor
   * {@link DownloadStatus.STATUS_PAUSED STATUS_PAUSED}, this column's value is `null`.
   *
   * @see [RFC 2616 status codes](http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html#sec6.1.1)
   */
  reason: DownloadReason | number;

  /**
   * Number of bytes download so far.
   */
  bytesDownloadedSoFar: number;

  /**
   * Timestamp when the download was last modified (wall clock time in UTC).
   */
  lastModifiedTimestamp: number;
}

/**
 * The status of the managed download.
 */
export const DownloadStatus = {
  /**
   * The download is waiting to start.
   */
  STATUS_PENDING: 1 << 0,

  /**
   * The download is currently running.
   */
  STATUS_RUNNING: 1 << 1,

  /**
   * The download is waiting to retry or resume.
   */
  STATUS_PAUSED: 1 << 2,

  /**
   * The download has successfully completed.
   */
  STATUS_SUCCESSFUL: 1 << 3,

  /**
   * The download has failed (and will not be retried).
   */
  STATUS_FAILED: 1 << 4,
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
   *
   * @hide
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
   * The file name that would be displayed to the user.
   */
  fileName?: string;

  /**
   * The mimetype of the content.
   */
  mimeType?: string;

  /**
   * The title of this download is displayed in notifications. If no title is given, a default one will be assigned
   * based on the download file name, once the download starts.
   */
  title?: string;

  /**
   * Set a description of this download, to be displayed in notifications.
   */
  description?: string;

  /**
   * HTTP headers to be included with the download request.
   */
  headers?: { [name: string]: string | readonly string[] };
}

export interface DownloadManager {
  /**
   * Starts a file download.
   *
   * @param uri The full URI of the content that should be downloaded. Supports HTTP, HTTPS, and data URI.
   * @param options Other download options.
   * @return The ID of the scheduled download or -1 if data URI was instantly written to file.
   */
  download(uri: string, options?: DownloadOptions): number;

  /**
   * Returns a previously started file download by its ID.
   *
   * @param id The ID of the download.
   * @returns The download or `null` if download doesn't exist.
   */
  getDownloadById(id: number): Download | null;

  /**
   * Returns all file downloads.
   */
  getAllDownloads(): Download[];
}

/**
 * Manages file downloads.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createDownloadManager(eventBridge: EventBridge): DownloadManager {
  return {
    download: (url, options) => {
      let headers;

      if (options !== undefined && options.headers !== undefined) {
        headers = Object.assign({}, options.headers);

        for (const key in headers) {
          let value;
          if (headers.hasOwnProperty(key) && typeof (value = headers[key]) === 'string') {
            headers[key] = [value];
          }
        }
      }

      const payload = Object.assign({}, options, { url, headers });

      return eventBridge.request({ type: 'org.racehorse.DownloadEvent', payload }).payload.id;
    },

    getDownloadById: id =>
      eventBridge.request({ type: 'org.racehorse.GetDownloadByIdEvent', payload: { id } }).payload.download,

    getAllDownloads: () => eventBridge.request({ type: 'org.racehorse.GetAllDownloadsEvent' }).payload.downloads,
  };
}
