import type { PageMetadata } from '../generated/api';

type PaginationToolbarProps = {
  label: string;
  page: PageMetadata;
  pageSize: number;
  pageSizes: number[];
  onPageSizeChange: (size: number) => void;
  onPrevious: () => void;
  onNext: () => void;
};

export function PaginationToolbar({
  label,
  page,
  pageSize,
  pageSizes,
  onPageSizeChange,
  onPrevious,
  onNext,
}: PaginationToolbarProps) {
  const pageCount = Math.max(page.totalPages, 1);
  const currentPage = Math.min(page.number + 1, pageCount);
  const hasMultiplePages = page.totalPages > 1;

  return (
    <div className="flex flex-col gap-3 border-b border-slate-200 bg-slate-50 px-4 py-3 text-sm sm:flex-row sm:items-center sm:justify-between">
      <label className="flex items-center gap-2 font-semibold text-slate-700">
        <span>{label}</span>
        <select
          className="rounded-md border border-slate-300 bg-white px-2 py-1 font-bold text-slate-900 focus:outline-none focus:ring-2 focus:ring-teal-700"
          value={pageSize}
          onChange={(event) => onPageSizeChange(Number(event.target.value))}
        >
          {pageSizes.map((size) => (
            <option value={size} key={size}>
              {size}
            </option>
          ))}
        </select>
      </label>

      {hasMultiplePages && (
        <div className="flex items-center gap-3">
          <span className="font-semibold text-slate-600">
            Page {currentPage} of {pageCount}
          </span>
          <div className="flex items-center gap-2">
            <button
              className="rounded-md border border-slate-300 bg-white px-3 py-1.5 font-bold text-slate-800 transition hover:border-teal-700 hover:text-teal-800 disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-400 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
              disabled={page.first}
              onClick={onPrevious}
              type="button"
            >
              Previous
            </button>
            <button
              className="rounded-md border border-slate-300 bg-white px-3 py-1.5 font-bold text-slate-800 transition hover:border-teal-700 hover:text-teal-800 disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-400 focus:outline-none focus:ring-2 focus:ring-teal-700 focus:ring-offset-2"
              disabled={page.last}
              onClick={onNext}
              type="button"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
