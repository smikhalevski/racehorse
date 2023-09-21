import { EventBridge } from './createEventBridge';

/**
 * The intent that can be passed from and to web application.
 */
export interface Intent {
  /**
   * The general action to be performed, such as {@link Intent.ACTION_VIEW}.
   */
  action?: string;

  /**
   * The explicit MIME type included in the intent.
   */
  type?: string;

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

  /**
   * The list of intent categories.
   */
  categories?: string[];

  /**
   * A selector for this intent.
   *
   * @see [Intent.setSelector](https://developer.android.com/reference/android/content/Intent#setSelector(android.content.Intent))
   */
  selector?: Intent;
}

export interface ActivityInfo {
  /**
   * The package name of the running activity.
   */
  packageName: string;
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
   * Start an activity for the intent and wait for it to return the result.
   *
   * @param intent The intent that starts an activity.
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
    getActivityInfo: () => eventBridge.request({ type: 'org.racehorse.GetActivityInfoEvent' }).payload.activityInfo,

    startActivity: intent =>
      eventBridge.request({ type: 'org.racehorse.StartActivityEvent', payload: { intent } }).payload.isStarted,

    startActivityForResult: intent =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.StartActivityForResultEvent', payload: { intent } })
        .then(event => event.payload),
  };
}
