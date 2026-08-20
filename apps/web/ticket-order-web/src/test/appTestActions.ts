import { screen, within } from '@testing-library/react';
import type { UserEvent } from '@testing-library/user-event';

export async function submitLoginForm(user: UserEvent, email: string, password: string) {
  await user.click(screen.getAllByRole('button', { name: 'Login' })[0]);
  const dialog = await screen.findByRole('dialog', { name: 'Login' });
  await user.type(await screen.findByLabelText('Email'), email);
  await user.type(screen.getByLabelText('Password'), password);
  await user.click(within(dialog).getByRole('button', { name: 'Login' }));
}

export async function submitRegistrationForm(user: UserEvent, email: string, password: string) {
  await user.click(screen.getAllByRole('button', { name: 'Create account' })[0]);
  const dialog = await screen.findByRole('dialog', { name: 'Create account' });
  await user.type(await within(dialog).findByLabelText('Email'), email);
  await user.type(screen.getByLabelText('Password'), password);
  await user.click(within(dialog).getByRole('button', { name: 'Create account' }));
}
