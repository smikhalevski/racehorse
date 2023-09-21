import React from 'react';

export function FormattedJSON({ value }: { value: any }) {
  return (
    <pre style={{ whiteSpace: 'pre-wrap' }}>{value === undefined ? 'undefined' : JSON.stringify(value, null, 2)}</pre>
  );
}
