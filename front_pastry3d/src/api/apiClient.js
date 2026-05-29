const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

const TOKEN_KEY = "pastry3d_token";
const USER_KEY = "pastry3d_user";
const DEFAULT_TIMEOUT_MS = 120000;

function normalizeBaseUrl(value) {
  if (!value) return "";
  return value.endsWith("/") ? value.slice(0, -1) : value;
}

function normalizePath(path) {
  if (!path) return "";
  return path.startsWith("/") ? path : `/${path}`;
}

function getApiOrigin() {
  if (!API_BASE_URL) return "";

  if (API_BASE_URL.startsWith("http://") || API_BASE_URL.startsWith("https://")) {
    try {
      return new URL(API_BASE_URL).origin;
    } catch {
      return "";
    }
  }

  return "";
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (!token) {
    localStorage.removeItem(TOKEN_KEY);
    return;
  }

  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export function getStoredUser() {
  const raw = localStorage.getItem(USER_KEY);

  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch {
    localStorage.removeItem(USER_KEY);
    return null;
  }
}

export function setStoredUser(user) {
  if (!user) {
    localStorage.removeItem(USER_KEY);
    return;
  }

  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearStoredUser() {
  localStorage.removeItem(USER_KEY);
}

export function clearSession() {
  clearToken();
  clearStoredUser();
}

export function getAssetUrl(assetUrl) {
  if (!assetUrl) return "";

  if (
    assetUrl.startsWith("http://") ||
    assetUrl.startsWith("https://") ||
    assetUrl.startsWith("data:") ||
    assetUrl.startsWith("blob:")
  ) {
    return assetUrl;
  }

  const normalized = normalizePath(assetUrl);

  if (normalized.startsWith("/uploads/")) {
    const origin = getApiOrigin();
    return origin ? `${origin}${normalized}` : normalized;
  }

  return normalized;
}

async function parseResponse(response) {
  const contentType = response.headers.get("content-type") || "";

  if (response.status === 204) {
    return null;
  }

  if (contentType.includes("application/json")) {
    return response.json();
  }

  const text = await response.text();

  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function resolveErrorMessage(payload, fallback) {
  if (!payload) {
    return fallback;
  }

  if (typeof payload === "string") {
    return payload;
  }

  if (payload.fields && typeof payload.fields === "object") {
    const fieldMessages = Object.entries(payload.fields)
      .map(([field, message]) => `${field}: ${message}`)
      .join(". ");

    if (fieldMessages) {
      return fieldMessages;
    }
  }

  return (
    payload.message ||
    payload.error ||
    payload.detail ||
    payload.title ||
    fallback
  );
}

export async function request(path, options = {}) {
  const baseUrl = normalizeBaseUrl(API_BASE_URL);
  const url = `${baseUrl}${normalizePath(path)}`;
  const token = getToken();
  const controller = new AbortController();
  const timeoutMs = options.timeoutMs || DEFAULT_TIMEOUT_MS;
  const timeoutId = window.setTimeout(() => controller.abort(), timeoutMs);

  const headers = {
    ...(options.headers || {}),
  };

  if (!(options.body instanceof FormData)) {
    headers["Content-Type"] = headers["Content-Type"] || "application/json";
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const fetchOptions = {
    ...options,
    headers,
    signal: options.signal || controller.signal,
  };

  delete fetchOptions.timeoutMs;

  if (
    fetchOptions.body &&
    !(fetchOptions.body instanceof FormData) &&
    typeof fetchOptions.body !== "string"
  ) {
    fetchOptions.body = JSON.stringify(fetchOptions.body);
  }

  try {
    const response = await fetch(url, fetchOptions);
    const payload = await parseResponse(response);

    if (!response.ok) {
      if (response.status === 401 && !path.includes("/auth/login")) {
        clearSession();
      }

      const message = resolveErrorMessage(payload, `Error HTTP ${response.status}`);
      throw new Error(message);
    }

    return payload;
  } catch (err) {
    if (err.name === "AbortError") {
      throw new Error("La solicitud tardó demasiado. Intenta nuevamente.", { cause: err });
    }

    throw err;
  } finally {
    window.clearTimeout(timeoutId);
  }
}

export const apiClient = {
  TOKEN_KEY,
  USER_KEY,

  getToken,
  setToken,
  clearToken,
  getAssetUrl,

  getStoredUser,
  setStoredUser,
  clearStoredUser,
  clearSession,

  async register(payload) {
    return request("/auth/register", {
      method: "POST",
      body: payload,
    });
  },

  async login(payload) {
    const response = await request("/auth/login", {
      method: "POST",
      body: payload,
    });

    if (response?.token) {
      setToken(response.token);
    }

    return response;
  },

  async confirmEmail(token) {
    return request(`/auth/confirm?token=${encodeURIComponent(token)}`, {
      method: "GET",
    });
  },

  async me() {
    return request("/auth/me", {
      method: "GET",
    });
  },

  async getMe() {
    return this.me();
  },

  async logout() {
    clearSession();
    return true;
  },

  async generateRecipe(prompt) {
    return request("/recipes/generate", {
      method: "POST",
      body: {
        prompt,
      },
    });
  },

  async listRecipes() {
    return request("/recipes", {
      method: "GET",
    });
  },

  async getRecipes() {
    return this.listRecipes();
  },

  async getRecipe(recipeId) {
    return request(`/recipes/${recipeId}`, {
      method: "GET",
    });
  },

  async getRecipeDetail(recipeId) {
    return this.getRecipe(recipeId);
  },

  async rebuildRecipeComposition(recipeId) {
    return request(`/recipes/${recipeId}/build-composition`, {
      method: "POST",
    });
  },

  async rebuildComposition(recipeId) {
    return this.rebuildRecipeComposition(recipeId);
  },

  async deleteRecipe(recipeId) {
    return request(`/recipes/${recipeId}`, {
      method: "DELETE",
    });
  },


  async getActiveTripoJob(recipeId) {
    return request(`/generation-jobs/recipes/${recipeId}/active-tripo`, {
      method: "GET",
    });
  },

  async startTripoGeneration(recipeId) {
    return request(`/generation-jobs/recipes/${recipeId}/start-tripo`, {
      method: "POST",
      timeoutMs: 180000,
    });
  },

  async startFairStackGeneration(recipeId) {
    return request(`/generation-jobs/recipes/${recipeId}/start-fairstack`, {
      method: "POST",
      timeoutMs: 180000,
    });
  },

  async getGenerationJob(jobId) {
    return request(`/generation-jobs/${jobId}`, {
      method: "GET",
    });
  },

  async syncGenerationJob(jobId) {
    return request(`/generation-jobs/${jobId}/sync`, {
      method: "POST",
    });
  },

  async listModels() {
    return request("/models", {
      method: "GET",
    });
  },

  async getModel(modelId) {
    return request(`/models/${modelId}`, {
      method: "GET",
    });
  },

  async addFavorite(recipeId) {
    return request("/favorites", {
      method: "POST",
      body: {
        recipeId,
      },
    });
  },

  async listFavorites() {
    return request("/favorites/me", {
      method: "GET",
    });
  },

  async removeFavorite(favoriteId) {
    return request(`/favorites/${favoriteId}`, {
      method: "DELETE",
    });
  },
};