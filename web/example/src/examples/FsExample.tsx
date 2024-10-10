import React, { useEffect, useState } from 'react';
import { activityManager, Directory, File, fsManager, Intent } from 'racehorse';
import { Section } from '../components/Section';

interface Content {
  dir: File;
  files: File[];
}

export function FsExample() {
  const [content, setContent] = useState<Content>({ dir: fsManager.File(Directory.CACHE), files: [] });

  const { dir, files } = content;

  useEffect(() => handleOpenFile(dir), []);

  const handleOpenFile = (file: File) => {
    const attributes = file.getAttributes();

    // Show directory content
    if (attributes.isDirectory) {
      file.readDir().then(files => {
        files.sort((a, b) => -a.isDirectory - -b.isDirectory || a.uri.localeCompare(b.uri));
        setContent({ dir: file, files });
      });
    }

    // Open preview
    if (attributes.isFile) {
      activityManager.startActivity({
        action: Intent.ACTION_VIEW,
        flags: Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION,
        data: file.contentUri,
      });
    }
  };

  return (
    <Section title={'File system'}>
      <div className="list-group">
        <button
          className="list-group-item list-group-item-primary d-flex justify-content-between align-items-center dropdown-toggle"
          data-bs-toggle="dropdown"
          aria-expanded="false"
        >
          <div className="overflow-hidden flex-shrink-1">{decodeURIComponent(dir.uri)}</div>
        </button>

        <ul className="dropdown-menu dropdown-menu-end shadow">
          <li className="dropdown-header">{'Jump to…'}</li>

          {Object.values(Directory).map(uri => (
            <li key={uri}>
              <button
                className="dropdown-item"
                onClick={() => handleOpenFile(fsManager.File(uri))}
              >
                {uri}
              </button>
            </li>
          ))}
        </ul>

        <div
          className="list-group-item list-group-item-action"
          onClick={() => {
            if (dir.parentFile !== null) {
              handleOpenFile(dir.parentFile);
            }
          }}
        >
          <i className="bi-arrow-90deg-up me-2" />
          {'..'}
        </div>

        {files.length === 0 && (
          <div className="list-group-item list-group-item-light text-secondary text-center">{'No files'}</div>
        )}

        {files.map(file => (
          <div
            key={file.uri}
            className="list-group-item list-group-item-action overflow-hidden"
            onClick={() => handleOpenFile(file)}
          >
            <i className={file.isDirectory ? 'bi-folder me-2' : 'me-4'} />

            {decodeURIComponent(file.uri).split('/').pop()}
          </div>
        ))}
      </div>
    </Section>
  );
}
