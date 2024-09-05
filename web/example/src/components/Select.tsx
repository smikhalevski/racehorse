import React, { createContext, ReactElement, ReactNode, useContext, useId } from 'react';

interface SelectState {
  values: any[];
  isMultiple: boolean | undefined;
  onChange: (value: any, isChecked: boolean) => void;
}

const SelectStateContext = createContext<SelectState | null>(null);

interface SelectProps<T> {
  values: T[];
  children: ReactNode;
  isMultiple?: boolean;
  onChange: (value: T[]) => void;
}

export function Select<T>({ values, children, isMultiple, onChange }: SelectProps<T>): ReactElement {
  return (
    <SelectStateContext.Provider
      value={{
        values,
        isMultiple,

        onChange(value, isChecked) {
          if (!isMultiple) {
            if (isChecked) {
              onChange([value]);
            }
            return;
          }

          if (values.indexOf(value) === -1) {
            if (isChecked) {
              onChange([...values, value]);
            }
            return;
          }

          if (isChecked) {
            return;
          }

          const nextValues = values.slice(0);
          nextValues.splice(values.indexOf(value), 1);
          onChange(nextValues);
        },
      }}
    >
      <ul className="list-group">{children}</ul>
    </SelectStateContext.Provider>
  );
}

interface SelectOptionProps {
  value: unknown;
  children: ReactNode;
}

export function SelectOption({ value, children }: SelectOptionProps): ReactElement {
  const state = useContext(SelectStateContext);
  const id = useId();

  if (state === null) {
    throw new Error('Cannot render outside of Select');
  }

  return (
    <li className="list-group-item d-flex">
      <input
        className="form-check-input me-2"
        type={state.isMultiple ? 'checkbox' : 'radio'}
        checked={state.values.includes(value)}
        onChange={event => state.onChange(value, event.target.checked)}
        id={id}
      />
      <label
        className="form-check-label flex-fill"
        htmlFor={id}
      >
        {children}
      </label>
    </li>
  );
}
