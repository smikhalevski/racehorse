import { AbortableCallback, AbortablePromise } from 'parallel-universe';
import { EventBridge } from './createEventBridge';
import { createScheduler } from './createScheduler';
import { noop } from './utils';
import { Unsubscribe } from './types';

/**
 * The intent that can be passed from and to web application.
 */
export interface Intent {
  __javaClass?: 'android.content.Intent';

  /**
   * The general action to be performed, such as {@link Intent.ACTION_VIEW}.
   */
  action?: string;

  /**
   * The explicit MIME type included in the intent.
   */
  type?: string | null;

  /**
   * The URI-encoded data that intent is operating on.
   */
  data?: string | null;

  /**
   * Special flags associated with this intent, such as {@link Intent.FLAG_ACTIVITY_NEW_TASK}.
   */
  flags?: number;

  /**
   * A map of extended data from the intent.
   */
  extras?: { [key: string]: any } | null;

  /**
   * The array of intent categories.
   */
  categories?: string[] | null;

  /**
   * A selector for this intent.
   *
   * @see [Intent.setSelector](https://developer.android.com/reference/android/content/Intent#setSelector(android.content.Intent))
   */
  selector?: Intent | null;
}

export const ActivityState = {
  /**
   * An activity is in the background and not visible to the user.
   */
  BACKGROUND: 0,

  /**
   * An activity is in the foreground, and it is visible to the user, but doesn't have focus (for example, covered by a
   * dialog).
   */
  FOREGROUND: 1,

  /**
   * An activity is in the foreground and user can interact with it.
   */
  ACTIVE: 2,
} as const;

export type ActivityState = (typeof ActivityState)[keyof typeof ActivityState];

export interface ActivityInfo {
  /**
   * The localized application label.
   */
  applicationLabel: string;

  /**
   * The [application ID](https://developer.android.com/build/configure-app-module) that uniquely identifies your app on
   * the device and in the Google Play Store.
   */
  applicationId: string;

  /**
   * The positive integer used as an internal version number.
   */
  versionCode: number;

  /**
   * The string used as the version number shown to users.
   */
  versionName: string;
}

/**
 * The result of an activity.
 */
export interface ActivityResult {
  /**
   * The result code of a completed activity, such as {@link Activity.RESULT_OK}
   */
  resultCode: number;

  /**
   * The data returned from the activity.
   */
  intent: Intent | null;
}

export const Intent = {
  /**
   * @see [Intent.ACTION_VIEW](https://developer.android.com/reference/android/content/Intent#ACTION_VIEW)
   */
  ACTION_VIEW: 'android.intent.action.VIEW',

  /**
   * @see [Intent.FLAG_ACTIVITY_NEW_TASK](https://developer.android.com/reference/android/content/Intent#FLAG_ACTIVITY_NEW_TASK)
   */
  FLAG_ACTIVITY_NEW_TASK: 0x10000000,

  /**
   * @see [Intent.FLAG_GRANT_READ_URI_PERMISSION](https://developer.android.com/reference/android/content/Intent#FLAG_GRANT_READ_URI_PERMISSION)
   */
  FLAG_GRANT_READ_URI_PERMISSION: 0x00000001,

  /**
   * @see [Intent.FLAG_GRANT_WRITE_URI_PERMISSION](https://developer.android.com/reference/android/content/Intent#FLAG_GRANT_WRITE_URI_PERMISSION)
   */
  FLAG_GRANT_WRITE_URI_PERMISSION: 0x00000002,

  /**
   * @see [Intent.CATEGORY_APP_FILES](https://developer.android.com/reference/android/content/Intent#CATEGORY_APP_FILES)
   */
  CATEGORY_APP_FILES: 'android.intent.category.APP_FILES',
} as const;

export const Activity = {
  /**
   * @see [Activity.RESULT_CANCELED](https://developer.android.com/reference/android/app/Activity#RESULT_CANCELED)
   */
  RESULT_CANCELED: 0,

  /**
   * @see [Activity.RESULT_FIRST_USER](https://developer.android.com/reference/android/app/Activity#RESULT_FIRST_USER)
   */
  RESULT_FIRST_USER: 1,

  /**
   * @see [Activity.RESULT_OK](https://developer.android.com/reference/android/app/Activity#RESULT_OK)
   */
  RESULT_OK: -1,
} as const;

export interface ActivityManager {
  /**
   * Get the state of the activity.
   */
  getActivityState(): ActivityState;

  /**
   * Get info about the current activity.
   */
  getActivityInfo(): ActivityInfo;

  /**
   * Starts an activity for the intent.
   *
   * @param intent The intent that starts an activity.
   * @returns `true` if activity has started, or `false` otherwise.
   */
  startActivity(intent: Intent): boolean;

  /**
   * Starts an activity for the intent and wait for it to return the result.
   *
   * **Note:** This operation requires the user interaction, consider using {@link ActivityManager.runUserInteraction}
   * to ensure that consequent UI-related operations are suspended until this one is completed.
   *
   * @param intent The intent that starts an activity.
   * @returns The activity result.
   */
  startActivityForResult(intent: Intent): Promise<ActivityResult | null>;

  /**
   * Runs an action that blocks the UI.
   *
   * If the activity is in {@link ActivityState.ACTIVE the active state} then the action is run immediately. Otherwise,
   * the active state is awaited and then the action is invoked. Consequent actions that are run using this method are
   * deferred until the current action is resolved.
   *
   * @param action The action callback that must be invoked.
   * @returns The promise to the action result.
   * @template T The result returned by the action.
   */
  runUserInteraction<T>(action: AbortableCallback<T>): AbortablePromise<T>;

  /**
   * Runs the action once the activity is in the expected state.
   *
   * If the activity is in the expected state then the action is run immediately. Otherwise, the expected state is
   * awaited and then the action is invoked.
   *
   * @param expectedState The state when the action must be run.
   * @param action The action callback that must be invoked.
   * @returns The promise to the action result.
   * @template T The result returned by the action.
   */
  runIn<T>(expectedState: ActivityState, action: AbortableCallback<T>): AbortablePromise<T>;

  /**
   * Subscribes a listener to activity status changes.
   */
  subscribe(listener: (activityState: ActivityState) => void): Unsubscribe;

  /**
   * The activity went to background: user doesn't see the activity anymore.
   */
  subscribe(eventType: 'background', listener: () => void): Unsubscribe;

  /**
   * The activity entered foreground: user can see the activity but cannot interact with it.
   */
  subscribe(eventType: 'foreground', listener: () => void): Unsubscribe;

  /**
   * The activity became active: user can see the activity and can interact with it.
   */
  subscribe(eventType: 'active', listener: () => void): Unsubscribe;
}

const eventTypeToActivityState = {
  background: ActivityState.BACKGROUND,
  foreground: ActivityState.FOREGROUND,
  active: ActivityState.ACTIVE,
} as const;

/**
 * Launches activities for various intents, and provides info about the current activity.
 *
 * @param eventBridge The underlying event bridge.
 * @param uiScheduler The scheduler that handles operations that block the UI.
 */
export function createActivityManager(eventBridge: EventBridge, uiScheduler = createScheduler()): ActivityManager {
  const getActivityState = () => eventBridge.request({ type: 'org.racehorse.GetActivityStateEvent' }).payload.state;

  const subscribe: ActivityManager['subscribe'] = (eventTypeOrListener, listener = eventTypeOrListener) => {
    const expectedState = typeof eventTypeOrListener === 'string' ? eventTypeToActivityState[eventTypeOrListener] : -1;

    if (expectedState === undefined || typeof listener !== 'function') {
      return noop;
    }

    return eventBridge.subscribe(
      'org.racehorse.ActivityStateChangedEvent',
      expectedState === -1
        ? payload => listener(payload.state)
        : payload => {
            if (expectedState === payload.state) {
              listener();
            }
          }
    );
  };

  const runIn: ActivityManager['runIn'] = (expectedState, action) =>
    new AbortablePromise((resolve, reject, signal) => {
      const unsubscribe = subscribe(activityState => {
        if (expectedState !== activityState) {
          return;
        }
        unsubscribe();
        try {
          resolve(action(signal));
        } catch (e) {
          reject(e);
        }
      });

      if (expectedState !== getActivityState()) {
        signal.addEventListener('abort', unsubscribe);
        return;
      }
      unsubscribe();
      resolve(action(signal));
    });

  const runUserInteraction: ActivityManager['runUserInteraction'] = action =>
    uiScheduler.schedule(signal => runIn(ActivityState.ACTIVE, action).withSignal(signal));

  return {
    getActivityState,

    getActivityInfo: () => eventBridge.request({ type: 'org.racehorse.GetActivityInfoEvent' }).payload.info,

    startActivity: intent =>
      eventBridge.request({ type: 'org.racehorse.StartActivityEvent', payload: { intent } }).payload.isStarted,

    startActivityForResult: intent =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.StartActivityForResultEvent', payload: { intent } })
        .then(event => event.payload),

    runIn,

    runUserInteraction,

    subscribe,
  };
}
