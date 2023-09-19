import React, { useEffect, useState } from 'react';
import { downloadManager, DownloadStatus } from 'racehorse';

const downloadStatusMessages = {
  [DownloadStatus.STATUS_PENDING]: 'pending',
  [DownloadStatus.STATUS_RUNNING]: 'running',
  [DownloadStatus.STATUS_PAUSED]: 'paused',
  [DownloadStatus.STATUS_SUCCESSFUL]: 'successful',
  [DownloadStatus.STATUS_FAILED]: 'failed',
};

const IMAGE_URL = 'https://upload.wikimedia.org/wikipedia/en/f/f9/Death_star1.png';

const DATA_URI =
  'data:image/gif;base64,R0lGODlhEAAQAMQAAORHHOVSKudfOulrSOp3WOyDZu6QdvCchPGolfO0o/XBs/fNwfjZ0frl3/zy7////wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACH5BAkAABAALAAAAAAQABAAAAVVICSOZGlCQAosJ6mu7fiyZeKqNKToQGDsM8hBADgUXoGAiqhSvp5QAnQKGIgUhwFUYLCVDFCrKUE1lBavAViFIDlTImbKC5Gm2hB0SlBCBMQiB0UjIQA7';

export function DownloadExample() {
  const [title, setTitle] = useState('');
  const [uri, setUri] = useState('');
  const [downloads, setDownloads] = useState(downloadManager.getAllDownloads);

  useEffect(() => {
    const timer = setInterval(() => {
      setDownloads(downloadManager.getAllDownloads());
    }, 500);

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
      >
        <thead>
          <tr>
            <th>{'Title'}</th>
            <th>{'Status'}</th>
            <th>{'%'}</th>
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
              <td>{download.title}</td>
              <td>{downloadStatusMessages[download.status]}</td>
              <td align={'right'}>
                {download.bytesDownloadedSoFar === 0
                  ? '–'
                  : Math.max(0, (download.totalSizeBytes / download.bytesDownloadedSoFar) * 100) + '%'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <p>
        {'Title: '}
        <input
          value={title}
          onChange={event => {
            setTitle(event.target.value);
          }}
        />
      </p>

      <p>
        {'URI: '}
        <input
          value={uri}
          onChange={event => {
            setUri(event.target.value);
          }}
        />
      </p>

      <p>
        <a
          href={'#'}
          onClick={event => {
            event.preventDefault();
            setUri(IMAGE_URL);
          }}
        >
          {'Try image URL'}
        </a>
      </p>

      <p>
        <a
          href={'#'}
          onClick={event => {
            event.preventDefault();
            setUri(DATA_URI);
          }}
        >
          {'Try data URI'}
        </a>
      </p>

      <button
        onClick={() => {
          downloadManager.download(uri, { title, headers: { 'My-Header': 'wow' } });
        }}
      >
        {'Start download'}
      </button>
    </>
  );
}
