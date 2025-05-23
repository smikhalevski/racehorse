import { EventBridge } from './createEventBridge.js';
import { noop } from './utils.js';

const SCHEME_RACEHORSE = 'racehorse';

/**
 * Device-independent URIs for well-known directories.
 */
export const Directory = {
  DOCUMENTS: `${SCHEME_RACEHORSE}://documents`,
  DATA: `${SCHEME_RACEHORSE}://data`,
  LIBRARY: `${SCHEME_RACEHORSE}://library`,
  CACHE: `${SCHEME_RACEHORSE}://cache`,
  EXTERNAL: `${SCHEME_RACEHORSE}://external`,
  EXTERNAL_STORAGE: `${SCHEME_RACEHORSE}://external_storage`,
} as const;

export type Directory = (typeof Directory)[keyof typeof Directory];

/**
 * Attributes associated with a file in a file system.
 */
export interface FileAttributes {
  /**
   * The time of last modification.
   */
  lastModifiedTime: number;

  /**
   * The time of last access.
   */
  lastAccessTime: number;

  /**
   * The creation time. The creation time is the time that the file was created.
   */
  creationTime: number;

  /**
   * Tells whether the file is a regular file with opaque content.
   */
  isFile: boolean;

  /**
   * Tells whether the file is a directory.
   */
  isDirectory: boolean;

  /**
   * Tells whether the file is a symbolic link.
   */
  isSymbolicLink: boolean;

  /**
   * Tells whether the file is something other than a regular file, directory, or symbolic link.
   */
  isOther: boolean;

  /**
   * The size of the file (in bytes).
   *
   * The size may differ from the actual size on the file system due to compression, support for sparse files, or other
   * reasons. The size of files that are not regular files is implementation specific and therefore unspecified.
   */
  size: number;
}

/**
 * Describes the file on the device.
 */
export class File {
  /**
   * @hidden
   */
  private readonly _eventBridge;

  /**
   * Creates a new {@link File} instance.
   *
   * @param eventBridge The underlying event bridge.
   * @param uri The URI that denotes the file.
   */
  constructor(
    eventBridge: EventBridge,
    public readonly uri: string
  ) {
    this._eventBridge = eventBridge;
  }

  /**
   * The time of the last modification.
   *
   * @see {@link getAttributes}
   */
  get lastModifiedTime(): number {
    return this.getAttributes().lastModifiedTime;
  }

  /**
   * The time of the last access.
   *
   * @see {@link getAttributes}
   */
  get lastAccessTime(): number {
    return this.getAttributes().lastAccessTime;
  }

  /**
   * The time when the file was created.
   *
   * @see {@link getAttributes}
   */
  get creationTime(): number {
    return this.getAttributes().creationTime;
  }

  /**
   * `true` if the file is a regular file with opaque content.
   *
   * @see {@link getAttributes}
   */
  get isFile(): boolean {
    return this.getAttributes().isFile;
  }

  /**
   * `true` if the file is a directory.
   *
   * @see {@link getAttributes}
   */
  get isDirectory(): boolean {
    return this.getAttributes().isDirectory;
  }

  /**
   * `true` if the file is a symbolic link.
   *
   * @see {@link getAttributes}
   */
  get isSymbolicLink(): boolean {
    return this.getAttributes().isSymbolicLink;
  }

  /**
   * `true` if the file is something other than a regular file, directory, or symbolic link.
   *
   * @see {@link getAttributes}
   */
  get isOther(): boolean {
    return this.getAttributes().isOther;
  }

  /**
   * The size of the file (in bytes).
   *
   * The size may differ from the actual size on the file system due to compression, support for sparse files, or other
   * reasons. The size of files that are not regular files is implementation specific and therefore unspecified.
   *
   * @see {@link getAttributes}
   */
  get size(): number {
    return this.getAttributes().size;
  }

  /**
   * `true` is the file exists.
   */
  get isExisting(): number {
    return this._eventBridge.request({
      type: 'org.racehorse.FsIsExistingEvent',
      payload: { uri: this.uri },
    }).payload.isExisting;
  }

  /**
   * The file that represents the parent directory, or `null` if there's no parent directory.
   */
  get parentFile(): File | null {
    const { parentUri } = this._eventBridge.request({
      type: 'org.racehorse.FsGetParentUriEvent',
      payload: { uri: this.uri },
    }).payload;

    const parentFile = parentUri !== null ? new File(this._eventBridge, parentUri) : null;

    Object.defineProperty(this, 'parentFile', { value: parentFile, configurable: true });

    return parentFile;
  }

  /**
   * The URL that can be loaded by the web view.
   */
  get localUrl(): string {
    const { localUrl } = this._eventBridge.request({
      type: 'org.racehorse.FsGetLocalUrlEvent',
      payload: { uri: this.uri },
    }).payload;

    Object.defineProperty(this, 'localUrl', { value: localUrl, configurable: true });

    return localUrl;
  }

  /**
   * The content URI of the file that can be shared with other applications.
   */
  get contentUri(): string {
    const { contentUri } = this._eventBridge.request({
      type: 'org.racehorse.FsGetContentUriEvent',
      payload: { uri: this.uri },
    }).payload;

    Object.defineProperty(this, 'contentUri', { value: contentUri, configurable: true });

    return contentUri;
  }

  /**
   * Reads a file's attributes as a bulk operation.
   */
  getAttributes(): FileAttributes {
    return this._eventBridge.request({
      type: 'org.racehorse.FsGetAttributesEvent',
      payload: { uri: this.uri },
    }).payload;
  }

  /**
   * Returns the [MIME type](https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types) of the file.
   */
  getMimeType(): Promise<string | null> {
    return this._eventBridge
      .requestAsync({ type: 'org.racehorse.FsGetMimeTypeEvent', payload: { uri: this.uri } })
      .then(event => event.payload.mimeType);
  }

  /**
   * Creates a directory denoted by this file.
   *
   * @returns `true` if the directory was created, or `false` otherwise.
   */
  mkdir(): Promise<boolean> {
    return this._eventBridge
      .requestAsync({ type: 'org.racehorse.FsMkdirEvent', payload: { uri: this.uri } })
      .then(event => event.payload.isSuccessful);
  }

  /**
   * Reads the list of files contained in the directory.
   */
  readDir(): Promise<File[]> {
    return this._eventBridge
      .requestAsync({ type: 'org.racehorse.FsReadDirEvent', payload: { uri: this.uri } })
      .then(event => event.payload.fileUris.map((fileUri: string) => new File(this._eventBridge, fileUri)));
  }

  /**
   * Reads text file contents as a string.
   *
   * @param encoding The expected file encoding.
   */
  readText(encoding = 'utf-8'): Promise<string> {
    return this._eventBridge
      .requestAsync({ type: 'org.racehorse.FsReadEvent', payload: { uri: this.uri, encoding } })
      .then(event => event.payload.data);
  }

  /**
   * Reads the binary file contents as base64-encoded string.
   */
  readBytes(): Promise<string> {
    return this._eventBridge
      .requestAsync({ type: 'org.racehorse.FsReadEvent', payload: { uri: this.uri } })
      .then(event => event.payload.data);
  }

  /**
   * Reads the binary file contents as a
   * [data URL](https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/Data_URLs).
   */
  readDataUri(): Promise<string> {
    return this.readBytes().then(data => this.getMimeType().then(mimeType => `data:${mimeType || ''};base64,${data}`));
  }

  /**
   * Reads the binary file contents as a {@link !Blob}.
   */
  readBlob(): Promise<Blob> {
    return this.readBytes().then(data =>
      this.getMimeType().then(mimeType => {
        const buffer = new Uint8Array(data.length);

        for (let i = 0; i < data.length; ++i) {
          buffer[i] = data.charCodeAt(i);
        }
        return new Blob([buffer], { type: mimeType || undefined });
      })
    );
  }

  /**
   * Appends text to the file.
   *
   * @param text The text to append.
   * @param encoding The expected file encoding.
   */
  appendText(text: string, encoding = 'utf-8'): Promise<void> {
    return this._eventBridge
      .requestAsync({
        type: 'org.racehorse.FsWriteEvent',
        payload: { uri: this.uri, data: text, encoding, append: true },
      })
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
        this._eventBridge.requestAsync({
          type: 'org.racehorse.FsWriteEvent',
          payload: { uri: this.uri, data, append: true },
        })
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
  writeText(text: string, encoding = 'utf-8'): Promise<void> {
    return this._eventBridge
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
        this._eventBridge.requestAsync({ type: 'org.racehorse.FsWriteEvent', payload: { uri: this.uri, data } })
      )
      .then(noop);
  }

  /**
   * Copies file contents to a new location.
   *
   * @param to The location to which contents must be copied.
   */
  copy(to: File): Promise<void> {
    return this._eventBridge
      .requestAsync({ type: 'org.racehorse.FsCopyEvent', payload: { uri: this.uri, toUri: to.uri } })
      .then(noop);
  }

  /**
   * Deletes the file.
   *
   * @returns `true` if the file was deleted, or `false` otherwise.
   */
  delete(): Promise<boolean> {
    return this._eventBridge
      .requestAsync({ type: 'org.racehorse.FsDeleteEvent', payload: { uri: this.uri } })
      .then(event => event.payload.isSuccessful);
  }

  /**
   * Returns the {@link uri} of the file.
   */
  toString(): string {
    return this.uri;
  }
}

/**
 * File system CRUD operations.
 */
export interface FsManager {
  /**
   * Creates a new {@link File} instance.
   *
   * @param uri The URI that points to the file on the file system.
   */
  File(uri: string): File;

  /**
   * Concatenates URI and relative path.
   *
   * @param uri The base URI.
   * @param path The relative path to concatenate.
   * @returns The concatenated URI.
   */
  resolve(uri: string, path: string): string;
}

/**
 * File system CRUD operations.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createFsManager(eventBridge: EventBridge): FsManager {
  return {
    File: uri => new File(eventBridge, uri),

    resolve: (uri, path) => uri + (path.length === 0 || uri.endsWith('/') || path.startsWith('/') ? path : '/' + path),
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

  return Promise.resolve(String(data));
}
