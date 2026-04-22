import { Template } from './templates';

export type Channel = 'email' | 'sms';

export interface NotificationRecord {
  id: string;
  channel: Channel;
  to: string;
  subject?: string;
  body: string;
  templateId?: string;
  createdAt: string;
}

export class NotificationStore {
  private readonly map = new Map<string, NotificationRecord>();
  private seq = 0;

  create(input: Omit<NotificationRecord, 'id' | 'createdAt'>): NotificationRecord {
    const id = `ntf_${++this.seq}`;
    const record: NotificationRecord = {
      id,
      createdAt: new Date().toISOString(),
      ...input,
    };
    this.map.set(id, record);
    return record;
  }

  get(id: string): NotificationRecord | undefined {
    return this.map.get(id);
  }

  list(): NotificationRecord[] {
    return Array.from(this.map.values());
  }
}

export class TemplateStore {
  private readonly map = new Map<string, Template>();

  upsert(t: Template): Template {
    this.map.set(t.id, t);
    return t;
  }

  get(id: string): Template | undefined {
    return this.map.get(id);
  }

  list(): Template[] {
    return Array.from(this.map.values());
  }

  has(id: string): boolean {
    return this.map.has(id);
  }
}
