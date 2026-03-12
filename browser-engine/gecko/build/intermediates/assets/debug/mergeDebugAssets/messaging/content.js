// Bridges page <-> Android native.
// Page->Android: window.postMessage({ type: "FROM_PAGE", text: "..." }) -> sendNativeMessage.
// Android->page: port.postMessage from native -> we forward via window.postMessage(FROM_ANDROID).

let port = null;

try {
  port = browser.runtime.connectNative("browser");
  port.onMessage.addListener((msg) => {
    const text = (msg && (msg.text || msg.data || msg.reply)) || JSON.stringify(msg);
    window.postMessage({ type: "FROM_ANDROID", text }, "*");
  });
} catch (e) {
  console.warn("MessagingBridge: connectNative failed", e);
}

// Page sends: { type: "FROM_PAGE", method: "onError"|"onReadJson", message: "<json>" }
window.addEventListener("message", async (event) => {
  if (!event.data || event.data.type !== "FROM_PAGE") return;

  const method = event.data.method || "onReadJson";
  const message = event.data.message ?? event.data.text ?? event.data.data ?? "";

  try {
    const response = await browser.runtime.sendNativeMessage("browser", {
      method,
      text: message
    });

    window.postMessage(
      {
        type: "FROM_ANDROID",
        text: response ? (response.reply ?? response.text ?? JSON.stringify(response)) : ""
      },
      "*"
    );
  } catch (err) {
    window.postMessage(
      {
        type: "FROM_ANDROID",
        error: String(err)
      },
      "*"
    );
  }
});
