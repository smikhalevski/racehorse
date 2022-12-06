import { createEventBridge, EventBridge } from '../main';

describe('EventBridge', () => {
  test('', () => {
    expect(createEventBridge()).toEqual({});
  });
});
