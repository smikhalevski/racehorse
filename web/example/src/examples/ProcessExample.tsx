import React, { useEffect, useState } from 'react';
import { processManager, ProcessState } from 'racehorse';

const processStateLabel = {
  [ProcessState.BACKGROUND]: '🔴 Background',
  [ProcessState.FOREGROUND]: '🟡 Foreground',
  [ProcessState.ACTIVE]: '🟢 Active',
} as const;

export function ProcessExample() {
  const [processStateHistory, setProcessStateHistory] = useState(() => [
    { timestamp: Date.now(), state: processManager.getProcessState() },
  ]);

  useEffect(
    () =>
      processManager.subscribe(state =>
        setProcessStateHistory(processStateHistory => [...processStateHistory, { timestamp: Date.now(), state }])
      ),
    []
  );

  return (
    <>
      <h2>{'Process'}</h2>

      <ol>
        {processStateHistory.map((record, i) => (
          <li key={i}>
            <span style={{ fontVariant: 'tabular-nums' }}>
              {new Date(record.timestamp).toISOString().split('T')[1].slice(0, -5)}
            </span>{' '}
            {processStateLabel[record.state]}
          </li>
        ))}
      </ol>
    </>
  );
}
