export function payloadAttr(payload) {
  return JSON.stringify(payload).replace(/"/g, "&quot;");
}

export function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

export function formatDate(value) {
  if (!value) {
    return "";
  }
  try {
    return new Date(value).toLocaleDateString();
  } catch {
    return "";
  }
}
