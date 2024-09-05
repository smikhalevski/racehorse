import React, { ReactElement, ReactNode } from 'react';

interface SectionProps {
  title: ReactNode;
  children: ReactNode;
}

export function Section({ title, children }: SectionProps): ReactElement {
  return (
    <section className="mb-4">
      <h1>{title}</h1>
      {children}
    </section>
  );
}
