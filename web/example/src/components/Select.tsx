import React, { createContext, InputHTMLAttributes, ReactElement, ReactNode, useContext, useId } from 'react';

interface SelectState {
  values: any[];
  isMultiple: boolean | undefined;

  onChange(value: any, isChecked: boolean): void;
}

const SelectStateContext = createContext<SelectState | null>(null);

interface SelectProps<T> {
  values: T[];
  children: ReactNode;
  multiple?: boolean;

  onChange(value: T[]): void;
}

export function Select<T>(props: SelectProps<T>): ReactElement {
  return (
    <SelectStateContext.Provider
      value={{
        values: props.values,
        isMultiple: props.multiple,

        onChange(value, isChecked) {
          if (!props.multiple) {
            if (isChecked) {
              props.onChange([value]);
            }
            return;
          }

          if (props.values.indexOf(value) === -1) {
            if (isChecked) {
              props.onChange([...props.values, value]);
            }
            return;
          }

          if (isChecked) {
            return;
          }

          const values = props.values.slice(0);
          values.splice(values.indexOf(value), 1);
          props.onChange(values);
        },
      }}
    >
      <ul className="list-group">{props.children}</ul>
    </SelectStateContext.Provider>
  );
}

interface SelectOptionProps {
  value: unknown;
  children: ReactNode;
}

export function SelectOption(props: SelectOptionProps): ReactElement {
  const state = useContext(SelectStateContext);
  const id = useId();

  if (state === null) {
    throw new Error('Cannot be used outside of Select');
  }

  return (
    <li className="list-group-item d-flex">
      <input
        className="form-check-input me-2"
        type={state.isMultiple ? 'checkbox' : 'radio'}
        checked={state.values.includes(props.value)}
        onChange={event => {
          state.onChange(props.value, event.target.checked);
        }}
        id={id}
      />
      <label
        className="form-check-label flex-fill"
        htmlFor={id}
      >
        {props.children}
      </label>
    </li>
  );
}

interface CheckboxProps extends InputHTMLAttributes<HTMLInputElement> {}

export function Checkbox({ children, ...props }: CheckboxProps): ReactElement {
  const id = useId();
  return (
    <>
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
    </>
  );
}
