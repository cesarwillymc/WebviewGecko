// Bridges page <-> Android native.
//
// Page→Android: window.postMessage({ type: "FROM_PAGE", method: "onReadJson"|"onError", message: "..." })
//               Uses sendNativeMessage (one-shot, reliable, no port needed)
//
// Android→Page: port.postMessage from native → forwarded via window.postMessage({ type: "FROM_ANDROID" })
//               Uses connectNative (persistent port) with retry/backoff on failure

// ─── Script injection (called by Android via port evaluate action) ────────────

function injectScriptIntoPage(script) {
  const blob = new Blob([script], { type: "application/javascript" });
  const url  = URL.createObjectURL(blob);
  const el   = document.createElement("script");
  el.src     = url;

  el.onload  = () => { URL.revokeObjectURL(url); el.remove(); };
  el.onerror = (e) => {
    URL.revokeObjectURL(url);
    el.remove();
    console.error("MessagingBridge: blob script failed to load", e);
    window.postMessage({ type: "FROM_ANDROID_ERROR",
                         error: "Script injection failed" }, "*");
  };

  (document.head || document.documentElement).appendChild(el);
  // ← no el.remove() here
}

// ─── Port (Android→Page channel) ─────────────────────────────────────────────

let port = null;
let portRetryCount = 0;
const MAX_RETRIES = 5;

function connectPort() {
  try {
    port = browser.runtime.connectNative("browser");

    port.onMessage.addListener((msg) => {
      if (!msg) return;

      // Android wants to inject + run a script in the page
      if (msg.action === "evaluate" && typeof msg.script === "string") {
        injectScriptIntoPage(msg.script);
        return;
      }

      // Android wants to post a channel message to the page
      if (msg.action === "postMessage" && msg.channel != null) {
        window.postMessage({
          type: "FROM_ANDROID",
          channel: msg.channel,
          data: msg.data ?? msg.text ?? ""
        }, "*");
        return;
      }

      // Generic message fallback
      const text = msg.text || msg.data || msg.reply || JSON.stringify(msg);
      window.postMessage({ type: "FROM_ANDROID", text }, "*");
    });

    port.onDisconnect.addListener((p) => {
      const err = p.error?.message ?? "unknown reason";
      console.warn("MessagingBridge: port disconnected —", err);
      port = null;

      if (portRetryCount < MAX_RETRIES) {
        portRetryCount++;
        const delay = Math.min(200 * portRetryCount, 2000);
        console.log(`MessagingBridge: retrying port in ${delay}ms (attempt ${portRetryCount}/${MAX_RETRIES})`);
        setTimeout(connectPort, delay);
      } else {
        console.error("MessagingBridge: gave up reconnecting after", MAX_RETRIES, "retries");
        window.postMessage({
          type: "FROM_ANDROID_ERROR",
          error: "Bridge port permanently unavailable: " + err
        }, "*");
      }
    });

    portRetryCount = 0;
    console.log("MessagingBridge: port connected");

  } catch (e) {
    console.warn("MessagingBridge: connectNative threw —", e);
    port = null;

    if (portRetryCount < MAX_RETRIES) {
      portRetryCount++;
      setTimeout(connectPort, 200 * portRetryCount);
    } else {
      console.error("MessagingBridge: connectNative failed permanently after", MAX_RETRIES, "retries");
      window.postMessage({
        type: "FROM_ANDROID_ERROR",
        error: "connectNative failed: " + String(e)
      }, "*");
    }
  }
}

// ─── Page→Android channel (sendNativeMessage, one-shot, no port needed) ──────

window.addEventListener("message", async (event) => {
  if (!event.data || event.data.type !== "FROM_PAGE") return;

  const method  = event.data.method  || "onReadJson";
  const message = event.data.message ?? event.data.text ?? event.data.data ?? "";

  try {
    const response = await browser.runtime.sendNativeMessage("browser", {
      method,
      text: message
    });

    // Only echo back if Android actually sent a non-empty reply.
    if (response && (response.reply || response.text)) {
      window.postMessage({
        type: "FROM_ANDROID",
        text: response.reply ?? response.text ?? JSON.stringify(response)
      }, "*");
    }

  } catch (err) {
    console.error("MessagingBridge: sendNativeMessage failed —", err);
    window.postMessage({
      type: "FROM_ANDROID_ERROR",
      error: String(err)
    }, "*");
  }
});

// ─── Boot ─────────────────────────────────────────────────────────────────────

connectPort(); // start persistent port for Android→Page push messages