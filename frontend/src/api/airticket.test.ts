import { beforeEach, describe, expect, it, vi } from 'vitest';
import { searchFlights } from './airticket';

const fetchMock = vi.fn();

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal('fetch', fetchMock);
});

describe('airticket API wrappers', () => {
  it('searches flights by city ids for passenger city-pair search', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ code: 0, message: 'success', data: [], timestamp: '2026-06-09T12:00:00' }),
    });

    await searchFlights({ departureCityId: 1, arrivalCityId: 2, flightDate: '2026-07-01' });

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/flight/search?departureCityId=1&arrivalCityId=2&flightDate=2026-07-01',
      expect.objectContaining({ method: 'GET' }),
    );
  });
});
