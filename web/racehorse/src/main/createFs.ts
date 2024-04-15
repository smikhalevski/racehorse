import { Event, EventBridge } from './createEventBridge';
import { noop } from './utils';

export const FileDir = {
  DOCUMENTS: 'racehorse://documents',
  DATA: 'racehorse://data',
  LIBRARY: 'racehorse://library',
  CACHE: 'racehorse://cache',
  EXTERNAL: 'racehorse://external',
  EXTERNAL_STORAGE: 'racehorse://external_storage',
} as const;

export type FileDir = (typeof FileDir)[keyof typeof FileDir];

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

export class File {
  constructor(
    private eventBridge: EventBridge,
    public readonly uri: string
  ) {}

  getLastModifiedTime(): number {
    return 0;
  }

  getLastAccessTime(): number {
    return 0;
  }

  getCreationTime(): number {
    return 0;
  }

  isFile(): boolean {
    return false;
  }

  isDirectory(): boolean {
    return false;
  }

  isSymbolicLink(): boolean {
    return false;
  }

  isExisting(): boolean {
    return this.eventBridge.request({
      type: 'org.racehorse.FsExistsEvent',
      payload: { uri: this.uri },
    }).payload.isExisting;
  }

  getSize(): number {
    return 0;
  }

  getStats(): FileStats {
    return this.eventBridge.request({
      type: 'org.racehorse.FsStatsEvent',
      payload: { uri: this.uri },
    }).payload.stats;
  }

  getParent(): File {
    return null!;
  }

  mkdir(): Promise<boolean> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsMkdirEvent', payload: { uri: this.uri } })
      .then(isSuccessful);
  }

  readDir(): Promise<File[]> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsReadDirEvent', payload: { uri: this.uri } })
      .then(event => event.payload.uris.map((uri: string) => new File(this.eventBridge, uri)));
  }

  readText(encoding = 'utf8'): Promise<string> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsReadEvent', payload: { uri: this.uri, encoding } })
      .then(event => event.payload.text);
  }

  readBytes(): Promise<string> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsReadEvent', payload: { uri: this.uri } })
      .then(event => event.payload.data);
  }

  appendText(text: string, encoding = 'utf8'): Promise<void> {
    return this.eventBridge
      .requestAsync({
        type: 'org.racehorse.FsWriteEvent',
        payload: { uri: this.uri, text, encoding, isAppended: true },
      })
      .then(noop);
  }

  appendBytes(data: string | Blob | ArrayBuffer): Promise<void> {
    return encodeBase64(data)
      .then(data =>
        this.eventBridge.requestAsync({
          type: 'org.racehorse.FsWriteEvent',
          payload: { uri: this.uri, data, isAppended: true },
        })
      )
      .then(noop);
  }

  writeText(text: string, encoding?: string): Promise<void> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsWriteEvent', payload: { uri: this.uri, text, encoding } })
      .then(noop);
  }

  writeBytes(data: string | Blob | ArrayBuffer): Promise<void> {
    return encodeBase64(data)
      .then(data =>
        this.eventBridge.requestAsync({ type: 'org.racehorse.FsWriteEvent', payload: { uri: this.uri, data } })
      )
      .then(noop);
  }

  copy(to: File, overwrite?: boolean): Promise<boolean> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsCopyEvent', payload: { uri: this.uri, toUri: to.uri, overwrite } })
      .then(isSuccessful);
  }

  move(to: File, overwrite?: boolean): Promise<boolean> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsMoveEvent', payload: { uri: this.uri, toUri: to.uri, overwrite } })
      .then(isSuccessful);
  }

  delete(): Promise<boolean> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsDeleteEvent', payload: { uri: this.uri } })
      .then(isSuccessful);
  }
}

export interface Fs {
  File(uri: string): File;

  File(parent: File | string, path: string): File;
}

/**
 * File system CRUD operations.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createFs(eventBridge: EventBridge): Fs {
  return {
    File: (uriOrParent, path?: string) =>
      new File(
        eventBridge,
        typeof uriOrParent === 'string'
          ? uriOrParent
          : path !== undefined
            ? resolve(uriOrParent.uri, path)
            : uriOrParent.uri
      ),
  };
}

function encodeBase64(data: string | Blob | ArrayBuffer): Promise<string> {
  if (data instanceof ArrayBuffer) {
    data = new Blob([data]);
  }

  if (data instanceof Blob) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();

      reader.addEventListener('load', () => {
        resolve(String(reader.result));
      });

      reader.addEventListener('error', () => {
        reject(new Error('Failed to read blob'));
      });

      reader.readAsDataURL(data);
    });
  }

  return Promise.resolve(data);
}

function isSuccessful(event: Event): boolean {
  return event.payload.isSuccessful;
}
