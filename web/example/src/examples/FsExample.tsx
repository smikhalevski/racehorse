import React, { useEffect, useState } from 'react';
import { Directory, File, fsManager } from 'racehorse';

export function FsExample() {
  const [goToUri, setGoToUri] = useState<string>(Directory.EXTERNAL_STORAGE);
  const [dir, setDir] = useState<File>();
  const [files, setFiles] = useState<File[]>();
  const [url, setUrl] = useState<string>();

  useEffect(() => {
    handleOpenFile(fsManager.File(goToUri));
  }, [goToUri]);

  const handleOpenFile = (file: File) => {
    const attributes = file.getAttributes();

    if (attributes.isDirectory) {
      file.readDir().then(files => {
        setDir(file);
        setFiles(files);
        setUrl(undefined);
      });
    }

    if (attributes.isFile) {
      setUrl(file.localUrl);
    }
  };

  return (
    <>
      <h2>{'File system'}</h2>

      <p>
        {'Go to: '}
        <select
          value={goToUri}
          onChange={event => {
            setGoToUri(event.target.value);
          }}
        >
          {Object.values(Directory).map(uri => (
            <option value={uri}>{uri}</option>
          ))}
        </select>
      </p>

      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '0.5em',
          width: '100%',
          alignItems: 'flex-start',
          overflow: 'hidden',
          whiteSpace: 'nowrap',
        }}
      >
        {dir?.uri && decodeURIComponent(dir.uri)}

        <a
          onClick={() => {
            if (dir !== undefined && dir.parentFile !== null) {
              handleOpenFile(dir.parentFile);
            }
          }}
        >
          {'⤴️'}
        </a>

        {files?.map(file => (
          <a
            href={'#'}
            onClick={event => {
              event.preventDefault();
              handleOpenFile(file);
            }}
          >
            {decodeURIComponent(file.uri.split('/').filter(Boolean).pop()!)}
          </a>
        ))}

        {'Total files: '}
        {files?.length || 0}
      </div>

      {url !== undefined && (
        <p>
          <iframe src={url} />
        </p>
      )}
    </>
  );
}
