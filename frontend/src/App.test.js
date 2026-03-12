import { render, screen } from '@testing-library/react';
import App from './App';

jest.mock('@auth0/auth0-react', () => ({
  useAuth0: () => ({
    isLoading: false,
    isAuthenticated: false,
    user: null,
    error: null,
    loginWithRedirect: jest.fn(),
    logout: jest.fn(),
  }),
}));

test('renders auth0 entry actions', () => {
  render(<App />);
  expect(screen.getByRole('button', { name: /entrar/i })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /criar conta/i })).toBeInTheDocument();
});
