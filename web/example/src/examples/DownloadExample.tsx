import React, { MouseEventHandler, useEffect, useState } from 'react';
import { activityManager, downloadManager, DownloadStatus, Intent } from 'racehorse';

const TEST_HTTP_URL = 'https://upload.wikimedia.org/wikipedia/en/f/f9/Death_star1.png';

const TEST_DATA_URI =
  'data:image/gif;base64,R0lGODlhBwAGAJEAAAAAAP////RDNv///yH/C05FVFNDQVBFMi4wAwEAAAAh+QQFAAADACwAAAAABwAGAAACCpxkeMudOyKMkhYAOw==';

export function DownloadExample() {
  const [uri, setURI] = useState(TEST_HTTP_URL);
  const [downloads, setDownloads] = useState(downloadManager.getAllDownloads);

  useEffect(() => {
    const timer = setInterval(() => {
      setDownloads(downloadManager.getAllDownloads());
    }, 1000);

    return () => {
      clearInterval(timer);
    };
  });

  const addDownload = (uri: string) => {
    downloadManager.addDownload(uri, { headers: { 'Example-Header': 'example' } }).then(() => {
      setDownloads(downloadManager.getAllDownloads());
    });
  };

  return (
    <>
      {'Quick actions'}
      <div className="d-flex gap-2 mb-3">
        <button
          className="btn btn-outline-primary"
          onClick={() => {
            addDownload(TEST_HTTP_URL);
          }}
        >
          {'HTTP URL'}
        </button>

        <button
          className="btn btn-outline-primary"
          onClick={() => {
            addDownload(TEST_DATA_URI);
          }}
        >
          {'Data URI'}
        </button>
      </div>
      <form
        className="input-group mb-3"
        onSubmit={event => {
          event.preventDefault();
          addDownload(uri);
        }}
      >
        <input
          className="form-control"
          value={uri}
          onChange={event => {
            setURI(event.target.value);
          }}
          required={true}
        />
        <button className="btn btn-primary">{'Add download'}</button>
      </form>
      <div className="list-group mb-3">
        {downloads.length === 0 && (
          <div className="list-group-item list-group-item-light text-secondary text-center">{'No downloads'}</div>
        )}

        {downloads.map(download => {
          const handleDeleteDownload: MouseEventHandler = event => {
            event.stopPropagation();
            downloadManager.removeDownload(download.id);
            setDownloads(downloadManager.getAllDownloads());
          };

          const handleOpenPreview = () => {
            activityManager.startActivity({
              action: Intent.ACTION_VIEW,
              flags: Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION,
              data: download.contentUri,
            });
          };

          return (
            <div
              key={download.id}
              className="list-group-item list-group-item-action d-flex align-items-center"
              onClick={handleOpenPreview}
            >
              <div className="flex-fill text-nowrap overflow-hidden me-3">
                {
                  {
                    [DownloadStatus.PENDING]: <i className="spinner-border spinner-border-sm me-3" />,
                    [DownloadStatus.RUNNING]: <i className="spinner-border spinner-border-sm me-3" />,
                    [DownloadStatus.PAUSED]: <i className="bi-pause-circle-fill text-warning me-3" />,
                    [DownloadStatus.SUCCESSFUL]: <i className="bi-check-circle-fill text-success me-3" />,
                    [DownloadStatus.FAILED]: <i className="bi-x-circle-fill text-danger me-3" />,
                  }[download.status]
                }
                {download.title}
              </div>
              <button
                className="btn btn-sm btn-outline-primary"
                onClick={handleDeleteDownload}
              >
                {'Delete'}
              </button>
            </div>
          );
        })}
      </div>
      <a
        href={TEST_HTTP_URL}
        download={true}
      >
        {'HTTP URL'}
      </a>{' '}
      <a
        href={TEST_DATA_URI}
        download={true}
      >
        {'Data URI'}
      </a>
    </>
  );
}
