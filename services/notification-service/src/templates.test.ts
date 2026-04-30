import { interpolate, renderTemplate } from './templates';

describe('interpolate', () => {
  test('substitutes named vars', () => {
    expect(interpolate('hello {{name}}', { name: 'Ada' })).toBe('hello Ada');
  });

  test('handles multiple vars with whitespace', () => {
    const out = interpolate('{{ greeting }}, {{ name }}!', { greeting: 'hi', name: 'Grace' });
    expect(out).toBe('hi, Grace!');
  });

  test('missing vars render empty string', () => {
    expect(interpolate('[{{missing}}]', {})).toBe('[]');
  });

  test('coerces non-string values', () => {
    expect(interpolate('n={{count}}', { count: 3 })).toBe('n=3');
  });

  test('throws TypeError for non-string template', () => {
    expect(() => interpolate(42 as any, {})).toThrow(TypeError);
  });

  test('null variable renders empty string', () => {
    expect(interpolate('hi {{name}}', { name: null })).toBe('hi ');
  });
});

describe('renderTemplate', () => {
  test('renders subject and body', () => {
    const r = renderTemplate(
      { id: 't1', subject: 'Hi {{name}}', body: 'Balance: {{bal}}' },
      { name: 'Alan', bal: 100 },
    );
    expect(r).toEqual({ subject: 'Hi Alan', body: 'Balance: 100' });
  });

  test('returns empty subject when template has no subject', () => {
    const r = renderTemplate(
      { id: 't2', body: 'hello {{name}}' },
      { name: 'Ada' },
    );
    expect(r).toEqual({ subject: '', body: 'hello Ada' });
  });
});
