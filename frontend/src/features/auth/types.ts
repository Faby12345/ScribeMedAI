export type LoginRequest = {
  email: string;
  password: string;
};

export type AuthenticatedUser = {
  id: string;
  tenantId: string;
  email: string;
  displayName: string;
  role: "DOCTOR" | "TENANT_ADMIN";
};

export type LoginResponse = {
  user: AuthenticatedUser;
};
