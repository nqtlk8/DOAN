import { components } from '@erp/api-contract/src/generated/api';

export type CustomerCreateDto = components['schemas']['CustomerCreateDto'];
export type CustomerResponseDto = components['schemas']['CustomerCreateDto']; // The backend might use same or Customer for response, I'll use the API type if exists. 
