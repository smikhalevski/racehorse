import React, { Fragment, useEffect, useState } from 'react';
import { activityManager, downloadManager, DownloadStatus, Intent } from 'racehorse';

const TEST_HTTP_URL = 'https://upload.wikimedia.org/wikipedia/en/f/f9/Death_star1.png';

const TEST_DATA_URI =
  'data:image/gif;base64,R0lGODlhEAAQAMQAAORHHOVSKudfOulrSOp3WOyDZu6QdvCchPGolfO0o/XBs/fNwfjZ0frl3/zy7////wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACH5BAkAABAALAAAAAAQABAAAAVVICSOZGlCQAosJ6mu7fiyZeKqNKToQGDsM8hBADgUXoGAiqhSvp5QAnQKGIgUhwFUYLCVDFCrKUE1lBavAViFIDlTImbKC5Gm2hB0SlBCBMQiB0UjIQA7';

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
      <h2>{'Download'}</h2>

      <p>
        <button
          onClick={() => {
            addDownload(TEST_HTTP_URL);
          }}
        >
          {'Download HTTP URL'}
        </button>
      </p>
      <p>
        <button
          onClick={() => {
            addDownload(TEST_DATA_URI);
          }}
        >
          {' Download data URI'}
        </button>
      </p>
      <p>
        <a
          href={TEST_HTTP_URL}
          download={true}
        >
          {'HTTP link'}
        </a>
      </p>
      <p>
        <a
          href={TEST_DATA_URI}
          download={true}
        >
          {'URI link'}
        </a>
      </p>

      <form
        onSubmit={event => {
          event.preventDefault();
          addDownload(uri);
        }}
      >
        <p>
          <input
            value={uri}
            onChange={event => {
              setURI(event.target.value);
            }}
            required={true}
          />{' '}
          <button>{'Add download'}</button>
        </p>
      </form>

      {downloads.map(download => {
        const handleDeleteDownload = () => {
          downloadManager.removeDownload(download.id);
          setDownloads(downloadManager.getAllDownloads());
        };

        const handlePreviewDownload = () => {
          activityManager.startActivity({
            action: Intent.ACTION_VIEW,
            flags: Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION,
            data: download.contentUri,
          });
        };

        return (
          <ol
            key={download.id}
            start={download.id}
          >
            <li>
              <a
                href={download.status === DownloadStatus.SUCCESSFUL ? '#' : undefined}
                onClick={handlePreviewDownload}
              >
                {download.title}
              </a>
              {
                {
                  [DownloadStatus.PENDING]: ' ⬇️',
                  [DownloadStatus.RUNNING]: ' ⬇️ ' + (((download.totalSize / download.downloadedSize) * 100) | 0) + '%',
                  [DownloadStatus.PAUSED]: ' ⏸',
                  [DownloadStatus.SUCCESSFUL]: '',
                  [DownloadStatus.FAILED]: ' 🔴',
                }[download.status]
              }
              <p>
                <button onClick={handleDeleteDownload}>{'❌ Delete'}</button>
              </p>
            </li>
          </ol>
        );
      })}
    </>
  );
}
