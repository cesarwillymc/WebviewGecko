package com.example.webviewgecko

/**
 * @author Cesar Canaza
 * @contact cesar@joinautopilot.com  
 * @company Autopilot
 * Created 3/12/26
 */
object Script {

    val ibkr ="""
        "use strict";
        var ExecutionPlatform;
        (function (ExecutionPlatform) {
            ExecutionPlatform[ExecutionPlatform["ios"] = 0] = "ios";
            ExecutionPlatform[ExecutionPlatform["android"] = 1] = "android";
        })(ExecutionPlatform || (ExecutionPlatform = {}));
        var WebLoginState;
        (function (WebLoginState) {
            WebLoginState["usernamePassword"] = "usernamePassword";
            WebLoginState["otpText"] = "otpText";
            WebLoginState["usernamePasswordError"] = "usernamePasswordError";
            WebLoginState["otpTextError"] = "otpTextError";
            WebLoginState["success"] = "success";
            WebLoginState["pushNotificationPrompt"] = "pushNotificationPrompt";
            WebLoginState["selectChallengeType"] = "selectChallengeType";
            WebLoginState["initialSetupIncomplete"] = "initialSetupIncomplete";
        })(WebLoginState || (WebLoginState = {}));
        let defaults = {
            executionPlatform: ExecutionPlatform.android,
            shouldAutoLogin: false,
        };
        let executionPlatform = defaults.executionPlatform;
        let shouldAutoLogin = defaults.shouldAutoLogin;
        // Private utility functions
        function sendMessageToHost(message, isError = false) {
            var _a;
            if (executionPlatform === ExecutionPlatform.ios) {
                (_a = window.webkit) === null || _a === void 0 ? void 0 : _a.messageHandlers.handler.postMessage(message);
            }
            else {
                isError ? Android.onError(message) : Android.onReadJson(message);
            }
        }
        function createState(type, props) {
            return { type: type.valueOf(), props };
        }
        function validateElementVisible(element) {
            return element && element.offsetWidth > 0 && element.offsetHeight > 0 ? element : null;
        }
        function validateElementEnabled(element) {
            if (!element)
                return null;
            // Form controls (button, input, etc.) have .disabled
            if ('disabled' in element && element.disabled)
                return null;
            // ARIA or class-based disabled (e.g. <a role="button" aria-disabled="true">)
            if (element.getAttribute('aria-disabled') === 'true')
                return null;
            if (element.classList.contains('disabled'))
                return null;
            return element;
        }
        function getTextExcludingLinks(element) {
            var _a;
            if (!element)
                return null;
            const clone = element.cloneNode(true);
            clone.querySelectorAll('a').forEach(a => a.remove());
            clone.querySelectorAll('button').forEach(btn => btn.remove());
            const text = clone.innerText.split('\n').map(line => line.trim()).join('\n').trim();
            return text.length > 0 ? text : (_a = element === null || element === void 0 ? void 0 : element.placeholder) !== null && _a !== void 0 ? _a : null;
        }
        function getElementCenter(element) {
            const rect = element.getBoundingClientRect();
            return { x: (rect.left + rect.right) / 2, y: (rect.top + rect.bottom) / 2 };
        }
        function createMouseEvent(type, x, y) {
            return new MouseEvent(type, {
                view: window,
                bubbles: true,
                cancelable: true,
                clientX: x,
                clientY: y,
                button: 0,
                buttons: 1,
                detail: 1,
                screenX: x - window.screenX,
                screenY: y - window.screenY
            });
        }
        function clickElement(element) {
            if (!element)
                return false;
            const { x, y } = getElementCenter(element);
            element.dispatchEvent(createMouseEvent("mousedown", x, y));
            setTimeout(() => {
                element.dispatchEvent(createMouseEvent("mouseup", x, y));
                element.click();
            }, 15);
            return true;
        }
        function updateInputField(field, value) {
            if (field) {
                field.value = value;
                field.dispatchEvent(new Event("input", { bubbles: true }));
            }
        }
        const observeDOM = (function () {
            const MutationObserver = window.MutationObserver || window.WebKitMutationObserver;
            return function (obj, callback) {
                if (!obj || obj.nodeType !== 1)
                    return;
                if (MutationObserver) {
                    const observer = new MutationObserver(callback);
                    observer.observe(obj, { childList: true, subtree: true, attributes: true });
                    return observer;
                }
                obj.addEventListener("DOMNodeInserted", callback, false);
                obj.addEventListener("DOMNodeRemoved", callback, false);
            };
        })();
        // Login script specific code
        let usernameField;
        let passwordField;
        let otpField;
        let loginButton;
        let usernamePasswordIndicator;
        let pushOptionIndicator;
        let challengeCodeIndicator;
        let successIndicator;
        let errorIndicator;
        let loadingIndicator;
        let otpChallengeIndicator;
        let showChallengeIndicator;
        let resendCodePushNotificationButton;
        let selectChallengeIndicator;
        let challengeOptions;
        let emailVerificationSuccessIndicator;
        let timeoutHandle = null;
        let fallbackTimerHandle = null;
        let initialSetupIncompleteIndicator;
        let initialSetupIncompleteEnabled = true;
        function setFields() {
            var _a, _b, _c, _d, _e, _f, _g, _h, _j, _k, _l;
            otpChallengeIndicator = (_b = (_a = validateElementVisible(document.querySelector("#xyz-field-silver-response"))) !== null && _a !== void 0 ? _a : validateElementVisible(document.querySelector(".xyzblock.xyzblock-manualibkey"))) !== null && _b !== void 0 ? _b : validateElementVisible(document.querySelector(".form-email"));
            loginButton = Array.from(document.querySelectorAll(".btn.btn-lg.btn-primary")).find((button) => validateElementVisible(button)) || Array.from(document.querySelectorAll(".btn.btn-primary")).find((button) => validateElementVisible(button));
            loadingIndicator = validateElementVisible(document.querySelector(".xyzblock.xyzblock-loading.position-absolute"));
            usernameField = validateElementVisible(document.querySelector("#xyz-field-username"));
            passwordField = validateElementVisible(document.querySelector("#xyz-field-password"));
            errorIndicator = (_e = (_d = (_c = validateElementVisible(document.querySelector(".xyzblock.xyzblock-error"))) !== null && _c !== void 0 ? _c : validateElementVisible(document.querySelector(".xyz-errormessage"))) !== null && _d !== void 0 ? _d : validateElementVisible(document.querySelector('.page-validate .v-email-failed'))) !== null && _e !== void 0 ? _e : validateElementVisible(document.querySelector('.invalid-feedback'));
            usernamePasswordIndicator = validateElementVisible(document.querySelector(".xyzblock.xyzblock-username"));
            challengeCodeIndicator = validateElementVisible(document.querySelector(".xyz-goldchallenge"));
            pushOptionIndicator = validateElementVisible(document.querySelector(".xyzblock.xyzblock-notification"));
            showChallengeIndicator = validateElementVisible(document.querySelector(".xyz-showchallenge"));
            successIndicator = validateElementVisible(document.querySelector(".portfolio-summary"));
            otpField = ((_g = (_f = validateElementVisible(document.querySelector("#xyz-field-silver-response"))) !== null && _f !== void 0 ? _f : validateElementVisible(document.querySelector("#xyz-field-gold-response"))) !== null && _g !== void 0 ? _g : validateElementVisible(document.querySelector(".form-control.email-code")));
            resendCodePushNotificationButton = (_h = validateElementVisible(document.querySelector(".xyz-resendnotification"))) !== null && _h !== void 0 ? _h : validateElementEnabled(validateElementVisible(document.querySelector(".xyz-resend-sms-button")));
            selectChallengeIndicator = validateElementVisible(document.querySelector(".form-control.xyz-multipleselect"));
            challengeOptions = document.querySelectorAll(".form-control.xyz-multipleselect option");
            emailVerificationSuccessIndicator = validateElementVisible(document.querySelector('.page-validate .v-email-success'));
            initialSetupIncompleteIndicator = (_l = (_k = (_j = validateElementVisible(document.querySelector('#reg-content'))) !== null && _j !== void 0 ? _j : validateElementVisible(document.querySelector('#applicationForm'))) !== null && _k !== void 0 ? _k : validateElementVisible(document.querySelector('#getContinueButtonActionButton'))) !== null && _l !== void 0 ? _l : validateElementVisible(document.querySelector('#amContents'));
        }
        function stateRecognizer() {
            var _a, _b, _c;
            try {
                setFields();
                let state;
                const buttonPressDelay = Math.random() * 500 + 500;
                if (initialSetupIncompleteIndicator && initialSetupIncompleteEnabled) {
                    const skipButtons = Array.from((_a = document.querySelectorAll("#toggleSkipLoginTaskButton")) !== null && _a !== void 0 ? _a : []);
                    const completeButtons = Array.from((_b = document.querySelectorAll("#completeLoginTaskButton")) !== null && _b !== void 0 ? _b : []);
                    if (skipButtons.length > 0 && (completeButtons.length == skipButtons.length)) {
                        initialSetupIncompleteEnabled = false;
                        skipButtons.forEach((button) => {
                            clickElement(button);
                        });
                    }
                    const continueButtonDelay = Math.max(100, skipButtons.length * 20);
                    setTimeout(() => {
                        const isEnabled = validateElementEnabled(validateElementVisible(document.querySelector('#getContinueButtonActionButton')));
                        if (isEnabled) {
                            clickElement(validateElementVisible(document.querySelector('#getContinueButtonActionButton')));
                            handleConfirmInitialSetupIncomplete();
                        }
                        else {
                            const st = createState(WebLoginState.initialSetupIncomplete, {});
                            sendMessageToHost(JSON.stringify(st));
                        }
                    }, continueButtonDelay);
                }
                else if (successIndicator) {
                    let cookieValue = "";
                    try {
                        cookieValue = document.cookie;
                    }
                    catch (e) {
                        cookieValue = "";
                    }
                    state = createState(WebLoginState.success, {
                        cookie: cookieValue,
                        userAgent: navigator.userAgent
                    });
                }
                else if (otpChallengeIndicator || emailVerificationSuccessIndicator) {
                    if (shouldAutoLogin) {
                        state = createState(WebLoginState.otpTextError, { message: "Cannot send code without user participation." });
                    }
                    else if (errorIndicator && errorIndicator.innerText.trim()) {
                        state = createState(WebLoginState.otpTextError, { message: errorIndicator.innerText });
                    }
                    else if (emailVerificationSuccessIndicator) {
                        setTimeout(() => {
                            clickElement(document.querySelector(".btn.btn-secondary"));
                        }, buttonPressDelay);
                    }
                    else {
                        var infoMessage = getTextExcludingLinks(otpChallengeIndicator);
                        const isManualIBKey = validateElementVisible(document.querySelector(".xyzblock.xyzblock-manualibkey"));
                        const sizeCode = isManualIBKey ? 8 : 6;
                        const challengeCode = (_c = challengeCodeIndicator === null || challengeCodeIndicator === void 0 ? void 0 : challengeCodeIndicator.innerText) === null || _c === void 0 ? void 0 : _c.trim();
                        infoMessage = isManualIBKey ? infoMessage + "\nChallenge Code: " + challengeCode : infoMessage;
                        state = createState(WebLoginState.otpText, { infoMessage: infoMessage, isResendable: resendCodePushNotificationButton != null, codeLength: sizeCode });
                    }
                }
                else if (selectChallengeIndicator && challengeOptions && challengeOptions.length > 0) {
                    state = createState(WebLoginState.selectChallengeType, {
                        options: Array.from(challengeOptions).slice(1).map((option) => ({
                            title: option.innerText.trim(),
                            value: option.value
                        }))
                    });
                }
                else if (usernamePasswordIndicator) {
                    state = errorIndicator ? createState(WebLoginState.usernamePasswordError, { message: errorIndicator.innerText }) : createState(WebLoginState.usernamePassword, {});
                }
                else if (pushOptionIndicator) {
                    state = createState(WebLoginState.pushNotificationPrompt, {
                        isVerifying: !!loadingIndicator,
                        additionalData: showChallengeIndicator === null || showChallengeIndicator === void 0 ? void 0 : showChallengeIndicator.innerText,
                        isDenied: false
                    });
                }
                if (state)
                    sendMessageToHost(JSON.stringify(state));
            }
            catch (error) {
                sendMessageToHost(error.message, true);
            }
        }
        function handleConfirmInitialSetupIncomplete() {
            setTimeout(function () {
                initialSetupIncompleteEnabled = true;
                stateRecognizer();
            }, 4000);
        }
        function resetFallbackTimer() {
            if (fallbackTimerHandle)
                clearTimeout(fallbackTimerHandle);
            fallbackTimerHandle = setTimeout(() => {
                stateRecognizer();
                fallbackTimerHandle = null;
            }, 1000);
        }
        observeDOM(document.body, () => {
            if (timeoutHandle)
                clearTimeout(timeoutHandle);
            timeoutHandle = setTimeout(() => {
                stateRecognizer();
                if (fallbackTimerHandle) {
                    clearTimeout(fallbackTimerHandle);
                    fallbackTimerHandle = null;
                }
                resetFallbackTimer();
            }, 40);
        });
        stateRecognizer();
        function updateUsername(username) {
            updateInputField(usernameField, username);
        }
        function updatePassword(password) {
            updateInputField(passwordField, password);
        }
        function submitUsernamePassword() {
            return clickElement(loginButton);
        }
        function updateOTP(otp) {
            updateInputField(otpField, otp);
        }
        function submitOTP() {
            return clickElement(loginButton);
        }
        function continueToOTP() {
            return clickElement(showChallengeIndicator);
        }
        function resendNotificationOnClick() {
            return clickElement(resendCodePushNotificationButton);
        }
        function selectChallengeTypeOption(option) {
            if (selectChallengeIndicator) {
                selectChallengeIndicator.value = option;
            }
        }
        function submitChallengeType() {
            if (selectChallengeIndicator) {
                selectChallengeIndicator.dispatchEvent(new Event("change", { bubbles: true }));
            }
        }
        ("");

    """.trimIndent()
}