import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const contractRoot = resolve(__dirname, '../../../../../../contracts/openapi/ticket-order-api/src');
const openApi = readContract('openapi.yml');
const ordersPath = readContract('paths/orders.yml');
const eventOrdersPath = readContract('paths/event-orders.yml');
const orderSchema = readContract('schemas/order.yml');
const eventOrderSchema = readContract('schemas/event-order.yml');
const paginationSchema = readContract('schemas/pagination.yml');

describe('my orders OpenAPI contract', () => {
  it('registers the canonical orders endpoint and removes the old event-scoped endpoint', () => {
    expect(openApi).toContain('"/orders/mine":');
    expect(openApi).toContain('"$ref": "./paths/orders.yml#/~1orders~1mine"');
    expect(openApi).not.toContain('"/events/orders/mine":');
    expect(eventOrdersPath).not.toContain('"/events/orders/mine":');
  });

  it('exposes listMyOrders with pagination parameters and no customer identity input', () => {
    expect(ordersPath).toContain('operationId: listMyOrders');
    expect(ordersPath).toContain('"$ref": "#/components/parameters/PageParameter"');
    expect(ordersPath).toContain('name: size');
    expect(ordersPath).toContain('"$ref": "#/components/parameters/SortParameter"');
    expect(paginationSchema).toContain('name: page');
    expect(paginationSchema).toContain('name: sort');
    expect(ordersPath).toContain('"$ref": "#/components/schemas/MyOrdersResponse"');

    expect(ordersPath).not.toMatch(/\bcustomerId\b/i);
    expect(ordersPath).not.toMatch(/\buserId\b/i);
    expect(ordersPath).not.toMatch(/\bemail\b/i);
    expect(ordersPath).not.toMatch(/\bowner\b/i);
  });

  it('uses dedicated order schemas without customer identity fields', () => {
    expect(openApi).toContain('MyOrderResponse:');
    expect(openApi).toContain('MyOrdersResponse:');
    expect(orderSchema).toContain('MyOrderResponse:');
    expect(orderSchema).toContain('MyOrdersResponse:');
    expect(orderSchema).toContain('eventOrderId:');
    expect(orderSchema).toContain('eventName:');
    expect(orderSchema).toContain('eventDate:');
    expect(orderSchema).toContain('eventPlace:');
    expect(eventOrderSchema).not.toContain('MyEventOrderResponse:');
    expect(eventOrderSchema).not.toContain('MyEventOrdersResponse:');

    const myOrderSchema = orderSchema.slice(orderSchema.indexOf('MyOrderResponse:'));

    expect(myOrderSchema).not.toMatch(/\bcustomerId\b/i);
    expect(myOrderSchema).not.toMatch(/\bcustomerEmail\b/i);
    expect(myOrderSchema).not.toMatch(/\bpassword\b/i);
    expect(myOrderSchema).not.toMatch(/\bsession\b/i);
  });
});

function readContract(path: string): string {
  return readFileSync(resolve(contractRoot, path), 'utf8');
}
