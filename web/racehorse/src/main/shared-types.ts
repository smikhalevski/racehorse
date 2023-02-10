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
 * {@linkcode https://developer.android.com/reference/android/webkit/JavascriptInterface JavascriptInterface}
 */
export interface Connection {
  /**
   * An array of envelopes pushed by Android, or an inbox object created by the {@linkcode EventBridge}.
   */
  inbox?: {
    /**
     * Called by Android when an envelope is pushed through the connection.
     */
    push(response: readonly [requestId: number | null, event: Event]): void;
  };

  /**
   * Delivers a serialized event to Android.
   *
   * @param requestId The unique request ID.
   * @param eventJson The serialized event.
   */
  post(requestId: number, eventJson: string): void;
}

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
 * @param listener The callback that the plugin should invoke to notify the plugin consumer that the plugin updated some
 * fields of the event bridge.
 * @returns The callback that unsubscribes the listener, or `undefined` if there's nothing to unsubscribe.
 */
export type Plugin<M> = (eventBridge: EventBridge & M, listener: () => void) => (() => void) | void;

/**
 * The event bridge that transports events between the Android and web realms.
 */
export interface EventBridge {
  /**
   * Sends an event through a connection to Android and returns a promise that is resolved when a response with a
   * matching {@linkcode Envelope.requestId} is pushed to the {@link Connection.inbox connection inbox}. The returned
   * promise is never rejected. Check {@linkcode ResponseEvent.ok} to detect that an error occurred.
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
  subscribeToAlerts(
    /**
     * @param event The event pushed to the connection inbox.
     */
    listener: (alertEvent: Event) => void
  ): () => void;
}
