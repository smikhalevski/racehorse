import React, { useState } from 'react';
import { Contact, contactsManager, fsManager } from 'racehorse';

export function ContactsExample() {
  const [contact, setContact] = useState<Contact | null>(contactsManager.getContact(1));

  return (
    <>
      <h1>{'Contacts'}</h1>

      <p>
        <button
          className="btn btn-primary"
          onClick={() => {
            contactsManager.pickContact().then(setContact);
          }}
        >
          {'Pick contact'}
        </button>
      </p>

      {contact !== null && <ContactCard contact={contact} />}
    </>
  );
}

function ContactCard({ contact }: { contact: Contact }) {
  return (
    <div className="card">
      <div className="row">
        {contact.photoUri !== null && (
          <div className="col-3">
            <img
              className="m-3 w-100 ratio-1x1 rounded-circle"
              src={fsManager.File(contact.photoUri).localUrl}
              alt={'Contact photo'}
            />
          </div>
        )}

        <div className="col-9">
          <div className="card-body">
            <h5 className="card-title">{contact.name}</h5>

            <div className="container">
              <div className="row gap-3">
                <button className="col btn btn-outline-primary">
                  <i className="bi-telephone me-2" />
                  {'Call'}
                </button>

                <button className="col btn btn-outline-primary">
                  <i className="bi-envelope-at me-2" />
                  {'Email'}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      {/*{contact.photoUri !== null && (*/}
      {/*  <img*/}
      {/*    className="card-img-top ratio-1x1"*/}
      {/*    src={fsManager.File(contact.photoUri).localUrl}*/}
      {/*    alt={'Contact photo'}*/}
      {/*  />*/}
      {/*)}*/}
      {/*<div className="card-body">*/}
      {/*  <h5 className="card-title">{contact.name}</h5>*/}
      {/*  /!*    <p class="card-text">Some quick example text to build on the card title and make up the bulk of the card's content.</p>*!/*/}
      {/*  /!*    <a href="#" class="btn btn-primary">Go somewhere</a>*!/*/}
      {/*</div>*/}

      {/*{contact.emails.length !== 0 && (*/}
      {/*  <ul className="list-group list-group-flush">*/}
      {/*    {contact.emails.map(email => (*/}
      {/*      <li className="list-group-item">{email}</li>*/}
      {/*    ))}*/}
      {/*  </ul>*/}
      {/*)}*/}
    </div>
  );
}
