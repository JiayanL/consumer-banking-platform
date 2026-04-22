import { createLogger } from './logger';
import { redactPII } from './redact';

describe('createLogger', () => {
  function capture() {
    const lines: string[] = [];
    const logger = createLogger({
      service: 'test-svc',
      out: (l) => lines.push(l),
      level: 'debug',
    });
    return { logger, lines };
  }

  test('emits JSON lines with level, msg, service', () => {
    const { logger, lines } = capture();
    logger.info('hello', { userId: 'u1' });
    expect(lines).toHaveLength(1);
    const parsed = JSON.parse(lines[0]);
    expect(parsed.level).toBe('info');
    expect(parsed.msg).toBe('hello');
    expect(parsed.service).toBe('test-svc');
    expect(parsed.userId).toBe('u1');
  });

  test('child logger inherits bindings', () => {
    const { logger, lines } = capture();
    const child = logger.child({ requestId: 'r1' });
    child.warn('oops');
    expect(JSON.parse(lines[0]).requestId).toBe('r1');
  });

  test('level filtering works', () => {
    const lines: string[] = [];
    const logger = createLogger({ service: 's', out: (l) => lines.push(l), level: 'warn' });
    logger.debug('d');
    logger.info('i');
    logger.warn('w');
    logger.error('e');
    expect(lines).toHaveLength(2);
  });

  test('redacts SSN-shaped strings by default', () => {
    const { logger, lines } = capture();
    logger.info('saw ssn', { raw: '123-45-6789' });
    expect(JSON.parse(lines[0]).raw).toBe('***-**-****');
  });
});

describe('redactPII', () => {
  test('masks PAN to last 4', () => {
    expect(redactPII('card 4111 1111 1111 1234')).toContain('1234');
    expect(redactPII('card 4111 1111 1111 1234')).not.toContain('4111');
  });

  test('walks nested objects and arrays', () => {
    const out = redactPII({ a: ['ssn 123-45-6789'], b: { c: 'fine' } });
    expect(out.a[0]).toBe('ssn ***-**-****');
    expect(out.b.c).toBe('fine');
  });

  test('leaves non-matching strings alone', () => {
    expect(redactPII('nothing sensitive')).toBe('nothing sensitive');
  });
});
