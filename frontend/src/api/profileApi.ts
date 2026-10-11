import { api } from './client';
import type { MessageResponse } from '../types/api';
import type { ChangePasswordPayload, Profile, ProfilePayload } from '../types/profile';

export const profileApi = {
  get: async () => (await api.get<Profile>('/profile')).data,
  update: async (payload: ProfilePayload) => (await api.put<Profile>('/profile', payload)).data,
  uploadPhoto: async (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return (await api.post<Profile>('/profile/photo', form)).data;
  },
  removePhoto: async () => (await api.delete<Profile>('/profile/photo')).data,
  changePassword: async (payload: ChangePasswordPayload) =>
    (await api.put<MessageResponse>('/profile/password', payload)).data,
  photoUrl: (userId: number, version: number | string) => `/api/users/${userId}/photo?v=${version}`,
};
