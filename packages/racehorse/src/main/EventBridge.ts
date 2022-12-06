interface Ok<T> {
  ok: true;
  requestId: number;
  javaClass: string;
  event: T;
}

interface Err {
  ok: false;
  requestId: number;
  javaClass: string;
  message: string;
  stack: string;
}

interface EventBridgeRequest {
  requestId: number;
  javaClass: string;
  eventJson: string;
}

export interface EventBridgeRequests {
  'com.example.myapplication.FooEvent': {
    www: number;
  };
  'com.example.myapplication.AAA': {
    www: number;
  };
}

export interface EventBridgeResponses {
  'com.example.myapplication.FooEvent': {
    www: number;
  };
  'com.example.myapplication.AAA': {
    www: number;
  };
}

export interface EventBridge<I, O> {
  request<T extends keyof I & keyof O>(javaClass: T, event: O[T]): Promise<I[T]>;

  subscribe<T extends keyof O>(javaClass: T, listener: (event: O[T]) => void): () => void;
}

export function createEventBridge<I, O = unknown>(): EventBridge<I, O> {
  let nonce = 0;

  const pendingRequestsMap = new Map<number, { resolve(value: any): void; reject(reason: any): void }>();
  const listenersMap = new Map<string, Set<(event: any) => void>>();

  // window.__inbox = {
  //   push(response: Ok<any> | Err) {
  //     const { requestId } = response;
  //
  //     if (requestId === -1) {
  //       if (response.ok) {
  //         listenersMap.get(response.javaClass)?.forEach(listener => {
  //           listener(response.event);
  //         });
  //         return;
  //       }
  //       throw new Error(response.message);
  //     }
  //
  //     const deferred = pendingRequestsMap.get(requestId);
  //
  //     if (deferred === undefined) {
  //       throw new Error('Received an unexpected response');
  //     }
  //     if (response.ok) {
  //       deferred.resolve(response.event);
  //     } else {
  //       deferred.reject(response);
  //     }
  //     pendingRequestsMap.delete(requestId);
  //   }
  // };

  const request = (javaClass: any, event: any): Promise<any> => {
    return new Promise((resolve, reject) => {
      const requestId = nonce++;
      const request: EventBridgeRequest = { requestId, javaClass, eventJson: JSON.stringify(event) };

      pendingRequestsMap.set(requestId, { resolve, reject });

      // window.__outbox.push(JSON.stringify(request));
    });
  };

  const subscribe = (javaClass: any, listener: (event: any) => void): (() => void) => {
    let listeners = listenersMap.get(javaClass);

    if (listeners === undefined) {
      listeners = new Set();
      listenersMap.set(javaClass, listeners);
    }

    listeners.add(listener);

    return () => {
      listeners!.delete(listener);

      if (listeners!.size === 0) {
        listenersMap.delete(javaClass);
      }
    };
  };

  return { request, subscribe };
}

const eventBridge = createEventBridge<EventBridgeRequests, EventBridgeResponses>();

eventBridge.request('com.example.myapplication.FooEvent', { www: 123 }).then(val => {
  val.www;
});
