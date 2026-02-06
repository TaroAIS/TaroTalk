import { useRecoilState } from "recoil";
import { userState, UserProfile } from "../state/user";

export function useSessionUser() {
  const [user, setUser] = useRecoilState(userState);

  const updateUser = (patch: Partial<UserProfile>) => {
    setUser((prev) => ({ ...(prev || { userId: "", nickname: "" }), ...patch }));
  };

  const clearUser = () => setUser(null);

  return { user, setUser, updateUser, clearUser };
}
