import React, { ReactElement, ReactNode } from 'react';

interface SectionProps {
  title: ReactNode;
  children: ReactNode;
}

export function Section(props: SectionProps): ReactElement {
  return (
    <section className="mb-4">
      <h1>{props.title}</h1>
      
      {props.children}
    </section>
  );
}
