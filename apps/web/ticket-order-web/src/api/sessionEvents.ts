const sessionEventTarget = new EventTarget();
const SESSION_EXPIRED_EVENT = 'session-expired';

export function notifySessionExpired(): void {
  sessionEventTarget.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
}

export function onSessionExpired(listener: () => void): () => void {
  sessionEventTarget.addEventListener(SESSION_EXPIRED_EVENT, listener);
  return () => sessionEventTarget.removeEventListener(SESSION_EXPIRED_EVENT, listener);
}
