import { EventBridge } from './createEventBridge.js';
import { noop } from './utils.js';

export interface FacebookShareLinkContent {
  /**
   * The quote to display for this link.
   */
  quote?: string;

  /**
   * The URL for the content being shared.
   */
  contentUrl?: string;

  /**
   * The array of IDs for taggable people to tag with this content.
   */
  peopleIds?: string[];

  /**
   * The ID for a place to tag with this content.
   */
  placeId?: string;

  /**
   * The ID of the Facebook page this share is associated with.
   */
  pageId?: string;

  /**
   * The value to be added to the referrer URL when a person follows a link from this shared content on feed.
   */
  ref?: string;

  /**
   * The hashtag for this content
   */
  hashtag?: string;
}

export interface FacebookShareManager {
  /**
   * Opens Facebook link share popup.
   *
   * **Note:** This operation requires the user interaction, consider using {@link ActivityManager.runUserInteraction}
   * to ensure that consequent UI-related operations are suspended until this one is completed.
   */
  shareLink(content: FacebookShareLinkContent): Promise<void>;
}

/**
 * Manages Facebook content sharing.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createFacebookShareManager(eventBridge: EventBridge): FacebookShareManager {
  return {
    shareLink: content =>
      eventBridge.requestAsync({ type: 'org.racehorse.FacebookShareLinkEvent', payload: content }).then(noop),
  };
}
