import React, { ReactElement } from 'react';

interface FormattedJSONProps {
  value: any;
}

export function FormattedJSON(props: FormattedJSONProps): ReactElement {
  return <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(props.value, null, 2)}</pre>;
}
