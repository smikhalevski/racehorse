import { PubSub, waitFor } from 'parallel-universe';
import { Unsubscribe } from './types.js';

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
   * The pub-sub to which Android publishes request IDs and corresponding response events.
   *
   * Request ID is either a non-negative number returned from {@link Connection.post}, or -2 if it is a
   * `org.racehorse.NoticeEvent` instance that was pushed by Android.
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
   * The connection is automatically established when this method is called.
   *
   * The error is thrown if `org.racehorse.ExceptionEvent` is published as a response, if the response event wasn't
   * published synchronously, or if connection cannot be established synchronously.
   *
   * @param event The request event to send.
   * @returns The response event.
   */
  request(event: Event): Event;

  /**
   * Sends an event through a connection to Android and returns a promise that is resolved with a response event.
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
   * Subscribes a listener to all notice events pushed by Android.
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
  ): Unsubscribe;

  /**
   * Subscribes a listener to notice events of the particular type pushed by Android.
   *
   * The connection is automatically established when this method is called.
   *
   * @param eventType The type of the event to subscribe to.
   * @param listener The listener to subscribe.
   * @returns The callback that unsubscribes the listener.
   */
  subscribe(
    eventType: string,
    /**
     * @param payload The event payload.
     */
    listener: (payload: any) => void
  ): Unsubscribe;

  /**
   * Returns `true` if an event of the given type is supported, or `false` otherwise.
   *
   * @param eventType The type of event to check.
   */
  isSupported(eventType: string): boolean;
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

  const connect = (): Promise<Required<Connection>> => {
    if (connectionPromise) {
      return connectionPromise;
    }

    const establishConnection = (conn: Connection): Required<Connection> => {
      (conn.inbox ||= new PubSub()).subscribe(([requestId, event]) => {
        if (requestId === -2) {
          noticePubSub.publish(event);
        }
      });
      return (connection = conn as Required<Connection>);
    };

    const conn = connectionProvider();

    let tryCount = 0;

    return (connectionPromise = conn
      ? Promise.resolve(establishConnection(conn))
      : waitFor(connectionProvider, () => 2 ** tryCount++ * 100).then(establishConnection));
  };

  const request = (event: Event): Event => {
    void connect();

    if (connection === null) {
      throw new Error('Expected an established connection');
    }

    const response = JSON.parse(connection.post(JSON.stringify(event)));

    if (typeof response === 'number') {
      throw new Error('Expected a synchronous response');
    }
    if (isException(response)) {
      throw toError(response);
    }
    return response;
  };

  return {
    connect,

    getConnection: () => connection,

    request,

    requestAsync(event) {
      const requestJson = JSON.stringify(event);

      return connect().then(
        connection =>
          new Promise((resolve, reject) => {
            const response = JSON.parse(connection.post(requestJson));

            if (typeof response === 'number') {
              const unsubscribe = connection.inbox.subscribe(([requestId, event]) => {
                if (requestId !== response) {
                  return;
                }

                unsubscribe();

                if (isException(event)) {
                  reject(toError(event));
                } else {
                  resolve(event);
                }
              });
              return;
            }

            if (isException(response)) {
              reject(toError(response));
            } else {
              resolve(response);
            }
          })
      );
    },

    subscribe(eventTypeOrListener, listener?: Function) {
      void connect();

      return noticePubSub.subscribe(
        typeof eventTypeOrListener === 'function'
          ? eventTypeOrListener
          : event => {
              if (event.type === eventTypeOrListener) {
                listener!(event.payload);
              }
            }
      );
    },

    isSupported: eventType =>
      request({ type: 'org.racehorse.IsSupportedEvent', payload: { eventType } }).payload.isSupported,
  };
}

function isException(event: Event): boolean {
  return event.type === 'org.racehorse.ExceptionEvent';
}

function toError(event: Event): Error {
  const error = new Error();

  const jsStack = error.stack;
  const javaStack = Object.assign(error, event.payload.exception).stack;

  if (jsStack !== undefined) {
    const b = jsStack.indexOf('at');
    const a = jsStack.lastIndexOf('\n', b);

    error.stack = (b !== -1 ? javaStack.replace(/\n\t/g, jsStack.substring(a, b)) + '' : '') + jsStack.substring(a + 1);
  }

  return error;
}
