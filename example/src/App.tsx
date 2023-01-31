import { Event } from 'racehorse';
import { useEventBridge, useEventBridgeSubscription } from '@racehorse/react';
import { Fragment, useEffect, useState } from 'react';

export function App() {
  const [events, setEvents] = useState<Event[]>([]);

  const eventBridge = useEventBridge();

  useEventBridgeSubscription(event => {
    setEvents(events => events.concat(event));
  });

  useEffect(() => {
    eventBridge.request({ type: 'com.example.myapplication.UnknownEvent' }).then(event => {
      setEvents(events => events.concat(event));
    });
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
