import { EventBridge } from './createEventBridge';

export interface Contact {
  id: string;
  name: string | null;
  photoUri: string | null;
  emails: string[];
  phoneNumbers: string[];
}

export interface ContactsManager {
  /**
   * Returns a contact picked by the user.
   *
   * @returns The contact or `null` if user didn't pick a contact or didn't grant permissions.
   */
  pickContact(): Promise<Contact | null>;

  /**
   * Get contact by ID.
   *
   * @param contactId The ID of the contact to retrieve.
   * @returns The contact or `null` if there's no such contact.
   */
  getContact(contactId: string): Contact | null;
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

    getContact: contactId =>
      eventBridge.request({ type: 'org.racehorse.GetContactEvent', payload: { contactId } }).payload.contact,
  };
}
