import { EventBridge } from './createEventBridge';

export interface Contact {
  name: string | null;
  photoUri: string | null;
  emails: string[];
  phoneNumbers: string[];
}

export interface ContactsManager {
  /**
   * Returns a contact picked by the user.
   */
  pickContact(): Promise<Contact | null>;
}

/**
 * Provides access to phone contacts.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createContactsManager(eventBridge: EventBridge): ContactsManager {
  return {
    pickContact: () =>
      eventBridge.requestAsync({ type: 'org.racehorse.PickContactEvent' }).then(event => event.payload.contact),
  };
}
