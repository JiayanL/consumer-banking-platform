import { redactPII } from './redact';

export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

const LEVEL_ORDER: Record<LogLevel, number> = {
  debug: 10,
  info: 20,
  warn: 30,
  error: 40,
};

export interface Logger {
  debug(msg: string, fields?: Record<string, unknown>): void;
  info(msg: string, fields?: Record<string, unknown>): void;
  warn(msg: string, fields?: Record<string, unknown>): void;
  error(msg: string, fields?: Record<string, unknown>): void;
  child(bindings: Record<string, unknown>): Logger;
}

export interface LoggerOptions {
  service: string;
  level?: LogLevel;
  /** Writer receives a single JSON-encoded line (no trailing newline). */
  out?: (line: string) => void;
  /** Additional fields included on every log line. */
  bindings?: Record<string, unknown>;
  /** If true, strings that look like SSNs/PANs are redacted. Default true. */
  redact?: boolean;
}

class JsonLogger implements Logger {
  constructor(private readonly opts: Required<LoggerOptions>) {}

  debug(msg: string, fields?: Record<string, unknown>) { this.emit('debug', msg, fields); }
  info(msg: string, fields?: Record<string, unknown>)  { this.emit('info',  msg, fields); }
  warn(msg: string, fields?: Record<string, unknown>)  { this.emit('warn',  msg, fields); }
  error(msg: string, fields?: Record<string, unknown>) { this.emit('error', msg, fields); }

  child(bindings: Record<string, unknown>): Logger {
    return new JsonLogger({
      ...this.opts,
      bindings: { ...this.opts.bindings, ...bindings },
    });
  }

  private emit(level: LogLevel, msg: string, fields?: Record<string, unknown>) {
    if (LEVEL_ORDER[level] < LEVEL_ORDER[this.opts.level]) return;
    const record: Record<string, unknown> = {
      ts: new Date().toISOString(),
      level,
      service: this.opts.service,
      msg,
      ...this.opts.bindings,
      ...(fields ?? {}),
    };
    const payload = this.opts.redact ? redactPII(record) : record;
    this.opts.out(JSON.stringify(payload));
  }
}

export function createLogger(opts: LoggerOptions): Logger {
  return new JsonLogger({
    service: opts.service,
    level: opts.level ?? 'info',
    out: opts.out ?? ((line) => process.stdout.write(line + '\n')),
    bindings: opts.bindings ?? {},
    redact: opts.redact ?? true,
  });
}
