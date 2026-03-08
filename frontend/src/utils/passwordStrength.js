export function getPasswordStrength(value) {
  if (!value) {
    return { label: "Fraca", color: "#b42318", progress: 20 };
  }

  const hasLower = /[a-z]/.test(value);
  const hasUpper = /[A-Z]/.test(value);
  const hasNumber = /\d/.test(value);

  if (value.length < 6) {
    return { label: "Fraca", color: "#b42318", progress: 33 };
  }

  if (hasLower && hasUpper && hasNumber) {
    return { label: "Forte", color: "#00b067", progress: 100 };
  }

  return { label: "Media", color: "#ff5e00", progress: 66 };
}
