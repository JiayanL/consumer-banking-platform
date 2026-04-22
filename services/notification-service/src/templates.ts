export interface Template {
  id: string;
  subject?: string;
  body: string;
}

const VAR_RE = /\{\{\s*([a-zA-Z_][a-zA-Z0-9_]*)\s*\}\}/g;

/**
 * Render a template by interpolating `{{ name }}` tokens against the
 * provided variable map. Missing variables are rendered as an empty
 * string. This function may end up with customer PII (email, name) in
 * its output which is then logged downstream, so it is marked as
 * compliance-critical.
 *
 * @compliance-critical PII_HANDLING
 */
export function interpolate(template: string, vars: Record<string, unknown>): string {
  if (typeof template !== 'string') {
    throw new TypeError('template must be a string');
  }
  return template.replace(VAR_RE, (_match, name: string) => {
    const v = vars[name];
    if (v === undefined || v === null) return '';
    return String(v);
  });
}

export function renderTemplate(tmpl: Template, vars: Record<string, unknown>): { subject: string; body: string } {
  return {
    subject: tmpl.subject ? interpolate(tmpl.subject, vars) : '',
    body: interpolate(tmpl.body, vars),
  };
}
