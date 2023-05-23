import { PubSub, untilTruthy } from 'parallel-universe';

/**
 * The event transported through the {@link EventBridge}.
 */
export interface Event {
  /**
   * The event type (a qualified class name).
   */
  type: string;

  /**
   * The event payload.
   */
  payload?: any;
}

/**
 * The connection is added to the page as a
 * [`JavascriptInterface`](https://developer.android.com/reference/android/webkit/JavascriptInterface).
 */
export interface Connection {
  /**
   * The pub-sub to which Android publishes envelopes for the web to consume.
   *
   * Request ID is either a non-negative number returned from {@link post}, or -1 if an event is an alert pushed by
   * Android.
   */
  inbox?: PubSub<[requestId: number, event: Event]>;

  /**
   * Delivers a serialized event to Android.
   *
   * @param eventJson The serialized event.
   * @returns The unique request ID.
   */
  post(eventJson: string): string;
}

/**
 * The event bridge that transports events between Android and web.
 */
export interface EventBridge {
  /**
   * Returns the promise that is resolved when a connection becomes available. Usually, you don't have to call this
   * method manually, since the connection would be established automatically as soon as the first request is sent or
   * the first listener is subscribed.
   */
  connect(): Promise<Required<Connection>>;

  /**
   * Returns the established connection, or `null` if it isn't available. Use {@link connect} to establish a connection.
   */
  getConnection(): Required<Connection> | null;

  /**
   * Sends an event through a connection to Android and returns a response event.
   *
   * Before using synchronous requests, wait for {@link connect a connection} to be established.
   *
   * The error is thrown if `org.racehorse.ExceptionEvent` is published as a response, if the response event wasn't
   * published synchronously, or if connection wasn't yet established.
   *
   * @param event The request event to send.
   * @returns The response event.
   */
  request(event: Event): Event;

  /**
   * Sends an event through a connection to Android and returns a promise that is resolved when a response event.
   *
   * The connection is automatically established when this method is called.
   *
   * The returned promise is rejected if `org.racehorse.ExceptionEvent` is published as a response.
   *
   * @param event The request event to send.
   * @returns The response event.
   */
  requestAsync(event: Event): Promise<Event>;

  /**
   * Subscribes a listener to notice events pushed by Android.
   *
   * The connection is automatically established when this method is called.
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

declare global {
  interface Window {
    /**
     * The connection object injected by Android.
     */
    racehorseConnection?: Connection;
  }
}

/**
 * Creates an event bridge that transports events from and to Android through the connection.
 *
 * Exponentially increases delay between `connectionProvider` calls if `undefined` is returned.
 *
 * @param connectionProvider The provider that returns the connection. By default, the provider of
 * `window.racehorseConnection` is used.
 */
export function createEventBridge(connectionProvider = () => window.racehorseConnection): EventBridge {
  const noticePubSub = new PubSub<Event>();

  let connectionPromise: Promise<Required<Connection>> | undefined;
  let connection: Required<Connection> | null = null;
  let tryCount = 0;

  const connect = (): Promise<Required<Connection>> =>
    (connectionPromise ||= untilTruthy(connectionProvider, () => 2 ** tryCount++ * 100).then(conn => {
      (conn.inbox ||= new PubSub()).subscribe(envelope => {
        if (envelope[0] === -2) {
          noticePubSub.publish(envelope[1]);
        }
      });
      return (connection = conn as Required<Connection>);
    }));

  return {
    connect,

    getConnection: () => connection,

    request(event) {
      if (connection === null) {
        throw new Error('Expected a connection');
      }

      const response = JSON.parse(connection.post(JSON.stringify(event)));

      if (typeof response === 'number') {
        throw new Error('Expected a synchronous response: ' + event.type);
      }

      return ensureEvent(response);
    },

    requestAsync(event) {
      const requestJson = JSON.stringify(event);

      return connect().then(
        connection =>
          new Promise(resolve => {
            const response = JSON.parse(connection.post(requestJson));

            if (typeof response === 'number') {
              const unsubscribe = connection.inbox.subscribe(envelope => {
                if (envelope[0] === response) {
                  unsubscribe();
                  resolve(ensureEvent(envelope[1]));
                }
              });
              return;
            }

            resolve(ensureEvent(response));
          })
      );
    },

    subscribe(listener) {
      void connect();
      return noticePubSub.subscribe(listener);
    },
  };
}

/**
 * Throws an error if an event carries an exception, or returns event as is.
 *
 * @param event The event to check.
 */
function ensureEvent(event: Event): Event {
  if (event.type === 'org.racehorse.ExceptionEvent') {
    throw Object.assign(new Error(), event.payload);
  }
  return event;
}
