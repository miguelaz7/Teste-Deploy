export function getGreeting() {
  const currentHour = new Date().getHours();

  if (currentHour < 12) {
    return "Bom dia";
  }

  if (currentHour < 19) {
    return "Boa tarde";
  }

  return "Boa noite";
}
