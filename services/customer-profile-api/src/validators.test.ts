import { isEmail, isPhone, requireString, requireBoolean, ValidationError } from './validators';

describe('validators', () => {
  describe('isEmail', () => {
    test('valid emails', () => {
      expect(isEmail('user@example.com')).toBe(true);
      expect(isEmail('a@b.co')).toBe(true);
      expect(isEmail('user+tag@domain.org')).toBe(true);
    });

    test('invalid emails', () => {
      expect(isEmail('not-an-email')).toBe(false);
      expect(isEmail('@missing.com')).toBe(false);
      expect(isEmail('user@')).toBe(false);
      expect(isEmail('')).toBe(false);
    });

    test('non-string inputs', () => {
      expect(isEmail(null)).toBe(false);
      expect(isEmail(undefined)).toBe(false);
      expect(isEmail(123)).toBe(false);
    });
  });

  describe('isPhone', () => {
    test('valid E.164 numbers', () => {
      expect(isPhone('+14155551234')).toBe(true);
      expect(isPhone('14155551234')).toBe(true);
      expect(isPhone('+442071234567')).toBe(true);
    });

    test('invalid numbers', () => {
      expect(isPhone('abc')).toBe(false);
      expect(isPhone('+0123')).toBe(false);
      expect(isPhone('')).toBe(false);
    });

    test('non-string inputs', () => {
      expect(isPhone(null)).toBe(false);
      expect(isPhone(undefined)).toBe(false);
    });
  });

  describe('requireString', () => {
    test('valid string', () => {
      expect(requireString('hello', 'field')).toBe('hello');
    });

    test('empty string throws', () => {
      expect(() => requireString('', 'field')).toThrow(ValidationError);
    });

    test('non-string throws', () => {
      expect(() => requireString(123 as any, 'field')).toThrow(ValidationError);
      expect(() => requireString(null as any, 'field')).toThrow(ValidationError);
      expect(() => requireString(undefined as any, 'field')).toThrow(ValidationError);
    });
  });

  describe('requireBoolean', () => {
    test('true and false pass', () => {
      expect(requireBoolean(true, 'field')).toBe(true);
      expect(requireBoolean(false, 'field')).toBe(false);
    });

    test('non-boolean throws', () => {
      expect(() => requireBoolean('true' as any, 'field')).toThrow(ValidationError);
      expect(() => requireBoolean(1 as any, 'field')).toThrow(ValidationError);
      expect(() => requireBoolean(null as any, 'field')).toThrow(ValidationError);
    });
  });

  describe('ValidationError', () => {
    test('has correct name', () => {
      const err = new ValidationError('test');
      expect(err.name).toBe('ValidationError');
      expect(err.message).toBe('test');
      expect(err instanceof Error).toBe(true);
    });
  });
});
