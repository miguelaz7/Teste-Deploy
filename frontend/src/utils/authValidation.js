import { EMPTY_ERRORS } from "../constants/auth";

export function validateAuthFields({ mode, firstName, lastName, email, password, confirmPassword }) {
  const nextErrors = { ...EMPTY_ERRORS };

  if (!email.trim()) {
    nextErrors.email = true;
  }

  if (!password) {
    nextErrors.password = true;
  }

  if (mode === "register") {
    if (!firstName.trim()) {
      nextErrors.firstName = true;
    }

    if (!lastName.trim()) {
      nextErrors.lastName = true;
    }

    if (password.length < 6) {
      nextErrors.password = true;
    }

    if (!confirmPassword || password !== confirmPassword) {
      nextErrors.confirmPassword = true;
    }
  }

  if (Object.values(nextErrors).some(Boolean)) {
    return { hasErrors: true, nextErrors, message: "Preenche os campos assinalados." };
  }

  return { hasErrors: false, nextErrors: { ...EMPTY_ERRORS }, message: "" };
}
