import React, { ReactNode } from 'react';

export function Section(props: { title: ReactNode; children: ReactNode }) {
  return (
    <section className="mb-4">
      <h3>{props.title}</h3>

      {props.children}
    </section>
  );
}
