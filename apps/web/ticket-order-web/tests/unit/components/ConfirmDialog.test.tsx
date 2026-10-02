import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ConfirmDialog } from '../../../src/components/ConfirmDialog';

describe('ConfirmDialog', () => {
  afterEach(() => {
    document.body.replaceChildren();
    document.body.style.overflow = '';
    vi.restoreAllMocks();
  });

  it('moves focus to the cancel button on open and returns focus to the trigger on close', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    const trigger = renderTrigger();
    trigger.focus();

    const { unmount } = renderDialog({ onCancel });

    expect(screen.getByRole('button', { name: 'Keep order' })).toHaveFocus();

    await user.click(screen.getByRole('button', { name: 'Keep order' }));
    expect(onCancel).toHaveBeenCalledOnce();

    unmount();
    expect(trigger).toHaveFocus();
  });

  it('keeps the original trigger as the focus target after pending toggles', () => {
    const trigger = renderTrigger();
    trigger.focus();

    const { rerender, unmount } = renderDialog();

    expect(screen.getByRole('button', { name: 'Keep order' })).toHaveFocus();

    rerender(dialogElement({ isPending: true }));
    expect(document.body.style.overflow).toBe('hidden');

    rerender(dialogElement({ isPending: false }));
    expect(document.body.style.overflow).toBe('hidden');

    unmount();
    expect(document.body.style.overflow).toBe('');
    expect(trigger).toHaveFocus();
  });

  it('closes with Escape only when not pending', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    const { rerender } = renderDialog({ onCancel, isPending: true });

    await user.keyboard('{Escape}');
    expect(onCancel).not.toHaveBeenCalled();

    rerender(dialogElement({ onCancel, isPending: false }));
    await user.keyboard('{Escape}');
    expect(onCancel).toHaveBeenCalledOnce();
  });

  it('wraps Tab and Shift Tab inside the dialog', async () => {
    const user = userEvent.setup();
    vi.spyOn(HTMLElement.prototype, 'offsetParent', 'get').mockReturnValue(document.body);
    renderDialog();
    const dialog = screen.getByRole('dialog', { name: 'Cancel order?' });
    const cancelButton = within(dialog).getByRole('button', { name: 'Keep order' });
    const confirmButton = within(dialog).getByRole('button', { name: 'Cancel order' });

    expect(cancelButton).toHaveFocus();

    await user.keyboard('{Shift>}{Tab}{/Shift}');
    expect(confirmButton).toHaveFocus();

    await user.tab();
    expect(cancelButton).toHaveFocus();
  });

  it('locks body scroll while open and restores it after close', () => {
    document.body.style.overflow = 'auto';
    const { rerender, unmount } = renderDialog();

    expect(document.body.style.overflow).toBe('hidden');

    rerender(dialogElement({ isPending: true }));
    expect(document.body.style.overflow).toBe('hidden');

    rerender(dialogElement({ isPending: false }));
    expect(document.body.style.overflow).toBe('hidden');

    unmount();
    expect(document.body.style.overflow).toBe('auto');
  });
});

function renderDialog(options: Partial<Parameters<typeof dialogElement>[0]> = {}) {
  return render(dialogElement(options));
}

function dialogElement({
  isPending = false,
  onCancel = vi.fn(),
  onConfirm = vi.fn(),
}: {
  isPending?: boolean;
  onCancel?: () => void;
  onConfirm?: () => void;
} = {}) {
  return (
    <ConfirmDialog
      cancelLabel="Keep order"
      confirmLabel="Cancel order"
      isPending={isPending}
      onCancel={onCancel}
      onConfirm={onConfirm}
      pendingConfirmLabel="Cancelling order"
      title="Cancel order?"
    >
      <p>This removes your booking.</p>
    </ConfirmDialog>
  );
}

function renderTrigger() {
  const trigger = document.createElement('button');
  trigger.type = 'button';
  trigger.textContent = 'Cancel order for The Horizon Live';
  document.body.append(trigger);
  return trigger;
}
