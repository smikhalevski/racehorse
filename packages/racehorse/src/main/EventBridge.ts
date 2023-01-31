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

/**
 * The envelope pushed to the inbox by Android.
 */
export interface Envelope {
  /**
   * The request ID passed to {@linkcode Connection.post} or -1 if the {@linkcode event} is a notification.
   */
  requestId: number;

  /**
   * `true` for an event, or `false` for an exception.
   */
  ok: boolean;

  /**
   * The response event.
   */
  event: Event;
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
     *
     * @param envelope An incoming envelope.
     */
    push(envelope: Envelope): void;
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
   * Sends an event through a connection to Android and returns a promise that is resolved when a response with a
   * matching {@linkcode Envelope.requestId} is pushed to the {@link Connection.inbox connection inbox}. If an exception
   * is thrown in Android during an event processing, the returned promise is rejected with an `Error`.
   *
   * @param event The request event to send.
   * @returns The response event.
   */
  request(event: Event): Promise<Event>;

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

  /**
   * A handler that is triggered if a listener throws an error, or an error was pushed the
   * {@link Connection.inbox connection inbox}.
   */
  errorHandler?: (error: unknown) => void;
}

/**
 * Creates a message bus that transports messages from and to the native realm through the connection.
 *
 * @param options The message bus options.
 */
export function createEventBridge(options: EventBridgeOptions = {}): EventBridge {
  const { connectionProvider = () => window.racehorseConnection, errorHandler = PubSub.defaultErrorHandler } = options;

  const pubSub = new PubSub<Event>(errorHandler);
  const consumers = new Map<number, { resolve(event: Event): void; reject(error: Error): void }>();

  const pushToInbox = (envelope: Envelope): void => {
    const { event } = envelope;

    const consumer = consumers.get(envelope.requestId);

    if (envelope.ok) {
      if (consumer) {
        consumer.resolve(event);
      } else {
        pubSub.publish(event);
      }
      return;
    }

    const error = Object.assign(Object.create(Error.prototype), event);

    if (consumer) {
      consumer.reject(error);
    } else {
      errorHandler(error);
    }
  };

  const connectionPromise = untilTruthy(connectionProvider, 100).then(connection => {
    const { inbox } = connection;

    connection.inbox = { push: pushToInbox };

    if (Array.isArray(inbox)) {
      for (const envelope of inbox) {
        pushToInbox(envelope);
      }
    }
    return connection;
  });

  let nonce = 0;

  const request: EventBridge['request'] = event =>
    new Promise((resolve, reject) => {
      const requestId = nonce++;
      const eventStr = JSON.stringify(event);

      consumers.set(requestId, { resolve, reject });

      connectionPromise.then(connection => {
        connection.post(requestId, eventStr);
      });
    });

  const subscribe: EventBridge['subscribe'] = listener => pubSub.subscribe(listener);

  return { request, subscribe };
}
