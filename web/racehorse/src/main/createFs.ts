import { Event, EventBridge } from './createEventBridge';
import { noop } from './utils';

export const FileDir = {
  DOCUMENTS: 'documents',
  DATA: 'data',
  LIBRARY: 'library',
  CACHE: 'cache',
  EXTERNAL: 'external',
  EXTERNAL_STORAGE: 'external_storage',
} as const;

export type FileDir = (typeof FileDir)[keyof typeof FileDir];

export class File {
  constructor(
    private eventBridge: EventBridge,
    public path: string,
    public dir: FileDir | null = null
  ) {}

  get lastModifiedTime(): number {
    return 0;
  }

  get lastAccessTime(): number {
    return 0;
  }

  get creationTime(): number {
    return 0;
  }

  get isFile(): boolean {
    return false;
  }

  get isDir(): boolean {
    return false;
  }

  get isSymbolicLink(): boolean {
    return false;
  }

  get isExisting(): boolean {
    return this.eventBridge.request({
      type: 'org.racehorse.FsExistsEvent',
      payload: { path: this.path, dir: this.dir },
    }).payload.isExisting;
  }

  get size(): number {
    return 0;
  }

  createDir(): Promise<boolean> {
    return this.eventBridge
      .requestAsync({
        type: 'org.racehorse.FsMkdirEvent',
        payload: { path: this.path, dir: this.dir },
      })
      .then(event => event.payload.isSuccessful);
  }

  listFiles(glob?: string): Promise<File[]> {
    return this.eventBridge
      .requestAsync({
        type: 'org.racehorse.FsReadDirEvent',
        payload: { path: this.path, dir: this.dir },
      })
      .then(event => event.payload.files);
  }

  read(): Promise<Blob>;

  read(encoding: string): Promise<string>;

  read(encoding?: string) {
    return this.eventBridge
      .requestAsync({
        type: 'org.racehorse.FsReadTextEvent',
        payload: { path: this.path, dir: this.dir, encoding },
      })
      .then(event => event.payload.text || event.payload.blob);
  }

  append(text: string, encoding?: string): Promise<void>;

  append(blob: Blob): Promise<void>;

  append(data: string | Blob, encoding?: string): Promise<void> {
    if (data instanceof Blob) {
      return this.eventBridge
        .requestAsync({
          type: 'org.racehorse.FsAppendBytesEvent',
          payload: { path: this.path, dir: this.dir, blob: data },
        })
        .then(noop);
    }

    return this.eventBridge
      .requestAsync({
        type: 'org.racehorse.FsAppendTextEvent',
        payload: { path: this.path, dir: this.dir, text: data, encoding },
      })
      .then(noop);
  }

  write(blob: Blob): Promise<void>;

  write(data: string | Blob, encoding?: string): Promise<void> {
    let event: Event;
    if (data instanceof Blob) {
      event = {
        type: 'org.racehorse.FsWriteBytesEvent',
        payload: { path: this.path, dir: this.dir, blob: data },
      };
    } else {
      event = {
        type: 'org.racehorse.FsWriteTextEvent',
        payload: { path: this.path, dir: this.dir, text: data, encoding },
      };
    }
    return this.eventBridge
      .requestAsync(
        data instanceof Blob
          ? {
              type: 'org.racehorse.FsWriteBytesEvent',
              payload: { path: this.path, dir: this.dir, blob: data },
            }
          : {
              type: 'org.racehorse.FsWriteTextEvent',
              payload: { path: this.path, dir: this.dir, text: data, encoding },
            }
      )
      .then(noop);
  }

  // -------------------------------------
  // -------------------------------------
  // -------------------------------------
  // -------------------------------------
  // -------------------------------------

  writeText(text: string, encoding?: string): Promise<void> {
    return this.eventBridge
      .requestAsync({
        type: 'org.racehorse.FsWriteTextEvent',
        payload: { path: this.path, dir: this.dir, text, encoding },
      })
      .then(noop);
  }

  writeBytes(data: string | Blob | ArrayBuffer): Promise<void> {
    const request = (data: unknown) =>
      this.eventBridge
        .requestAsync({
          type: 'org.racehorse.FsWriteBytesEvent',
          payload: { path: this.path, dir: this.dir, data },
        })
        .then(noop);

    if (data instanceof ArrayBuffer) {
      data = new Blob([data]);
    }

    if (data instanceof Blob) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();

        reader.onload = () => {
          resolve(request(reader.result));
        };
        reader.onerror = () => {
          reject(new Error('Cannot read blob'));
        };
      });
    }

    return request(data);
  }

  copy(to: File, overwrite?: boolean): Promise<void> {
    return this.eventBridge
      .requestAsync({
        type: 'org.racehorse.FsCopyEvent',
        payload: { path: this.path, dir: this.dir, toPath: to.path, toDir: to.dir, overwrite },
      })
      .then(noop);
  }

  move(to: File, overwrite?: boolean): Promise<void> {
    return this.eventBridge
      .requestAsync({
        type: 'org.racehorse.FsMoveEvent',
        payload: { path: this.path, dir: this.dir, toPath: to.path, toDir: to.dir, overwrite },
      })
      .then(noop);
  }

  delete(): Promise<boolean> {
    return this.eventBridge
      .requestAsync({
        type: 'org.racehorse.FsDeleteEvent',
        payload: { path: this.path, dir: this.dir },
      })
      .then(event => event.payload.isSuccessful);
  }
}

// ---------------------------------------------------------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------------------------------------

export interface Fs {
  File(path: string, dir?: FileDir | null): File;
}

/**
 * File system CRUD operations.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createFs(eventBridge: EventBridge): Fs {
  return {
    File(path: string, dir: FileDir | null = null) {
      return new File(eventBridge, path, dir);
    },
  };
}
