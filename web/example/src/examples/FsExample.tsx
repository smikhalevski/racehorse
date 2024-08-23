import React, { useEffect, useState } from 'react';
import { activityManager, Directory, File, fsManager, Intent } from 'racehorse';

export function FsExample() {
  const [baseUri, setBaseUri] = useState<string>(Directory.CACHE);
  const [content, setContent] = useState<{ dir: File; files: File[] }>();

  useEffect(() => {
    handleOpen(fsManager.File(baseUri));
  }, [baseUri]);

  const handleOpen = (file: File) => {
    const attributes = file.getAttributes();

    if (attributes.isDirectory) {
      file.readDir().then(files => {
        files.sort((a, b) => -a.isDirectory - -b.isDirectory);

        setContent({ dir: file, files });
      });
    }

    if (attributes.isFile) {
      activityManager.startActivity({
        action: Intent.ACTION_VIEW,
        flags: Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION,
        data: file.contentUri,
      });
    }
  };

  return (
    <>
      <h1>{'File system'}</h1>

      <p>
        <label className="form-label">{'Go to'}</label>
        <select
          className="form-select"
          value={baseUri}
          onChange={event => {
            setBaseUri(event.target.value);
          }}
        >
          {Object.values(Directory).map(uri => (
            <option
              key={uri}
              value={uri}
            >
              {uri}
            </option>
          ))}
        </select>
      </p>

      {content !== undefined && (
        <Content
          dir={content.dir}
          files={content.files}
          onOpen={handleOpen}
        />
      )}
    </>
  );
}

interface ContentProps {
  dir: File;
  files: File[];

  onOpen(file: File): void;
}

function Content(props: ContentProps) {
  const { dir, files, onOpen } = props;

  return (
    <div className="list-group">
      <div className="list-group-item list-group-item-primary overflow-hidden">{decodeURIComponent(dir.uri)}</div>

      <div
        className="list-group-item list-group-item-action"
        onClick={() => {
          if (dir.parentFile !== null) {
            onOpen(dir.parentFile);
          }
        }}
      >
        <i className="bi-arrow-90deg-up me-2" />
        {'..'}
      </div>

      {files.map(file => (
        <div
          key={file.uri}
          className="list-group-item list-group-item-action overflow-hidden"
          onClick={() => onOpen(file)}
        >
          <i className={file.isDirectory ? 'bi-folder me-2' : 'me-4'} />

          {decodeURIComponent(file.uri.substring(file.uri.lastIndexOf('/') + 1))}
        </div>
      ))}

      {files.length === 0 && (
        <div className="list-group-item list-group-item-light text-secondary text-center">{'No files'}</div>
      )}
    </div>
  );
}
