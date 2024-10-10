import React, { InputHTMLAttributes, ReactElement, useId } from 'react';
import clsx from 'clsx';

export function Checkbox(props: InputHTMLAttributes<HTMLInputElement>): ReactElement {
  const { className, children, ...inputProps } = props;

  const id = useId();

  return (
    <div className={clsx('d-flex', className)}>
      <input
        {...inputProps}
        className="form-check-input me-2"
        type="checkbox"
        id={id}
      />
      <label
        className="form-check-label flex-fill"
        htmlFor={id}
      >
        {children}
      </label>
    </div>
  );
}
