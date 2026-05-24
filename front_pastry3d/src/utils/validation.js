export const FIELD_LIMITS = {
  DISPLAY_NAME_MIN: 2,
  DISPLAY_NAME_MAX: 60,
  EMAIL_MAX: 254,
  PASSWORD_MIN: 8,
  PASSWORD_MAX: 128,
  PROMPT_MIN: 5,
  PROMPT_MAX: 1000,
  HISTORY_PROMPT_PREVIEW: 180,
};

export function clampText(value, maxLength) {
  return String(value ?? "").slice(0, maxLength);
}

export function normalizeEmail(value) {
  return clampText(value, FIELD_LIMITS.EMAIL_MAX).trim().toLowerCase();
}

export function getTextLength(value) {
  return Array.from(String(value ?? "")).length;
}

export function truncateText(value, maxLength = 160) {
  const text = String(value ?? "").trim();

  if (getTextLength(text) <= maxLength) {
    return text;
  }

  return `${Array.from(text).slice(0, maxLength - 1).join("")}…`;
}

export function validateEmail(value) {
  const email = normalizeEmail(value);

  if (!email) {
    return "El correo es obligatorio";
  }

  if (getTextLength(email) > FIELD_LIMITS.EMAIL_MAX) {
    return `El correo no puede superar ${FIELD_LIMITS.EMAIL_MAX} caracteres`;
  }

  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
  if (!emailPattern.test(email)) {
    return "Ingresa un correo válido";
  }

  return "";
}

export function validateDisplayName(value) {
  const name = String(value ?? "").trim();
  const length = getTextLength(name);

  if (!name) {
    return "El nombre visible es obligatorio";
  }

  if (length < FIELD_LIMITS.DISPLAY_NAME_MIN) {
    return `El nombre debe tener al menos ${FIELD_LIMITS.DISPLAY_NAME_MIN} caracteres`;
  }

  if (length > FIELD_LIMITS.DISPLAY_NAME_MAX) {
    return `El nombre no puede superar ${FIELD_LIMITS.DISPLAY_NAME_MAX} caracteres`;
  }

  return "";
}

export function validatePassword(value) {
  const password = String(value ?? "");
  const length = getTextLength(password);

  if (!password) {
    return "La contraseña es obligatoria";
  }

  if (length < FIELD_LIMITS.PASSWORD_MIN) {
    return `La contraseña debe tener mínimo ${FIELD_LIMITS.PASSWORD_MIN} caracteres`;
  }

  if (length > FIELD_LIMITS.PASSWORD_MAX) {
    return `La contraseña no puede superar ${FIELD_LIMITS.PASSWORD_MAX} caracteres`;
  }

  if (!/[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]/.test(password) || !/\d/.test(password)) {
    return "Usa al menos una letra y un número";
  }

  return "";
}

export function validatePrompt(value) {
  const prompt = String(value ?? "").trim();
  const length = getTextLength(prompt);

  if (!prompt) {
    return "Describe el postre que quieres generar";
  }

  if (length < FIELD_LIMITS.PROMPT_MIN) {
    return `La descripción debe tener al menos ${FIELD_LIMITS.PROMPT_MIN} caracteres`;
  }

  if (length > FIELD_LIMITS.PROMPT_MAX) {
    return `La descripción no puede superar ${FIELD_LIMITS.PROMPT_MAX} caracteres`;
  }

  return "";
}

export function passwordChecks(value) {
  const password = String(value ?? "");
  const length = getTextLength(password);

  return [
    {
      label: `${FIELD_LIMITS.PASSWORD_MIN} a ${FIELD_LIMITS.PASSWORD_MAX} caracteres`,
      valid: length >= FIELD_LIMITS.PASSWORD_MIN && length <= FIELD_LIMITS.PASSWORD_MAX,
    },
    {
      label: "Incluye al menos una letra",
      valid: /[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]/.test(password),
    },
    {
      label: "Incluye al menos un número",
      valid: /\d/.test(password),
    },
  ];
}
