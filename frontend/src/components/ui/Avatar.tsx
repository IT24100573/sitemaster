import { useState } from 'react';
import { profileApi } from '../../api/profileApi';
import { initials } from '../../lib/format';

interface AvatarProps {
  userId: number;
  name: string;
  hasPhoto: boolean;
  /** Changes when the photo changes, to bypass the browser cache. */
  version?: number | string;
  size?: 'sm' | 'md' | 'lg';
}

const SIZES = { sm: 'h-9 w-9 text-sm', md: 'h-12 w-12 text-base', lg: 'h-24 w-24 text-2xl' };

/** Profile photo (served by the authorised API) with an initials fallback. */
export function Avatar({ userId, name, hasPhoto, version = 0, size = 'sm' }: AvatarProps) {
  const [failed, setFailed] = useState(false);
  const showPhoto = hasPhoto && !failed;

  return showPhoto ? (
    <img
      src={profileApi.photoUrl(userId, version)}
      alt={name}
      onError={() => setFailed(true)}
      className={`${SIZES[size]} shrink-0 rounded-full object-cover`}
    />
  ) : (
    <span
      className={`${SIZES[size]} flex shrink-0 items-center justify-center rounded-full bg-primary-light font-semibold text-primary-dark`}
      aria-hidden
    >
      {initials(name)}
    </span>
  );
}
