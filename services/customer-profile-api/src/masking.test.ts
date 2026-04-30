import { maskEmail, maskPhone, maskedView, initials } from './masking';
import { Customer } from './types';

describe('masking', () => {
  describe('maskEmail', () => {
    test('normal email', () => {
      const result = maskEmail('alice@example.com');
      expect(result).toBe('a***e@example.com');
    });

    test('short username', () => {
      expect(maskEmail('ab@example.com')).toBe('*@example.com');
    });

    test('single char username', () => {
      expect(maskEmail('a@example.com')).toBe('*@example.com');
    });

    test('no domain', () => {
      expect(maskEmail('nodomain')).toBe('nodomain');
    });
  });

  describe('maskPhone', () => {
    test('standard number', () => {
      expect(maskPhone('+14155551234')).toBe('***-***-1234');
    });

    test('short number', () => {
      expect(maskPhone('123')).toBe('123');
    });

    test('number with dashes', () => {
      expect(maskPhone('415-555-1234')).toBe('***-***-1234');
    });
  });

  describe('maskedView', () => {
    test('produces masked customer object', () => {
      const c: Customer = {
        id: 'c1',
        firstName: 'Alice',
        lastName: 'Smith',
        email: 'alice@example.com',
        phone: '+14155551234',
        createdAt: '2024-01-01',
        updatedAt: '2024-01-01',
      };
      const masked = maskedView(c);
      expect(masked.email).toBe('a***e@example.com');
      expect(masked.phone).toBe('***-***-1234');
      expect(masked.firstName).toBe('Alice');
    });

    test('handles missing phone', () => {
      const c: Customer = {
        id: 'c1', firstName: 'Bob', lastName: 'J', email: 'b@x.com',
        createdAt: '2024-01-01', updatedAt: '2024-01-01',
      };
      const masked = maskedView(c);
      expect(masked.phone).toBeUndefined();
    });
  });

  describe('initials', () => {
    test('normal names', () => {
      const c = { id: 'c1', firstName: 'Alice', lastName: 'Smith', email: 'a@b.com', createdAt: '', updatedAt: '' };
      expect(initials(c)).toBe('AS');
    });

    test('missing names', () => {
      const c = { id: 'c1', firstName: '', lastName: '', email: 'a@b.com', createdAt: '', updatedAt: '' };
      expect(initials(c)).toBe('');
    });
  });
});
