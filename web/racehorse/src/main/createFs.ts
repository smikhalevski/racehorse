import { Event, EventBridge } from './createEventBridge';
import { noop } from './utils';

export const SystemDir = {
  DOCUMENTS: 'racehorse://documents',
  DATA: 'racehorse://data',
  LIBRARY: 'racehorse://library',
  CACHE: 'racehorse://cache',
  EXTERNAL: 'racehorse://external',
  EXTERNAL_STORAGE: 'racehorse://external_storage',
} as const;

export type SystemDir = (typeof SystemDir)[keyof typeof SystemDir];

export interface FileStat {
  lastModifiedTime: number;
  lastAccessTime: number;
  creationTime: number;
  isFile: boolean;
  isDirectory: boolean;
  isSymbolicLink: boolean;
  isOther: boolean;
  size: number;
}

/**
 * Describes the file on the device.
 */
export class File {
  /**
   * Creates a new {@link File} instance.
   *
   * @param eventBridge The underlying event bridge.
   * @param uri The URI that denotes the file.
   */
  constructor(
    private eventBridge: EventBridge,
    public readonly uri: string
  ) {}

  /**
   * Returns `true` if the file exists, of `false` otherwise.
   */
  isExisting(): Promise<boolean> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsIsExistingEvent', payload: { uri: this.uri } })
      .then(event => event.payload.isExisting);
  }

  /**
   * Returns filesystem attributes of the file.
   */
  getStat(): Promise<FileStat> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsStatsEvent', payload: { uri: this.uri } })
      .then(event => event.payload.stats);
  }

  /**
   * Returns the [MIME type](https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types) of the file.
   */
  getMimeType(): Promise<string> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsGetMimeTypeEvent', payload: { uri: this.uri } })
      .then(event => event.payload.mimeType);
  }

  /**
   * Creates a directory denoted by this file.
   */
  mkdir(): Promise<boolean> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsMkdirEvent', payload: { uri: this.uri } })
      .then(isSuccessful);
  }

  /**
   * Reads the list of files contained in the directory.
   */
  readDir(): Promise<File[]> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsReadDirEvent', payload: { uri: this.uri } })
      .then(event => event.payload.uris.map((uri: string) => new File(this.eventBridge, uri)));
  }

  /**
   * Reads file contents as a string.
   *
   * @param encoding The expected file encoding.
   */
  readText(encoding = 'utf8'): Promise<string> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsReadEvent', payload: { uri: this.uri, encoding } })
      .then(event => event.payload.data);
  }

  /**
   * Reads the file contents as base64-encoded string.
   */
  readBytes(): Promise<string> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsReadEvent', payload: { uri: this.uri } })
      .then(event => event.payload.data);
  }

  /**
   * Reads file contents as a [data URL](https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/Data_URLs).
   */
  readDataUrl(): Promise<string> {
    return this.getMimeType().then(mimeType => this.readBytes().then(data => `data:${mimeType};base64],${data}`));
  }

  /**
   * Reads the file contents as a [`Blob`](https://developer.mozilla.org/en-US/docs/Web/API/Blob).
   */
  readBlob(): Promise<Blob> {
    return this.getMimeType().then(mimeType =>
      this.readBytes().then(data => {
        const buffer = new Uint8Array(data.length);

        for (let i = 0; i < data.length; ++i) {
          buffer[i] = data.charCodeAt(i);
        }
        return new Blob([buffer], { type: mimeType });
      })
    );
  }

  /**
   * Appends text to the file.
   *
   * @param text The text to append.
   * @param encoding The expected file encoding.
   */
  appendText(text: string, encoding = 'utf8'): Promise<void> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsAppendEvent', payload: { uri: this.uri, data: text, encoding } })
      .then(noop);
  }

  /**
   * Appends binary data to the file.
   *
   * @param data The data to append.
   */
  appendBytes(data: string | Blob | ArrayBuffer): Promise<void> {
    return encodeBase64(data)
      .then(data =>
        this.eventBridge.requestAsync({ type: 'org.racehorse.FsAppendEvent', payload: { uri: this.uri, data } })
      )
      .then(noop);
  }

  /**
   * Writes the new text content to a file.
   *
   * If file doesn't exist then it is created. If file exists, it is overwritten.
   *
   * @param text The new text file contents.
   * @param encoding The expected file encoding.
   */
  writeText(text: string, encoding = 'utf8'): Promise<void> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsWriteEvent', payload: { uri: this.uri, data: text, encoding } })
      .then(noop);
  }

  /**
   * Writes the new binary content to a file.
   *
   * If file doesn't exist then it is created. If file exists, it is overwritten.
   *
   * @param data The new binary file contents.
   */
  writeBytes(data: string | Blob | ArrayBuffer): Promise<void> {
    return encodeBase64(data)
      .then(data =>
        this.eventBridge.requestAsync({ type: 'org.racehorse.FsWriteEvent', payload: { uri: this.uri, data } })
      )
      .then(noop);
  }

  /**
   * Copies file to a new location.
   *
   * If file is a directory then its contents are copied recursively.
   *
   * @param to The location to which contents must be copied.
   * @param overwrite If `true` then destination is overwritten.
   */
  copy(to: File, overwrite = false): Promise<boolean> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsCopyEvent', payload: { uri: this.uri, toUri: to.uri, overwrite } })
      .then(isSuccessful);
  }

  /**
   * Moves file to a new location.
   *
   * If file is a directory then its contents are moved recursively.
   *
   * @param to The location to which contents must be moved.
   * @param overwrite If `true` then destination is overwritten.
   */
  move(to: File, overwrite = false): Promise<boolean> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsMoveEvent', payload: { uri: this.uri, toUri: to.uri, overwrite } })
      .then(isSuccessful);
  }

  /**
   * Deletes the file.
   *
   * @returns `true` if the file was deleted, or `false` otherwise.
   */
  delete(): Promise<boolean> {
    return this.eventBridge
      .requestAsync({ type: 'org.racehorse.FsDeleteEvent', payload: { uri: this.uri } })
      .then(isSuccessful);
  }

  /**
   * Returns a URL that points to the file and can be loaded by the web view.
   */
  toUrl(): string {
    const { urlBase } = this.eventBridge.request({ type: 'org.racehorse.FsGetUrlBaseEvent' }).payload;

    return urlBase + '?uri=' + encodeURIComponent(this.uri);
  }
}

export interface Fs {
  /**
   * Creates a new {@link File} instance.
   *
   * @param uri The file URI.
   */
  File(uri: string): File;

  /**
   * Creates a new {@link File} instance.
   *
   * @param parent The parent file or URI.
   * @param path The relative file path.
   */
  File(parent: File | string, path: string): File;
}

/**
 * Filesystem CRUD operations.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createFs(eventBridge: EventBridge): Fs {
  return {
    File: (uriOrParent, path?: string) => {
      let uri = uriOrParent instanceof File ? uriOrParent.uri : uriOrParent;

      if (path !== undefined) {
        uri = eventBridge.request({ type: 'org.racehorse.FsResolveEvent', payload: { uri, path } }).payload.uri;
      }

      return new File(eventBridge, uri);
    },
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
