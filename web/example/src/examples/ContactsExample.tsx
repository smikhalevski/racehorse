import React, { useState } from 'react';
import { Contact, contactsManager, fs } from 'racehorse';
import { FormattedJSON } from '../components/FormattedJSON';

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
            src={fs.File(contact.photoUri).toUrl()}
            alt={contact.name || ''}
          />
        </p>
      )}

      <p>
        {'Contact: '}
        <FormattedJSON value={contact} />
      </p>
    </>
  );
}
