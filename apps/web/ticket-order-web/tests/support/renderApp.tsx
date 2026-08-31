import App from '../../src/App';
import { renderWithQueryClient } from './renderWithQueryClient';

export function renderApp() {
  return renderWithQueryClient(<App />);
}
