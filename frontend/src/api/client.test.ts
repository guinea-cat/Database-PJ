import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiGet, apiPost } from './client';

const fetchMock = vi.fn();

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal('fetch', fetchMock);
});

describe('api client HTTP semantics', () => {
  it('uses GET with query string and no request body for query operations', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ code: 0, message: 'success', data: [{ id: 1 }], timestamp: '2026-06-09T12:00:00' }),
    });

    await apiGet('/flight/search', { departureCityId: 1, arrivalCityId: 2, cabinClass: undefined });

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/flight/search?departureCityId=1&arrivalCityId=2',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(fetchMock.mock.calls[0][1]).not.toHaveProperty('body');
  });

  it('uses POST with JSON body for state-changing operations', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ code: 0, message: 'success', data: { ticketId: 10001 }, timestamp: '2026-06-09T12:00:00' }),
    });

    await apiPost('/ticket/pay', { ticketId: 10001 });

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/ticket/pay',
      expect.objectContaining({
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ticketId: 10001 }),
      }),
    );
  });

  it('throws business errors when the backend response code is not zero', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ code: 42001, message: '当前航段余票不足', data: null, timestamp: '2026-06-09T12:00:00' }),
    });

    await expect(apiPost('/ticket/create', { segmentId: 1 })).rejects.toThrow('当前航段余票不足');
  });
});
