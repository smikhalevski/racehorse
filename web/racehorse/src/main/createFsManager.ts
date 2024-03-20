import { EventBridge } from './createEventBridge';
import { noop } from './utils';

export interface FileStats {
  lastModifiedTime: number;
  lastAccessTime: number;
  creationTime: number;
  isFile: boolean;
  isDirectory: boolean;
  isSymbolicLink: boolean;
  isOther: boolean;
  size: number;
}

export const FileDirectory = {
  DOCUMENTS: 'documents',
  DATA: 'data',
  LIBRARY: 'library',
  CACHE: 'cache',
  EXTERNAL: 'external',
  EXTERNAL_STORAGE: 'external_storage',
} as const;

export type FileDirectory = (typeof FileDirectory)[keyof typeof FileDirectory];

export interface FsManager {
  mkdir(path: string, options?: { directory?: FileDirectory }): Promise<boolean>;

  readDir(path: string, options?: { directory?: FileDirectory; glob?: string }): Promise<string[]>;

  readText(path: string, options?: { directory?: FileDirectory; encoding?: string }): Promise<string>;

  readBytes(path: string, options?: { directory?: FileDirectory }): Promise<Blob>;

  appendText(path: string, text: string, options?: { directory?: FileDirectory; encoding?: string }): Promise<void>;

  appendBytes(path: string, buffer: Blob, options?: { directory?: FileDirectory }): Promise<void>;

  writeText(path: string, text: string, options?: { directory?: FileDirectory; encoding?: string }): Promise<void>;

  writeBytes(path: string, buffer: Blob, options?: { directory?: FileDirectory }): Promise<void>;

  copy(options: {
    sourcePath: string;
    targetPath: string;
    sourceDirectory?: FileDirectory;
    targetDirectory?: FileDirectory;
    overwrite?: boolean;
  }): Promise<boolean>;

  move(options?: {
    sourcePath: string;
    targetPath: string;
    sourceDirectory?: FileDirectory;
    targetDirectory?: FileDirectory;
    overwrite?: boolean;
  }): Promise<void>;

  delete(path: string, options?: { directory?: FileDirectory }): Promise<boolean>;

  exists(path: string, options?: { directory?: FileDirectory }): Promise<boolean>;

  stat(path: string, options?: { directory?: FileDirectory }): Promise<FileStats>;
}

/**
 * File system CRUD operations.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createFsManager(eventBridge: EventBridge): FsManager {
  return {
    mkdir: (path, options) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.FsMkdirEvent', payload: { ...options, path } })
        .then(event => event.payload.isSuccessful),

    readDir: (path, options) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.FsReadDirEvent', payload: { ...options, path } })
        .then(event => event.payload.files),

    readText: (path, options) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.FsReadTextEvent', payload: { ...options, path } })
        .then(event => event.payload.text),

    readBytes: (path, options) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.FsReadBytesEvent', payload: { ...options, path } })
        .then(event => event.payload.buffer),

    appendText: (path, text, options) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.FsAppendTextEvent', payload: { ...options, path, text } })
        .then(noop),

    appendBytes: (path, buffer, options) =>
      eventBridge
        .requestAsync({
          type: 'org.racehorse.FsAppendBytesEvent',
          payload: { ...options, path, buffer },
        })
        .then(noop),

    writeText: (path, text, options) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.FsWriteTextEvent', payload: { ...options, path, text } })
        .then(noop),

    writeBytes: (path, buffer, options) =>
      eventBridge
        .requestAsync({
          type: 'org.racehorse.FsWriteBytesEvent',
          payload: { ...options, path, buffer },
        })
        .then(noop),

    copy: options =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.FsCopyEvent', payload: options })
        .then(event => event.payload.isSuccessful),

    move: options => eventBridge.requestAsync({ type: 'org.racehorse.FsMoveEvent', payload: options }).then(noop),

    delete: (path, options) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.FsDeleteEvent', payload: { ...options, path } })
        .then(event => event.payload.isSuccessful),

    exists: (path, options) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.FsExistsEvent', payload: { ...options, path } })
        .then(event => event.payload.isExisting),

    stat: (path, options) =>
      eventBridge
        .requestAsync({
          type: 'org.racehorse.FsStatEvent',
          payload: { ...options, path },
        })
        .then(event => event.payload),
  };
}
