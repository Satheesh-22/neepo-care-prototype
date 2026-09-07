import { liveApi } from "./liveApi";
import { mockApi } from "./mockApi";

export const apiMode = import.meta.env.VITE_API_MODE ?? "mock";
export const neepoApi = apiMode === "live" ? liveApi : mockApi;
export const api = neepoApi;
