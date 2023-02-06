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
    push(response: readonly [requestId: number | null, event: ResponseEvent]): void;
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
  const resolvers = new Map<number, (event: ResponseEvent) => void>();

  const inbox: Connection['inbox'] = {
    push([requestId, event]) {
      if (requestId === null) {
        pubSub.publish(event);
        return;
      }

      const resolver = resolvers.get(requestId);

      if (resolver !== undefined) {
        resolvers.delete(requestId);
        resolver(event as ResponseEvent);
      }
    },
  };

  let requestCount = 0;

  let openConnection = (): Promise<Connection> => {
    const connectionPromise = untilTruthy(connectionProvider, 100).then(connection => {
      const responses = connection.inbox;

      connection.inbox = inbox;

      if (Array.isArray(responses)) {
        for (const response of responses) {
          inbox.push(response);
        }
      } else if (responses !== undefined) {
        throw new Error('Connection should belong to a single event bridge');
      }
      return connection;
    });

    openConnection = () => connectionPromise;

    return connectionPromise;
  };

  return {
    request(event) {
      return new Promise(resolve => {
        const requestId = requestCount++;
        const eventJson = JSON.stringify(event);

        resolvers.set(requestId, resolve);

        openConnection().then(connection => {
          connection.post(requestId, eventJson);
        });
      });
    },

    subscribe(listener) {
      openConnection();
      return pubSub.subscribe(listener);
    },
  };
}
