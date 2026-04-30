import { CustomerStore } from './store';

describe('CustomerStore', () => {
  let store: CustomerStore;

  beforeEach(() => {
    store = new CustomerStore();
  });

  test('create assigns sequential IDs', () => {
    const c1 = store.create({ firstName: 'A', lastName: 'B', email: 'a@b.com' });
    const c2 = store.create({ firstName: 'C', lastName: 'D', email: 'c@d.com' });
    expect(c1.id).toBe('cust_1');
    expect(c2.id).toBe('cust_2');
  });

  test('create sets timestamps', () => {
    const c = store.create({ firstName: 'A', lastName: 'B', email: 'a@b.com' });
    expect(c.createdAt).toBeDefined();
    expect(c.updatedAt).toBeDefined();
  });

  test('get returns created customer', () => {
    const c = store.create({ firstName: 'A', lastName: 'B', email: 'a@b.com' });
    const fetched = store.get(c.id);
    expect(fetched).toBeDefined();
    expect(fetched!.firstName).toBe('A');
  });

  test('get returns undefined for missing', () => {
    expect(store.get('cust_999')).toBeUndefined();
  });

  test('patch updates fields', () => {
    const c = store.create({ firstName: 'A', lastName: 'B', email: 'a@b.com' });
    const updated = store.patch(c.id, { firstName: 'Z' });
    expect(updated).toBeDefined();
    expect(updated!.firstName).toBe('Z');
    expect(updated!.lastName).toBe('B');
  });

  test('patch preserves id and createdAt', () => {
    const c = store.create({ firstName: 'A', lastName: 'B', email: 'a@b.com' });
    const updated = store.patch(c.id, { firstName: 'Z' });
    expect(updated!.id).toBe(c.id);
    expect(updated!.createdAt).toBe(c.createdAt);
  });

  test('patch returns undefined for missing customer', () => {
    expect(store.patch('cust_999', { firstName: 'Z' })).toBeUndefined();
  });

  test('getPreferences returns undefined when none set', () => {
    const c = store.create({ firstName: 'A', lastName: 'B', email: 'a@b.com' });
    expect(store.getPreferences(c.id)).toBeUndefined();
  });

  test('putPreferences and getPreferences round-trip', () => {
    const c = store.create({ firstName: 'A', lastName: 'B', email: 'a@b.com' });
    const prefs = store.putPreferences(c.id, {
      emailOptIn: true, smsOptIn: false, marketingOptIn: false, locale: 'en-US',
    });
    expect(prefs.customerId).toBe(c.id);
    expect(prefs.updatedAt).toBeDefined();

    const fetched = store.getPreferences(c.id);
    expect(fetched).toEqual(prefs);
  });
});
