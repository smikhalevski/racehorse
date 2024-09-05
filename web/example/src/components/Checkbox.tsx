import React, { InputHTMLAttributes, ReactElement, useId } from 'react';
import clsx from 'clsx';

export function Checkbox({ children, className, ...props }: InputHTMLAttributes<HTMLInputElement>): ReactElement {
  const id = useId();

  return (
    <div className={clsx('d-flex', className)}>
      <input
        {...props}
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
