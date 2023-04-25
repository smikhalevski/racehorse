import { PubSub } from 'parallel-universe';

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
  [key: string]: any;
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
   * @return The unique request ID.
   */
  post(eventJson: string): number;
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
   * Sends an event through a connection to Android and returns a promise that is resolved when a response with a
   * matching ID is published to the {@link Connection.inbox}. The returned promise is never rejected.
   *
   * @param event The request event to send.
   * @returns The response event.
   */
  request(event: Event): Promise<Event>;

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

/**
 * The intent that can be passed from and to web application.
 */
export interface WebIntent {
  /**
   * The general action to be performed, such as
   * [`Intent.ACTION_VIEW`](https://developer.android.com/reference/android/content/Intent#ACTION_VIEW).
   *
   * The action describes the general way the rest of the information in the intent should be interpreted — most
   * importantly, what to do with the [uri].
   */
  action?: string;

  /**
   * The data this intent is operating on. This URI specifies the name of the data; often it uses the content: scheme,
   * specifying data in a content provider. Other schemes may be handled by specific activities, such as http: by the
   * web browser.
   */
  uri?: string;

  /**
   * Any special flags associated with this intent, such as
   * [`Intent.FLAG_ACTIVITY_NEW_TASK`](https://developer.android.com/reference/android/content/Intent#FLAG_ACTIVITY_NEW_TASK).
   */
  flags?: number;

  /**
   * A map of extended data from the intent.
   */
  extras?: Record<string, any>;
}

/**
 * The result of an activity.
 *
 * @template T The data returned from the activity.
 */
export interface WebActivityResult<T> {
  /**
   * The result code of a completed activity, such as
   * [Activity.RESULT_OK](https://developer.android.com/reference/android/app/Activity#RESULT_OK)
   */
  resultCode: number;

  /**
   * The data returned from the activity.
   */
  data: T;
}
