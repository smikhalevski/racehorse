import React, { useEffect, useState } from 'react';
import { activityManager, downloadManager, DownloadStatus, Intent } from 'racehorse';

const TEST_HTTP_URL = 'https://upload.wikimedia.org/wikipedia/en/f/f9/Death_star1.png';

const TEST_DATA_URI =
  'data:image/gif;base64,R0lGODlhEAAQAMQAAORHHOVSKudfOulrSOp3WOyDZu6QdvCchPGolfO0o/XBs/fNwfjZ0frl3/zy7////wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACH5BAkAABAALAAAAAAQABAAAAVVICSOZGlCQAosJ6mu7fiyZeKqNKToQGDsM8hBADgUXoGAiqhSvp5QAnQKGIgUhwFUYLCVDFCrKUE1lBavAViFIDlTImbKC5Gm2hB0SlBCBMQiB0UjIQA7';

export function DownloadExample() {
  const [uri, setUri] = useState(TEST_HTTP_URL);
  const [downloads, setDownloads] = useState(downloadManager.getAllDownloads);

  useEffect(() => {
    const timer = setInterval(() => {
      setDownloads(downloadManager.getAllDownloads());
    }, 200);

    return () => {
      clearInterval(timer);
    };
  });

  return (
    <>
      <h2>{'Download'}</h2>

      <table
        border={1}
        cellPadding={7}
        width={'100%'}
      >
        <thead>
          <tr>
            <th>{'Title'}</th>
            <th>{'Status'}</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {downloads.length === 0 && (
            <tr>
              <td colSpan={3}>
                <center>{'No downloads'}</center>
              </td>
            </tr>
          )}

          {downloads.map(download => (
            <tr key={download.id}>
              <td width={'100%'}>
                <a
                  href={download.status === DownloadStatus.SUCCESSFUL ? '#' : undefined}
                  onClick={event => {
                    event.preventDefault();

                    if (download.contentUri !== null) {
                      activityManager.startActivity({
                        action: Intent.ACTION_VIEW,
                        flags: Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        data: download.contentUri,
                      });
                    }
                  }}
                >
                  {decodeURI(download.localUri?.split('/').pop() || '')}
                </a>
              </td>

              <td align={'center'}>
                {
                  {
                    [DownloadStatus.PENDING]: '',
                    [DownloadStatus.RUNNING]: (((download.totalSize / download.downloadedSize) * 100) | 0) + '%',
                    [DownloadStatus.PAUSED]: '🟡',
                    [DownloadStatus.SUCCESSFUL]: '🟢',
                    [DownloadStatus.FAILED]: '🔴',
                  }[download.status]
                }
              </td>

              <td align={'center'}>
                <a
                  onClick={event => {
                    event.preventDefault();
                    downloadManager.removeDownload(download.id);
                  }}
                >
                  {'❌'}
                </a>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <p>
        <input
          value={uri}
          onChange={event => {
            setUri(event.target.value);
          }}
        />{' '}
        <button
          onClick={() => {
            downloadManager.addDownload(uri, { headers: { 'Example-Header': 'example' } });
          }}
        >
          {'Add download'}
        </button>
      </p>

      <p>
        {'Use '}
        <a
          href={'#'}
          onClick={event => {
            event.preventDefault();
            setUri(TEST_HTTP_URL);
          }}
        >
          {'HTTP URL'}
        </a>
        {' or '}
        <a
          href={'#'}
          onClick={event => {
            event.preventDefault();
            setUri(TEST_DATA_URI);
          }}
        >
          {'data URI'}
        </a>
      </p>

      <hr />

      <p>
        {'Downloadable '}
        <a
          href={TEST_DATA_URI}
          download={true}
        >
          {'data URI link'}
        </a>
      </p>
    </>
  );
}
