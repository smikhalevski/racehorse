import { createContext, ReactNode, useContext, useEffect } from 'react';

export const TitleContext = createContext<(title: ReactNode) => void>(() => {});

export function Heading({ children }: { children: ReactNode }) {
  const setTitle = useContext(TitleContext);

  useEffect(() => {
    setTitle(children);
  }, [children]);

  return null;
}
