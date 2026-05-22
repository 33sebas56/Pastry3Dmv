const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

const TOKEN_KEY = "pastry3d_token";
const USER_KEY = "pastry3d_user";

function normalizeBaseUrl(value) {
  if (!value) return "";
  return value.endsWith("/") ? value.slice(0, -1) : value;
}

function normalizePath(path) {
  if (!path) return "";
  return path.startsWith("/") ? path : `/${path}`;
}

function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function setStoredToken(token) {
  if (!token) {
    localStorage.removeItem(TOKEN_KEY);
    return;
  }

  localStorage.setItem(TOKEN_KEY, token);
}

function clearStoredToken() {
  localStorage.removeItem(TOKEN_KEY);
}

function getStoredUser() {
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

function setStoredUser(user) {
  if (!user) {
    localStorage.removeItem(USER_KEY);
    return;
  }

  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

function clearStoredUser() {
  localStorage.removeItem(USER_KEY);
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

  return (
    payload.message ||
    payload.error ||
    payload.detail ||
    payload.title ||
    fallback
  );
}

async function request(path, options = {}) {
  const baseUrl = normalizeBaseUrl(API_BASE_URL);
  const url = `${baseUrl}${normalizePath(path)}`;

  const token = getStoredToken();

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
  };

  if (
    fetchOptions.body &&
    !(fetchOptions.body instanceof FormData) &&
    typeof fetchOptions.body !== "string"
  ) {
    fetchOptions.body = JSON.stringify(fetchOptions.body);
  }

  const response = await fetch(url, fetchOptions);
  const payload = await parseResponse(response);

  if (!response.ok) {
    const message = resolveErrorMessage(payload, `Error HTTP ${response.status}`);
    throw new Error(message);
  }

  return payload;
}

export const apiClient = {
  TOKEN_KEY,
  USER_KEY,

  getToken() {
    return getStoredToken();
  },

  setToken(token) {
    setStoredToken(token);
  },

  clearToken() {
    clearStoredToken();
  },

  getStoredUser() {
    return getStoredUser();
  },

  setStoredUser(user) {
    setStoredUser(user);
  },

  clearStoredUser() {
    clearStoredUser();
  },

  clearSession() {
    clearStoredToken();
    clearStoredUser();
  },

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
      setStoredToken(response.token);
    }

    return response;
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
    this.clearSession();
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
    return request(`/recipes/${recipeId}/rebuild-composition`, {
      method: "POST",
    });
  },

  async deleteRecipe(recipeId) {
    return request(`/recipes/${recipeId}`, {
      method: "DELETE",
    });
  },

  async startFairStackGeneration(recipeId) {
    return request(`/generation-jobs/recipes/${recipeId}/start-fairstack`, {
      method: "POST",
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

export { request };