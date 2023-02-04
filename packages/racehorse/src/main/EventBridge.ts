import { PubSub, untilTruthy } from 'parallel-universe';

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

export interface ResponseEvent extends Event {
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
   * @param eventStr The serialized event.
   */
  post(requestId: number, eventStr: string): void;
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
 * The event bridge that transports events between the Android and web realms.
 */
export interface EventBridge {
  /**
   * `true` if bridge connection is ready to process events, `false` otherwise.
   */
  isConnected(): boolean;

  /**
   * Sends an event through a connection to Android and returns a promise that is resolved when a response with a
   * matching {@linkcode Envelope.requestId} is pushed to the {@link Connection.inbox connection inbox}. If an exception
   * is thrown in Android during an event processing, the returned promise is rejected with an `Error`.
   *
   * @param event The request event to send.
   * @returns The response event.
   */
  request(event: Event): Promise<ResponseEvent>;

  /**
   * Subscribes a listener to all events pushed to the inbox that don't have a consumer.
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

export interface EventBridgeOptions {
  /**
   * Returns the connection opened by Android. The callback is polled until the connection is returned. By default,
   * {@linkcode window.racehorseConnection} is awaited.
   */
  connectionProvider?: () => Connection | undefined;
}

/**
 * Creates a message bus that transports messages from and to the native realm through the connection.
 *
 * @param options The message bus options.
 */
export function createEventBridge(options: EventBridgeOptions = {}): EventBridge {
  const { connectionProvider = () => window.racehorseConnection } = options;

  const pubSub = new PubSub<Event>();
  const callbacks = new Map<number, (event: ResponseEvent) => void>();

  let requestCount = 0;
  let connected = false;

  const inbox: Connection['inbox'] = {
    push([requestId, event]) {
      if (requestId === null) {
        // org.racehorse.AlertEvent
        pubSub.publish(event);
        return;
      }

      // org.racehorse.ResponseEvent
      const callback = callbacks.get(requestId);

      if (callback !== undefined) {
        callbacks.delete(requestId);
        callback(event as ResponseEvent);
      }
    },
  };

  const connectionPromise = untilTruthy(connectionProvider, 100).then(connection => {
    const responses = connection.inbox;

    connection.inbox = inbox;

    connected = true;

    if (Array.isArray(responses)) {
      for (const response of responses) {
        inbox.push(response);
      }
    }
    return connection;
  });

  const request: EventBridge['request'] = event =>
    new Promise(resolve => {
      const requestId = requestCount++;
      const eventStr = JSON.stringify(event);

      callbacks.set(requestId, resolve);

      connectionPromise.then(connection => {
        connection.post(requestId, eventStr);
      });
    });

  const subscribe: EventBridge['subscribe'] = listener => pubSub.subscribe(listener);

  return {
    isConnected: () => connected,
    request,
    subscribe,
  };
}
