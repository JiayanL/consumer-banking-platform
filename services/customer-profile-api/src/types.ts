export interface Customer {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CommunicationPreferences {
  customerId: string;
  emailOptIn: boolean;
  smsOptIn: boolean;
  marketingOptIn: boolean;
  locale: string;
  updatedAt: string;
}

export type CustomerPatch = Partial<Pick<Customer, 'firstName' | 'lastName' | 'email' | 'phone'>>;
