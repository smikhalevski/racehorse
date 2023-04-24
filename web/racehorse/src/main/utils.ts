import { Event } from './types';

/**
 * Throws an error if an event carries an exception, or returns event as is.
 *
 * @param event The event to ensure isn't an exception.
 */
export function ensureEvent(event: Event): Event {
  if (event.type === 'org.racehorse.ExceptionEvent') {
    throw new Error(event.stackTrace);
  }
  return event;
}
