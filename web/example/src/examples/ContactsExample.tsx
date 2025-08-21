import React, { useState } from 'react';
import { Contact, contactsManager, fsManager } from 'racehorse';
import { FormattedJSON } from '../components/FormattedJSON.js';

export function ContactsExample() {
  const [contact, setContact] = useState<Contact | null>(null);

  return (
    <>
      <h2>{'Contacts'}</h2>

      <p>
        <button
          onClick={() => {
            contactsManager.pickContact().then(setContact);
          }}
        >
          {'Pick contact'}
        </button>
      </p>

      {contact !== null && contact.photoUri !== null && (
        <p>
          <img
            style={{ display: 'block', width: '100px', height: '100px', borderRadius: '50%' }}
            src={fsManager.open(contact.photoUri).localUrl}
            alt={contact.name || ''}
          />
        </p>
      )}

      {'Contact: '}
      <FormattedJSON value={contact} />
    </>
  );
}
