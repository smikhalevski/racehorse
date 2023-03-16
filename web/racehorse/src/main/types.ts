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
 * [`JavascriptInterface`](https://developer.android.com/reference/android/webkit/JavascriptInterface).
 */
export interface Connection {
  /**
   * The pub-sub to which Android publishes envelopes for the web to consume.
   *
   * Request ID is either a non-negative number returned from {@linkcode post}, or -1 if an event is an alert pushed by
   * Android.
   */
  inbox?: PubSub<[requestId: number, event: Event]>;

  /**
   * Delivers a serialized event to Android.
   *
   * @param eventData The serialized event.
   * @return The unique request ID.
   */
  post(eventData: string): number;
}

/**
 * The event bridge that transports events between Android and web.
 */
export interface EventBridge {
  /**
   * Returns the promise that is resolved when a connection becomes available. You don't have to call this method
   * manually, since the connection would be established automatically as soon as the first request or subscription is
   * posted. Await the returned promise before the app starts, to ensure the connection availability.
   */
  connect(): Promise<Required<Connection>>;

  /**
   * Sends an event through a connection to Android and returns a promise that is resolved when a response with a
   * matching ID is published to the {@linkcode Connection.inbox}. The returned promise is never rejected.
   * Check {@linkcode ResponseEvent.ok} to detect that an error occurred.
   *
   * @param event The request event to send.
   * @returns The response event.
   */
  request(event: Event): Promise<ResponseEvent>;

  /**
   * Subscribes a listener to alert events pushed by Android.
   *
   * @param listener The listener to subscribe.
   * @returns The callback that unsubscribes the listener.
   */
  subscribe(
    /**
     * @param event The event pushed to the connection inbox.
     */
    listener: (event: Event) => void
  ): () => void;
}
