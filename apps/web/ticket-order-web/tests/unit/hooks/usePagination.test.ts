import { act, renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { usePagination } from '../../../src/hooks/usePagination';

describe('usePagination', () => {
  it('starts at page 0 with the given default size', () => {
    const { result } = renderHook(() => usePagination(20));

    expect(result.current.pageNumber).toBe(0);
    expect(result.current.pageSize).toBe(20);
  });

  it('advances and retreats the page number', () => {
    const { result } = renderHook(() => usePagination(10));

    act(() => result.current.goToNext());
    expect(result.current.pageNumber).toBe(1);

    act(() => result.current.goToNext());
    expect(result.current.pageNumber).toBe(2);

    act(() => result.current.goToPrevious());
    expect(result.current.pageNumber).toBe(1);
  });

  it('does not go below page 0', () => {
    const { result } = renderHook(() => usePagination(10));

    act(() => result.current.goToPrevious());
    expect(result.current.pageNumber).toBe(0);
  });

  it('resets the page number to 0 when the page size changes', () => {
    const { result } = renderHook(() => usePagination(10));

    act(() => result.current.goToNext());
    act(() => result.current.goToNext());
    expect(result.current.pageNumber).toBe(2);

    act(() => result.current.setPageSize(50));
    expect(result.current.pageNumber).toBe(0);
    expect(result.current.pageSize).toBe(50);
  });

  it('resets the page number to 0 via reset', () => {
    const { result } = renderHook(() => usePagination(10));

    act(() => result.current.goToNext());
    act(() => result.current.reset());
    expect(result.current.pageNumber).toBe(0);
  });
});
