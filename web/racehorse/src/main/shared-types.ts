import { PubSub } from 'parallel-universe';

/**
 * The event transported through the {@linkcode EventBridge}.
 */
export interface Event {
  /**
   * The event type (a qualified class name).
   */
  type: string;

  /**
   * The event payload.
   */
  [key: string]: any;
}

/**
 * The response event pushed by Android as the result of a request.
 */
export interface ResponseEvent extends Event {
  /**
   * `true` for a success response, or `false` for an error response.
   */
  ok: boolean;
}

/**
 * The connection is added to the page as a
 * {@linkcode https://developer.android.com/reference/android/webkit/JavascriptInterface JavascriptInterface}.
 */
export interface Connection {
  /**
   * The total number of requests transported through this connection. Used as a request ID.
   */
  requestCount?: number;

  /**
   * The pub-sub to which Android publishes envelopes for the web to consume.
   */
  inboxPubSub?: PubSub<[requestId: number, event: Event]>;

  /**
   * Delivers a serialized event to Android.
   *
   * @param requestId The unique request ID.
   * @param eventJson The serialized event.
   */
  post(requestId: number, eventJson: string): void;
}

/**
 * Returns a connection object or promise that resolves when a connection is available.
 */
export type ConnectionProvider = () => Connection | Promise<Connection>;

declare global {
  interface Window {
    /**
     * The connection object injected by Android.
     */
    racehorseConnection?: Connection;
  }
}

/**
 * The plugin that enhances the event bridge.
 *
 * @param eventBridge The event bridge that must be enhanced.
 * @param listener The callback that plugin should use to notify the listeners that were attached to the event bridge
 * via {@linkcode EventBridge.subscribe}
 */
export type Plugin<M extends object> = (eventBridge: EventBridge & Partial<M>, listener: () => void) => void;

/**
 * The event bridge that transports events between Android and web.
 */
export interface EventBridge {
  /**
   * The promise that is resolved when a connection becomes available.
   */
  waitForConnection(): Promise<void>;

  /**
   * Sends an event through a connection to Android and returns a promise that is resolved when a response with a
   * matching ID is published to the {@linkcode Connection.inboxPubSub}. The returned promise is never rejected.
   * Check {@linkcode ResponseEvent.ok} to detect that an error occurred.
   *
   * @param event The request event to send.
   * @returns The response event.
   */
  request(event: Event): Promise<ResponseEvent>;

  /**
   * Sends an event through a connection to Android and synchronously returns a response event, or `null` if a response
   * cannot be produced synchronously.
   *
   * @param event The request event to send.
   * @returns The response event.
   */
  requestSync(event: Event): ResponseEvent | undefined;

  /**
   * Subscribes a listener to alert events pushed by Android.
   *
   * @param listener The listener to subscribe.
   * @returns The callback that unsubscribes the listener.
   */
  subscribeToAlerts(
    /**
     * @param event The event pushed to the connection inbox.
     */
    listener: (event: Event) => void
  ): () => void;

  /**
   * Subscribes listener to changes of the event bridge object.
   *
   * @param listener The listener to subscribe.
   * @returns The callback that unsubscribes the listener.
   */
  subscribe(listener: () => void): () => void;
}
