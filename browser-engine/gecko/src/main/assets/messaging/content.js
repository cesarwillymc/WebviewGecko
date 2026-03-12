// Bridges page <-> Android native.
// Page->Android: window.postMessage({ type: "FROM_PAGE", text: "..." }) -> sendNativeMessage.
// Android->page: port.postMessage from native -> we forward via window.postMessage(FROM_ANDROID).

let port = null;

function injectScriptIntoPage(script) {
  const androidPolyfill = `
    if (typeof window.Android === 'undefined') {
      window.Android = {
        onError: function(m) { window.postMessage({ type: "FROM_PAGE", method: "onError", message: m }, "*"); },
        onReadJson: function(m) { window.postMessage({ type: "FROM_PAGE", method: "onReadJson", message: m }, "*"); }
      };
    }
  `;
  const el = document.createElement("script");
  el.textContent = androidPolyfill + "\n" + script;
  (document.head || document.documentElement).appendChild(el);
  el.remove();
}

try {
  port = browser.runtime.connectNative("browser");
  port.onMessage.addListener((msg) => {
    if (msg && msg.action === "evaluate" && typeof msg.script === "string") {
      injectScriptIntoPage(msg.script);
      return;
    }
    if (msg && msg.action === "postMessage" && msg.channel != null) {
      window.postMessage({ type: "FROM_ANDROID", channel: msg.channel, data: msg.data ?? msg.text ?? "" }, "*");
      return;
    }
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
