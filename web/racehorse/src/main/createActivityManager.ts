import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface ActivityInfo {
  /**
   * The package name of the running activity.
   */
  packageName: string;
}

/**
 * The intent that can be passed from and to web application.
 */
export interface Intent {
  /**
   * The general action to be performed, such as {@link Intent.ACTION_VIEW}.
   */
  action?: string;

  /**
   * The URI-encoded data that intent is operating on.
   */
  data?: string;

  /**
   * Special flags associated with this intent, such as {@link Intent.FLAG_ACTIVITY_NEW_TASK}.
   */
  flags?: number;

  /**
   * A map of extended data from the intent.
   */
  extras?: { [key: string]: any };
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
  data: Intent | null;
}

export const Intent = {
  /**
   * @see {@link https://developer.android.com/reference/android/content/Intent#ACTION_VIEW Intent.ACTION_VIEW}
   */
  ACTION_VIEW: 'android.intent.action.VIEW',

  /**
   * @see {@link https://developer.android.com/reference/android/content/Intent#FLAG_ACTIVITY_NEW_TASK Intent.FLAG_ACTIVITY_NEW_TASK}
   */
  FLAG_ACTIVITY_NEW_TASK: 0x10000000,
} as const;

export const Activity = {
  /**
   * @see {@link https://developer.android.com/reference/android/app/Activity#RESULT_CANCELED Activity.RESULT_CANCELED}
   */
  RESULT_CANCELED: 0,

  /**
   * @see {@link https://developer.android.com/reference/android/app/Activity#RESULT_FIRST_USER Activity.RESULT_FIRST_USER}
   */
  RESULT_FIRST_USER: 1,

  /**
   * @see {@link https://developer.android.com/reference/android/app/Activity#RESULT_OK Activity.RESULT_OK}
   */
  RESULT_OK: -1,
} as const;

export interface ActivityManager {
  /**
   * Get info about the current activity.
   */
  getActivityInfo(): Promise<ActivityInfo>;

  /**
   * Starts an activity for the intent.
   *
   * @returns `true` if activity has started, or `false` otherwise.
   */
  startActivity(intent: Intent): Promise<boolean>;

  /**
   * Start an activity for the intent.
   *
   * @returns The activity result.
   */
  startActivityForResult(intent: Intent): Promise<ActivityResult | null>;
}

/**
 * Launches activities for various intents.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createActivityManager(eventBridge: EventBridge): ActivityManager {
  return {
    getActivityInfo: () =>
      eventBridge
        .request({ type: 'org.racehorse.GetActivityInfoEvent' })
        .then(event => ensureEvent(event).activityInfo),

    startActivity: intent =>
      eventBridge
        .request({ type: 'org.racehorse.StartActivityEvent', intent })
        .then(event => ensureEvent(event).isStarted),

    startActivityForResult: intent =>
      eventBridge.request({ type: 'org.racehorse.StartActivityForResultEvent', intent }).then(event => {
        event = ensureEvent(event);
        return { resultCode: event.resultCode, data: event.data };
      }),
  };
}
