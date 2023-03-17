import { ResponseEvent } from './types';

/**
 * Throws an error if an event carries an exception, or returns event as is.
 *
 * @param event The event to ensure isn't an exception.
 */
export function ensureEvent(event: ResponseEvent): ResponseEvent {
  if (event.type === 'org.racehorse.webview.ExceptionResponseEvent') {
    throw new Error(event.stackTrace);
  }
  return event;
}
