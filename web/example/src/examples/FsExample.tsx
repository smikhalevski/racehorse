import React, { useEffect, useState } from 'react';
import { activityManager, File, fs, Intent, SystemDir } from 'racehorse';

export function FsExample() {
  const [rootDir, setRootDir] = useState<string>(SystemDir.EXTERNAL_STORAGE);
  const [folder, setFolder] = useState<{ dir: File; files: File[] }>();

  useEffect(() => {
    handleGoToFile(fs.File(rootDir, '/'));
  }, [rootDir]);

  const handleGoToParent = () => {
    const parentFile = folder?.dir.getParent();

    if (parentFile) {
      handleGoToFile(parentFile);
    }
  };

  const handleGoToFile = async (file: File) => {
    const stat = await file.getStat();

    if (stat.isDirectory) {
      file.readDir().then(files => {
        setFolder({ dir: file, files });
      });
    }

    if (stat.isFile) {
      activityManager.startActivity({
        action: Intent.ACTION_VIEW,
        flags: Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION,
        type: await file.getMimeType(),
        data: file.getExposableUri(),
      });
    }
  };

  return (
    <>
      <h2>{'Filesystem'}</h2>

      <p>
        {'Root: '}
        <select
          onChange={event => {
            setRootDir(event.target.value);
          }}
        >
          {Object.values(SystemDir).map(dir => (
            <option value={dir}>{dir}</option>
          ))}
        </select>
      </p>

      {folder !== undefined && (
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
          {folder.dir.uri}

          <a onClick={handleGoToParent}>{'⤴️'}</a>

          {folder.files.map(file => (
            <a
              href={'#'}
              onClick={event => {
                event.preventDefault();
                handleGoToFile(file);
              }}
            >
              {file.uri.split('/').filter(Boolean).pop()}
            </a>
          ))}
        </div>
      )}
    </>
  );
}
