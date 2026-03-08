import { useState } from "react";
import { EMPTY_ERRORS } from "../constants/auth";
import { sendAuthRequest } from "../services/authService";
import { validateAuthFields } from "../utils/authValidation";

export default function useAuthFlow() {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [mode, setMode] = useState("login");
  const [loading, setLoading] = useState(false);
  const [mensagem, setMensagem] = useState("");
  const [fieldErrors, setFieldErrors] = useState(EMPTY_ERRORS);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [loggedInEmail, setLoggedInEmail] = useState("");
  const [loggedInFirstName, setLoggedInFirstName] = useState("");
  const [loggedInLastName, setLoggedInLastName] = useState("");

  const clearFieldError = (fieldName) => {
    setFieldErrors((prev) => ({ ...prev, [fieldName]: false }));
  };

  const markFieldError = (fieldName) => {
    setFieldErrors((prev) => ({ ...prev, [fieldName]: true }));
  };

  const resetSensitiveFields = () => {
    setPassword("");
    setConfirmPassword("");
    setFieldErrors(EMPTY_ERRORS);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const validation = validateAuthFields({
      mode,
      firstName,
      lastName,
      email,
      password,
      confirmPassword,
    });

    if (validation.hasErrors) {
      setFieldErrors(validation.nextErrors);
      setMensagem(validation.message);
      return;
    }

    setLoading(true);
    setMensagem("");
    setFieldErrors(EMPTY_ERRORS);

    try {
      const payload = {
        firstName,
        lastName,
        email,
        password,
        confirmPassword,
      };

      const { ok, data } = await sendAuthRequest(mode, payload);

      if (ok) {
        if (mode === "login") {
          setAuthenticated(true);
          setLoggedInEmail(email);
          setLoggedInFirstName((data.firstName || "").trim());
          setLoggedInLastName((data.lastName || "").trim());
          setMensagem("");
          resetSensitiveFields();
          return;
        }

        setMensagem(data.message || "Conta criada com sucesso. Faz login para entrar.");
        setMode("login");
        setFirstName("");
        setLastName("");
        setEmail("");
        resetSensitiveFields();
        return;
      }

      const errorMessage = data.message || "Nao foi possivel concluir o pedido.";
      setMensagem(errorMessage);
      applyBackendErrors(errorMessage, markFieldError, setFieldErrors);
    } catch (error) {
      setMensagem("Nao foi possivel ligar ao servidor.");
    } finally {
      setLoading(false);
    }
  };

  const handleToggleMode = () => {
    setMode((prevMode) => (prevMode === "login" ? "register" : "login"));
    setMensagem("");
    resetSensitiveFields();
  };

  const handleLogout = () => {
    setAuthenticated(false);
    setLoggedInEmail("");
    setLoggedInFirstName("");
    setLoggedInLastName("");
    setMensagem("");
  };

  return {
    authenticated,
    loggedInEmail,
    loggedInFirstName,
    loggedInLastName,
    handleLogout,
    authFormProps: {
      mode,
      loading,
      mensagem,
      fieldErrors,
      firstName,
      lastName,
      email,
      password,
      confirmPassword,
      showPassword,
      showConfirmPassword,
      onSubmit: handleSubmit,
      onSetFirstName: setFirstName,
      onSetLastName: setLastName,
      onSetEmail: setEmail,
      onSetPassword: setPassword,
      onSetConfirmPassword: setConfirmPassword,
      onTogglePassword: () => setShowPassword((prev) => !prev),
      onToggleConfirmPassword: () => setShowConfirmPassword((prev) => !prev),
      onToggleMode: handleToggleMode,
      clearFieldError,
      markFieldError,
    },
  };
}

function applyBackendErrors(errorMessage, markFieldError, setFieldErrors) {
  const lowerMessage = errorMessage.toLowerCase();

  if (lowerMessage.includes("credenciais invalidas")) {
    setFieldErrors((prev) => ({ ...prev, email: true, password: true }));
    return;
  }

  if (lowerMessage.includes("email")) {
    markFieldError("email");
    return;
  }

  if (lowerMessage.includes("passwords") || lowerMessage.includes("coincidem")) {
    markFieldError("confirmPassword");
    return;
  }

  if (lowerMessage.includes("password")) {
    markFieldError("password");
    return;
  }

  if (lowerMessage.includes("nome") || lowerMessage.includes("apelido")) {
    setFieldErrors((prev) => ({ ...prev, firstName: true, lastName: true }));
  }
}
