package com.example.webviewgecko

/**
 * @author Cesar Canaza
 * @contact cesar@joinautopilot.com  
 * @company Autopilot
 * Created 3/12/26
 */
object Script {
    val robinhoodULR ="https://robinhood.com/login"
    val ibkrURL ="https://portal.interactivebrokers.com/sso/Login"
    val robinhood= """
        "use strict";
        var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
            function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
            return new (P || (P = Promise))(function (resolve, reject) {
                function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
                function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
                function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
                step((generator = generator.apply(thisArg, _arguments || [])).next());
            });
        };
        var ExecutionPlatform;
        (function (ExecutionPlatform) {
            ExecutionPlatform[ExecutionPlatform["ios"] = 0] = "ios";
            ExecutionPlatform[ExecutionPlatform["android"] = 1] = "android";
        })(ExecutionPlatform || (ExecutionPlatform = {}));
        let defaults = {
            executionPlatform: ExecutionPlatform.android,
            shouldAutoLogin: false,
        };
        /**
         * These values will be replaced according to the parameters
         * passed in the `getScript` function in the Firebase function.
         */
        let executionPlatform = defaults.executionPlatform;
        let shouldAutoLogin = defaults.shouldAutoLogin;
        let otpField;
        let usernameField;
        let passwordField;
        let loginButton;
        let trustDeviceCheckbox;
        let useSecurityCodeCheckbox;
        let submitOTPButton;
        let singleSMSNumber;
        let smsNumberOptions;
        let usernamePasswordIndicator;
        let selectOTPOptionIndicator;
        let enterOTPIndicator;
        let accountNavIndicator;
        let portfolioValueIndicator;
        let errorIndicator;
        let otpErrorIndicator;
        let systemErrorHeader;
        let resendCodeOPTButton;
        let pushNotificationPromptIndicator;
        let pushNotificationLoadingIndicator;
        let pushNotificationTitleElement;
        let resendPushNotificationButton;
        let takePhotoLoadingIndicator;
        let takeLivePhotoIndicator;
        let infoMessageContainer;
        let termsAndConditionsIndicator;
        let trustThisDeviceRadioButton;
        let trustDeviceContinueButton;
        let paperlessNotTodayButton;
        let accountTitleElements;
        let accountDescriptionElements;
        let continueToPlaidAuthenticationButton;
        let plaidAuthenticationIframe;
        let identyVerificationError = false;
        let tryAgainButton;
        let smsButton;
        let initialSetupIncompleteIndicator;
        let selectedSMSNumberIndex = 0;
        let modalButtons;
        let closeModalDialogButton;
        let verifyWithSelfieButton;
        let modalDialog;
        let timeSelfieButtonClicked = null;
        let liveSelfieViewDismissed = false;
        var WebLoginState;
        (function (WebLoginState) {
            WebLoginState["usernamePassword"] = "usernamePassword";
            WebLoginState["selectSMSNumber"] = "selectSMSNumber";
            WebLoginState["otpText"] = "otpText";
            WebLoginState["usernamePasswordError"] = "usernamePasswordError";
            WebLoginState["otpTextError"] = "otpTextError";
            WebLoginState["success"] = "success";
            WebLoginState["resetPassword"] = "resetPassword";
            WebLoginState["systemError"] = "systemError";
            WebLoginState["pushNotificationPrompt"] = "pushNotificationPrompt";
            WebLoginState["accountRequiresSetup"] = "accountRequiresSetup";
            WebLoginState["initialSetupIncomplete"] = "initialSetupIncomplete";
            WebLoginState["plaidAuthenticationAccounts"] = "plaidAuthenticationAccounts";
            WebLoginState["plaidAuthenticationIframe"] = "plaidAuthenticationIframe";
            WebLoginState["unableToVerifyIdentity"] = "unableToVerifyIdentity";
            WebLoginState["takeLivePhotoIframe"] = "takeLivePhotoIframe";
        })(WebLoginState || (WebLoginState = {}));
        function sendMessageToHost(message, isError = false) {
            var _a;
            if (executionPlatform === ExecutionPlatform.ios) {
                (_a = window.webkit) === null || _a === void 0 ? void 0 : _a.messageHandlers.handler.postMessage(message);
            }
            else {
                if (isError) {
                    Android.onError(message);
                }
                else {
                    Android.onReadJson(message);
                }
            }
        }
        function setFields() {
            var _a, _b, _c, _d, _e, _f, _g, _h;
            otpField = document.querySelector('input[autocomplete="one-time-code"]');
            usernameField = document.querySelector('input[name="username"]');
            passwordField = document.querySelector('input[name="password"]');
            loginButton = document.querySelector("footer button[type='submit']");
            const personaContainer = document.querySelector('div[data-testid="persona-container"]');
            const personaConsent = document.querySelector('div[data-testid="persona-consent"]');
            takeLivePhotoIndicator = personaConsent !== null && personaConsent !== void 0 ? personaConsent : personaContainer;
            closeModalDialogButton = document.querySelector('#react_root>div:last-child>div:last-child button[aria-label="Close"]');
            trustDeviceCheckbox = document.querySelector("input[name='long_session']");
            useSecurityCodeCheckbox = document.getElementById("securityCode");
            modalDialog = (_a = document.querySelector('div[data-testid="rh-design-modal-backdrop"]')) !== null && _a !== void 0 ? _a : document.querySelector('dialog[aria-modal="true"][open]');
            modalButtons = modalDialog ? Array.from(modalDialog.querySelectorAll('button')) : null;
            submitOTPButton = modalDialog
                ? (modalDialog.querySelector('button[type="submit"]') ||
                    Array.from(modalDialog.querySelectorAll('button')).find(button => button.innerText.includes('Continue')) || null)
                : null;
            selectOTPOptionIndicator = document.getElementById("authenticatorSelectorForm");
            singleSMSNumber = document.getElementById("target0");
            smsNumberOptions = document.querySelectorAll(".widgets.target-input");
            usernamePasswordIndicator = document.querySelector('input[name="username"]');
            enterOTPIndicator = document.querySelector('input[autocomplete="one-time-code"]');
            accountNavIndicator = document.querySelector('nav a.rh-hyperlink[href="/account"]');
            portfolioValueIndicator = document.querySelector('h2[data-testid="PortfolioValue"]');
            errorIndicator = document.querySelector('div[data-testid="LoginErrorMessage"]');
            if (!otpField) {
                otpErrorIndicator = document.querySelector("p.css-n2rtdi");
            }
            else {
                otpErrorIndicator = null;
            }
            resendPushNotificationButton = modalDialog ? Array.from(modalDialog.querySelectorAll('button')).find((elem) => elem.innerText.toLocaleLowerCase().includes("resend")) : null;
            pushNotificationPromptIndicator = document.querySelector('button[data-testid="device-approval-fallback-button"]');
            pushNotificationTitleElement = modalDialog === null || modalDialog === void 0 ? void 0 : modalDialog.querySelector('main h1');
            pushNotificationLoadingIndicator = modalDialog ? (_b = Array.from(modalDialog.querySelectorAll('div main div h1')).find((h1) => { var _a; return (_a = h1 === null || h1 === void 0 ? void 0 : h1.textContent) === null || _a === void 0 ? void 0 : _a.toLowerCase().includes("we recognize this device. verifying it"); })) !== null && _b !== void 0 ? _b : null : null;
            takePhotoLoadingIndicator = modalDialog ? (_c = Array.from(modalDialog.querySelectorAll('div h1')).find((h1) => { var _a; return ((_a = h1 === null || h1 === void 0 ? void 0 : h1.textContent) === null || _a === void 0 ? void 0 : _a.toLowerCase()) === "reviewing your info..."; })) !== null && _c !== void 0 ? _c : null : null;
            resendCodeOPTButton = document.getElementById("lnkGetNewCode");
            systemErrorHeader = document.getElementById("informationContainer");
            infoMessageContainer = document.querySelector('[data-testid="PageContainerContent"] p');
            termsAndConditionsIndicator = document.getElementById("terms-and-conditions-container");
            trustThisDeviceRadioButton = document.getElementById("remember-device-yes");
            trustDeviceContinueButton = document.getElementById("btnContinue");
            paperlessNotTodayButton = document.getElementById("ctl00_WebPartManager1_wpPaperless_PaperLessSplashUserControl_btnNotToday1");
            accountTitleElements = document.querySelectorAll(".css-la8vf2");
            accountDescriptionElements = document.querySelectorAll(".css-9f1dr5");
            const iframes = Array.from(document.querySelectorAll('iframe[title="Plaid Link"]'));
            plaidAuthenticationIframe = iframes.length > 0 ? iframes[iframes.length - 1] : null;
            continueToPlaidAuthenticationButton = (_d = document.querySelector('div[data-testid="PageContainer"] button[data-testid="continue"]')) !== null && _d !== void 0 ? _d : (modalDialog ? modalDialog.querySelector('button[data-testid="primary-cta"]') : null);
            const buttons = Array.from(document.querySelectorAll("button"));
            smsButton = (_e = buttons.find((button) => { var _a; return ((_a = button.innerText) === null || _a === void 0 ? void 0 : _a.toLowerCase()) === "sms"; })) !== null && _e !== void 0 ? _e : null;
            const allHeaders = Array.from(document.querySelectorAll('h1'));
            if (allHeaders.findIndex((header) => header.textContent === 'We can’t verify you') !== -1) {
                identyVerificationError = true;
            }
            else {
                identyVerificationError = false;
            }
            const allIncompleteIndicators = Array.from(document.querySelectorAll('div[data-testid="PortfolioDetail"] div main section h1'));
            initialSetupIncompleteIndicator = (_f = allIncompleteIndicators.find((header) => { var _a, _b; return ((((_a = header.textContent) === null || _a === void 0 ? void 0 : _a.toLowerCase()) === "welcome to robinhood") || (((_b = header.textContent) === null || _b === void 0 ? void 0 : _b.toLowerCase()) === "investing starts here")); })) !== null && _f !== void 0 ? _f : null;
            tryAgainButton = modalButtons ? (_g = modalButtons.find((button) => { var _a; return ((_a = button.textContent) === null || _a === void 0 ? void 0 : _a.toLowerCase()) === "please try again"; })) !== null && _g !== void 0 ? _g : null : null;
            verifyWithSelfieButton = (_h = document.querySelector('button[data-testid="Verify with selfie instead"]')) !== null && _h !== void 0 ? _h : document.querySelector('button[data-testid="secondary-cta"]');
        }
        setFields();
        function createState(type, props) {
            return { type: type.valueOf(), props };
        }
        let returnedTokens = false;
        let usernameSubmitted = false;
        let clickedVerifyWithBank = false;
        let plaidAccountsState = null;
        function stateRecognizer() {
            try {
                let state = (() => {
                    var _a, _b, _c, _d, _e;
                    setFields();
                    const buttonPressDelay = Math.random() * 500 + 500;
                    const isSuccess = accountNavIndicator && portfolioValueIndicator;
                    const isInitialSetupIncomplete = initialSetupIncompleteIndicator && accountNavIndicator && portfolioValueIndicator == null;
                    (_a = closeModalDialogButton === null || closeModalDialogButton === void 0 ? void 0 : closeModalDialogButton.style) === null || _a === void 0 ? void 0 : _a.setProperty('display', 'none');
                    if (isSuccess) {
                        return createState(WebLoginState.success, { cookie: "" });
                    }
                    else if (paperlessNotTodayButton) {
                        setTimeout(() => {
                            paperlessNotTodayButton === null || paperlessNotTodayButton === void 0 ? void 0 : paperlessNotTodayButton.click();
                        }, buttonPressDelay);
                    }
                    else if (termsAndConditionsIndicator) {
                        return createState(WebLoginState.accountRequiresSetup, {});
                    }
                    else if (isInitialSetupIncomplete) {
                        let description = document.querySelector('div[data-testid="PortfolioDetail"] div main section:nth-of-type(2) div div div span:nth-of-type(2)');
                        return createState(WebLoginState.initialSetupIncomplete, {
                            message: description === null || description === void 0 ? void 0 : description.innerHTML
                        });
                    }
                    else if (smsButton) {
                        if (shouldAutoLogin) {
                            return createState(WebLoginState.otpTextError, {
                                message: "Cannot verify OTP without user participation"
                            });
                        }
                        setTimeout(() => {
                            smsButton === null || smsButton === void 0 ? void 0 : smsButton.click();
                        }, buttonPressDelay);
                    }
                    else if (smsNumberOptions.length >= 1) {
                        if (shouldAutoLogin) {
                            return createState(WebLoginState.otpTextError, {
                                message: "Cannot verify OTP without user participation"
                            });
                        }
                        if (smsNumberOptions.length === 1) {
                            setTimeout(() => {
                                smsNumberOptions[0].click();
                            }, buttonPressDelay);
                        }
                        else {
                            let numbers = Array.from(smsNumberOptions).map((number) => number.innerText);
                            return createState(WebLoginState.selectSMSNumber, { numbers });
                        }
                    }
                    else if (otpErrorIndicator &&
                        otpErrorIndicator.innerText.trim().length > 0) {
                        let okButton = (_b = modalButtons === null || modalButtons === void 0 ? void 0 : modalButtons.find((button) => { var _a; return ((_a = button.innerText) === null || _a === void 0 ? void 0 : _a.toLowerCase()) === "ok"; })) !== null && _b !== void 0 ? _b : null;
                        setTimeout(() => {
                            okButton === null || okButton === void 0 ? void 0 : okButton.click();
                        }, buttonPressDelay);
                        return createState(WebLoginState.otpTextError, {
                            message: otpErrorIndicator.innerText,
                        });
                    }
                    else if (enterOTPIndicator) {
                        usernameSubmitted = true;
                        if (shouldAutoLogin) {
                            return createState(WebLoginState.otpTextError, {
                                message: "Cannot verify OTP without user participation"
                            });
                        }
                        let infoMessage = infoMessageContainer === null || infoMessageContainer === void 0 ? void 0 : infoMessageContainer.innerText;
                        if (otpErrorIndicator &&
                            otpErrorIndicator.innerText.trim().length > 0) {
                            return createState(WebLoginState.otpTextError, {
                                message: otpErrorIndicator.innerText,
                            });
                        }
                        else {
                            return createState(WebLoginState.otpText, { infoMessage: infoMessage });
                        }
                    }
                    else if (trustThisDeviceRadioButton) {
                        setTimeout(() => {
                            trustThisDeviceRadioButton === null || trustThisDeviceRadioButton === void 0 ? void 0 : trustThisDeviceRadioButton.click();
                        }, buttonPressDelay);
                        // A random number between -250 and 250
                        let randomOffset = Math.random() * 500 - 250;
                        setTimeout(() => {
                            trustDeviceContinueButton === null || trustDeviceContinueButton === void 0 ? void 0 : trustDeviceContinueButton.click();
                        }, buttonPressDelay + randomOffset);
                    }
                    else if (pushNotificationPromptIndicator) {
                        usernameSubmitted = true;
                        if (shouldAutoLogin) {
                            return createState(WebLoginState.otpTextError, {
                                message: "Cannot perform device approval without user participation"
                            });
                        }
                        const promptTitle = (_c = pushNotificationTitleElement === null || pushNotificationTitleElement === void 0 ? void 0 : pushNotificationTitleElement.innerText.toLocaleLowerCase()) !== null && _c !== void 0 ? _c : "";
                        const isDenied = promptTitle.includes("deny") || promptTitle.includes("denied");
                        return createState(WebLoginState.pushNotificationPrompt, {
                            isVerifying: pushNotificationLoadingIndicator !== null && pushNotificationLoadingIndicator !== void 0 ? pushNotificationLoadingIndicator : false,
                            additionalData: pushNotificationPromptIndicator.innerText,
                            isDenied
                        });
                    }
                    else if (pushNotificationLoadingIndicator) {
                        return createState(WebLoginState.pushNotificationPrompt, {
                            isVerifying: true
                        });
                    }
                    else if (systemErrorHeader) {
                        let cmsSection = systemErrorHeader.querySelector(".cms-section");
                        let errorTextContent = (_d = cmsSection === null || cmsSection === void 0 ? void 0 : cmsSection.textContent) !== null && _d !== void 0 ? _d : "An unknown system error occurred";
                        // The inner div exists, access its content
                        var content = errorTextContent.trim();
                        return createState(WebLoginState.systemError, {
                            message: content,
                        });
                    }
                    else if (accountTitleElements.length > 0) {
                        usernameSubmitted = true;
                        let accountTitles = Array.from(accountTitleElements).map((element) => element.textContent);
                        let accountDescriptions = Array.from(accountDescriptionElements).map((element) => element.textContent);
                        let accounts = accountTitles.map((title, index) => {
                            return { title, description: accountDescriptions[index] };
                        });
                        plaidAccountsState = createState(WebLoginState.plaidAuthenticationAccounts, { accounts });
                        return createState(WebLoginState.plaidAuthenticationAccounts, {
                            accounts,
                            additionalData: verifyWithSelfieButton === null || verifyWithSelfieButton === void 0 ? void 0 : verifyWithSelfieButton.innerText
                        });
                    }
                    else if (identyVerificationError) {
                        return createState(WebLoginState.unableToVerifyIdentity, {});
                    }
                    else if ((takeLivePhotoIndicator || takePhotoLoadingIndicator) && !liveSelfieViewDismissed) {
                        const isVerifying = takePhotoLoadingIndicator &&
                            !takeLivePhotoIndicator &&
                            Date.now() - (timeSelfieButtonClicked !== null && timeSelfieButtonClicked !== void 0 ? timeSelfieButtonClicked : 0) > 5000;
                        return createState(WebLoginState.takeLivePhotoIframe, {
                            isVerifying
                        });
                    }
                    else if (plaidAuthenticationIframe) {
                        if (plaidAuthenticationIframe.style.display === "none") {
                            let dismissButton = (_e = document.querySelector('button[data-testid="test-dismiss-button"]')) !== null && _e !== void 0 ? _e : document.querySelector('button[data-testid="dismiss-button"]');
                            dismissButton === null || dismissButton === void 0 ? void 0 : dismissButton.click();
                            if (dismissButton) {
                                plaidAuthenticationIframe === null || plaidAuthenticationIframe === void 0 ? void 0 : plaidAuthenticationIframe.remove();
                                plaidAuthenticationIframe = null;
                                plaidAccountsState = null;
                                dismissedModalDialog();
                            }
                            return plaidAccountsState;
                        }
                        return createState(WebLoginState.plaidAuthenticationIframe, {});
                    }
                    else if (plaidAccountsState) {
                        return plaidAccountsState;
                    }
                    else if (usernamePasswordIndicator && (!usernameSubmitted || liveSelfieViewDismissed)) {
                        if (errorIndicator) {
                            let errorMessage = errorIndicator.innerText;
                            return createState(WebLoginState.usernamePasswordError, {
                                message: errorMessage,
                            });
                        }
                        else {
                            if (trustDeviceCheckbox) {
                                setTimeout(() => {
                                    if (!(trustDeviceCheckbox === null || trustDeviceCheckbox === void 0 ? void 0 : trustDeviceCheckbox.checked)) {
                                        trustDeviceCheckbox === null || trustDeviceCheckbox === void 0 ? void 0 : trustDeviceCheckbox.click();
                                    }
                                }, buttonPressDelay);
                            }
                            return createState(WebLoginState.usernamePassword, {});
                        }
                    }
                })();
                if ((state === null || state === void 0 ? void 0 : state.type) === WebLoginState.success) {
                    (() => __awaiter(this, void 0, void 0, function* () {
                        let tokens = yield extractTokens();
                        let stringifiedTokens = JSON.stringify(tokens);
                        if (!returnedTokens) {
                            returnedTokens = true;
                            let state = createState(WebLoginState.success, { cookie: "", additionalData: stringifiedTokens });
                            sendMessageToHost(JSON.stringify(state));
                        }
                    }))();
                }
                else if (state) {
                    sendMessageToHost(JSON.stringify(state));
                }
            }
            catch (error) {
                sendMessageToHost(error.message, true);
            }
        }
        var observeDOM = (function () {
            var MutationObserver = window.MutationObserver || window.WebKitMutationObserver;
            return function (obj, callback) {
                if (!obj || obj.nodeType !== 1) {
                    return;
                }
                if (MutationObserver) {
                    // define a new observer
                    var mutationObserver = new MutationObserver(callback);
                    // have the observer observe for changes in children
                    mutationObserver.observe(obj, {
                        childList: true,
                        subtree: true,
                        attributes: true,
                        attributeFilter: ["data-testid"]
                    });
                    return mutationObserver;
                }
                // browser support fallback
                else {
                    obj.addEventListener("DOMNodeInserted", callback, false);
                    obj.addEventListener("DOMNodeRemoved", callback, false);
                }
            };
        })();
        observeDOM(document.body, () => {
            stateRecognizer();
        });
        stateRecognizer();
        function extractTokens() {
            return __awaiter(this, void 0, void 0, function* () {
                return yield new Promise((resolve, reject) => {
                    const request = window.indexedDB.open('localforage');
                    let db;
                    let found = false;
                    request.onerror = function (event) {
                        console.error('Unable to retrieve from db');
                        reject(new Error('Unable to retrieve from db'));
                    };
                    request.onsuccess = function (event) {
                        var _a;
                        db = (_a = event.target) === null || _a === void 0 ? void 0 : _a.result;
                        const finder = setInterval(() => {
                            retrieve(db);
                            if (found)
                                clearInterval(finder);
                        }, 500);
                    };
                    function retrieve(db) {
                        const tx = db.transaction(['keyvaluepairs'], 'readonly').objectStore('keyvaluepairs').get('reduxPersist:auth');
                        tx.onsuccess = (event) => {
                            try {
                                const result = event.target.result;
                                const parsed = JSON.parse(JSON.parse(result));
                                console.log(parsed);
                                const mainArrayIndex = parsed.findIndex((item) => Array.isArray(item));
                                const mainArray = parsed[mainArrayIndex];
                                const accessToken = getDBValue('access_token', mainArray);
                                const refreshToken = getDBValue('refresh_token', mainArray);
                                const expiresInString = getDBValue('expires_in', mainArray);
                                const expiresIn = parseInt(expiresInString !== null && expiresInString !== void 0 ? expiresInString : '', 10);
                                if (accessToken == null || refreshToken == null || expiresIn == null) {
                                    reject(new Error('Token not found'));
                                }
                                else {
                                    resolve({ accessToken, refreshToken, expiresIn });
                                }
                                found = true;
                            }
                            catch (error) {
                                console.log(`Error parsing token: ${'$'}{error}`);
                                if (error instanceof Error) {
                                    reject(error);
                                }
                                else {
                                    reject(new Error('Unknown error'));
                                }
                            }
                        };
                    }
                });
            });
        }
        function getDBValue(key, parsedDB) {
            const tokenKeyIndex = parsedDB.findIndex((item) => item === key);
            return parsedDB[tokenKeyIndex + 1];
        }
        /**
         * BEGIN "FFI"
         *
         * DO NOT ALTER FUNCTION NAMES OR PARAMETERS
         *
         * The mobile app uses these functions to access inputs and buttons on the page
         */
        function updateUsername(username) {
            var _a;
            if (usernameField != null) {
                let nativeInputValueSetter = (_a = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value")) === null || _a === void 0 ? void 0 : _a.set;
                nativeInputValueSetter === null || nativeInputValueSetter === void 0 ? void 0 : nativeInputValueSetter.call(usernameField, username);
                let event = new Event("input", { bubbles: true });
                usernameField.dispatchEvent(event);
            }
        }
        function updatePassword(password) {
            var _a;
            if (passwordField) {
                let nativeInputValueSetter = (_a = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value")) === null || _a === void 0 ? void 0 : _a.set;
                nativeInputValueSetter === null || nativeInputValueSetter === void 0 ? void 0 : nativeInputValueSetter.call(passwordField, password);
                let event = new Event("input", { bubbles: true });
                passwordField.dispatchEvent(event);
            }
        }
        function sendOTP() {
            // No-op for E*Trade
        }
        function resendOPTCode() {
            return clickElement(resendCodeOPTButton);
        }
        function updateOTP(otp) {
            var _a;
            if (otpField) {
                let nativeInputValueSetter = (_a = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value")) === null || _a === void 0 ? void 0 : _a.set;
                nativeInputValueSetter === null || nativeInputValueSetter === void 0 ? void 0 : nativeInputValueSetter.call(otpField, otp);
                let event = new Event("input", { bubbles: true });
                otpField.dispatchEvent(event);
            }
        }
        function submitUsernamePassword() {
            liveSelfieViewDismissed = false;
            return clickElement(loginButton);
        }
        function continueToOTP() {
            timeSelfieButtonClicked = Date.now();
            return clickElement(pushNotificationPromptIndicator) || clickElement(verifyWithSelfieButton);
        }
        function verifyWithSelfie() {
            timeSelfieButtonClicked = Date.now();
            return clickElement(verifyWithSelfieButton);
        }
        function submitOTP() {
            return clickElement(submitOTPButton);
        }
        function updateSMSNumber(index) {
            selectedSMSNumberIndex = index;
        }
        function updateSymTecCode(code) {
            // No-op for schwab
        }
        function setUseSecurityCode(useSecurityCode) {
            if (useSecurityCodeCheckbox) {
                useSecurityCodeCheckbox.checked = useSecurityCode;
            }
        }
        function continueToPlaidAuthentication() {
            continueToPlaidAuthenticationButton === null || continueToPlaidAuthenticationButton === void 0 ? void 0 : continueToPlaidAuthenticationButton.click();
        }
        function pressTryAgainButton() {
            return clickElement(tryAgainButton);
        }
        // Keeping this for backwards compatibility
        function dismissedLiveSelfieView() {
            dismissedLiveSelfieView();
        }
        function dismissedModalDialog() {
            usernameSubmitted = false;
            liveSelfieViewDismissed = true;
            stateRecognizer();
            setTimeout(() => {
                clickElement(closeModalDialogButton);
            }, 500);
        }
        function resendPushNotification() {
            clickElement(resendPushNotificationButton);
        }
        function getElementCenter(element) {
            const rect = element.getBoundingClientRect();
            const xCenter = (rect.left + rect.right) / 2;
            const yCenter = (rect.top + rect.bottom) / 2;
            return { x: xCenter, y: yCenter };
        }
        function clickElement(element) {
            if (!element) {
                return false;
            }
            let { x, y } = getElementCenter(element);
            let event = new MouseEvent("mousedown", {
                view: window,
                bubbles: true,
                cancelable: true,
                clientX: x,
                clientY: y,
                button: 0, // Left button click
                buttons: 1, // Indicates the primary button is pressed
                altKey: false,
                ctrlKey: false,
                shiftKey: false,
                metaKey: false,
                detail: 1,
                screenX: x - window.screenX,
                screenY: y - window.screenY,
                relatedTarget: null,
                movementX: 0,
                movementY: 0,
            });
            element.dispatchEvent(event);
            setTimeout(() => {
                let event = new MouseEvent("mouseup", {
                    view: window,
                    bubbles: true,
                    cancelable: true,
                    clientX: x,
                    clientY: y,
                    button: 0, // Left button click
                    buttons: 1, // Indicates the primary button is pressed
                    altKey: false,
                    ctrlKey: false,
                    shiftKey: false,
                    metaKey: false,
                    detail: 1,
                    screenX: x - window.screenX,
                    screenY: y - window.screenY,
                    relatedTarget: null,
                    movementX: 0,
                    movementY: 0,
                });
                element.dispatchEvent(event);
                element.click();
            }, 15);
            return true;
        }
        /**
         * END "FFI"
         */
        /** This empty string expression is necessary to prevent an error when Swift evaluates this code */
        ("");

    """.trimIndent()

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