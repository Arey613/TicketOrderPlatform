import { describe, expect, it, vi } from 'vitest';
import { notifySessionExpired, onSessionExpired } from '../../../src/api/sessionEvents';

describe('sessionEvents', () => {
  it('notifies subscribed listeners when the session expires', () => {
    const listener = vi.fn();
    const unsubscribe = onSessionExpired(listener);

    notifySessionExpired();

    expect(listener).toHaveBeenCalledTimes(1);
    unsubscribe();
  });

  it('stops notifying a listener after it unsubscribes', () => {
    const listener = vi.fn();
    const unsubscribe = onSessionExpired(listener);

    unsubscribe();
    notifySessionExpired();

    expect(listener).not.toHaveBeenCalled();
  });

  it('notifies multiple independent listeners', () => {
    const firstListener = vi.fn();
    const secondListener = vi.fn();
    const unsubscribeFirst = onSessionExpired(firstListener);
    const unsubscribeSecond = onSessionExpired(secondListener);

    notifySessionExpired();

    expect(firstListener).toHaveBeenCalledTimes(1);
    expect(secondListener).toHaveBeenCalledTimes(1);
    unsubscribeFirst();
    unsubscribeSecond();
  });
});
