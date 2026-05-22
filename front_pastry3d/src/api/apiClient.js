const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
const TOKEN_KEY = "pastry3d_token";

function normalizeBaseUrl(url) {
  return String(url || "").replace(/\/+$/, "");
}

function getBackendOrigin() {
  const normalized = normalizeBaseUrl(API_BASE_URL);
  if (normalized.endsWith("/api")) {
    return normalized.slice(0, -4);
  }
  return normalized;
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (!token) return;
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export function getAssetUrl(modelUrl) {
  if (!modelUrl) return "";
  if (modelUrl.startsWith("http://") || modelUrl.startsWith("https://")) {
    return modelUrl;
  }
  if (modelUrl.startsWith("/")) {
    return `${getBackendOrigin()}${modelUrl}`;
  }
  return `${getBackendOrigin()}/${modelUrl}`;
}

function buildHeaders(options = {}) {
  const headers = {
    ...(options.headers || {}),
  };

  if (!options.isFormData) {
    headers["Content-Type"] = "application/json";
  }

  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
}

async function parseResponse(response) {
  const contentType = response.headers.get("content-type") || "";

  if (response.status === 204) {
    return null;
  }

  let payload;
  if (contentType.includes("application/json")) {
    payload = await response.json();
  } else {
    payload = await response.text();
  }

  if (!response.ok) {
    const message =
      typeof payload === "object" && payload !== null
        ? payload.message || payload.error || "Error inesperado del servidor"
        : payload || "Error inesperado del servidor";

    const error = new Error(message);
    error.status = response.status;
    error.payload = payload;
    throw error;
  }

  return payload;
}

async function request(path, options = {}) {
  const url = `${normalizeBaseUrl(API_BASE_URL)}${path.startsWith("/") ? path : `/${path}`}`;

  const response = await fetch(url, {
    method: options.method || "GET",
    headers: buildHeaders(options),
    body: options.body,
  });

  return parseResponse(response);
}

export const apiClient = {
  async register({ email, password, displayName }) {
    return request("/auth/register", {
      method: "POST",
      body: JSON.stringify({ email, password, displayName }),
    });
  },

  async login({ email, password }) {
    return request("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
  },

  async me() {
    return request("/auth/me");
  },

  async generateRecipe(prompt) {
    return request("/recipes/generate", {
      method: "POST",
      body: JSON.stringify({ prompt }),
    });
  },

  async listRecipes() {
    return request("/recipes");
  },

  async getRecipe(id) {
    return request(`/recipes/${id}`);
  },

  async deleteRecipe(id) {
    return request(`/recipes/${id}`, {
      method: "DELETE",
    });
  },

  async rebuildComposition(id) {
    return request(`/recipes/${id}/build-composition`, {
      method: "POST",
    });
  },

  async listModels(params = {}) {
    const query = new URLSearchParams();
    if (params.category) query.set("category", params.category);
    if (params.dessertType) query.set("dessertType", params.dessertType);
    const suffix = query.toString() ? `?${query.toString()}` : "";
    return request(`/models${suffix}`);
  },

  async addFavorite({ targetType, targetId }) {
    return request("/favorites", {
      method: "POST",
      body: JSON.stringify({ targetType, targetId }),
    });
  },

  async listFavorites() {
    return request("/favorites/me");
  },

  async removeFavorite(id) {
    return request(`/favorites/${id}`, {
      method: "DELETE",
    });
  },
};