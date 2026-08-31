import { useState } from 'react';

export type Pagination = {
  pageNumber: number;
  pageSize: number;
  setPageSize: (size: number) => void;
  goToPrevious: () => void;
  goToNext: () => void;
  reset: () => void;
};

export function usePagination(defaultPageSize: number): Pagination {
  const [pageNumber, setPageNumber] = useState(0);
  const [pageSize, setPageSize] = useState(defaultPageSize);

  return {
    pageNumber,
    pageSize,
    setPageSize: (size: number) => {
      setPageSize(size);
      setPageNumber(0);
    },
    goToPrevious: () => setPageNumber((page) => Math.max(0, page - 1)),
    goToNext: () => setPageNumber((page) => page + 1),
    reset: () => setPageNumber(0),
  };
}
