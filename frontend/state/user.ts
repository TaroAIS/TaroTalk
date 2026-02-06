import { atom } from "recoil";

export interface UserProfile {
  userId: string;
  nickname: string;
  avatarUrl?: string;
  personaSummary?: string;
}

export const userState = atom<UserProfile | null>({
  key: "userState",
  default: null
});
