import { createEventBridge, Event } from 'racehorse';
import { Fragment, useEffect, useState } from 'react';

const eventBridge = createEventBridge();

export function App() {
  const [events, setEvents] = useState<Event[]>([]);

  useEffect(() => {
    eventBridge.subscribe(event => {
      setEvents(events => events.concat(event));
    });

    eventBridge.request({ type: 'com.example.myapplication.UnknownEvent' }).then(
      event => {
        setEvents(events => events.concat(event));
      }
      // error => {
      //   setEvents(events => events.concat(error));
      // }
    );
  });

  return (
    <>
      <h1>Events:</h1>

      {events.map((event, i) => (
        <Fragment key={i}>
          <pre>{JSON.stringify(event, null, 2)}</pre>
          <hr />
        </Fragment>
      ))}
    </>
  );
}
