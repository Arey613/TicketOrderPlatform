import type { AuthenticatedUser } from '../api/authClient';
import type { LoginResponse } from '../generated/api';

export const buyerUser = {
  id: '4b804b8d-6a6f-44d3-9b98-a2ab33f8b8c2',
  email: 'buyer@example.com',
  role: 'CUSTOMER',
} satisfies AuthenticatedUser;

export const newBuyerUser = {
  id: '4f89e87e-9192-4a1a-9baa-7ad90a2ac5fd',
  email: 'new-buyer@example.com',
  role: 'CUSTOMER',
} satisfies AuthenticatedUser;

export const buyerLoginResponse = {
  ...buyerUser,
  enabled: true,
} satisfies LoginResponse;

export const newBuyerLoginResponse = {
  ...newBuyerUser,
  enabled: true,
} satisfies LoginResponse;

export const storedUserKey = 'ticketOrderPlatform.currentUser';
