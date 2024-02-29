import { EventBridge } from './createEventBridge';
import { noop } from './utils';
import { Scheduler } from './createScheduler';
import { Unsubscribe } from './types';

export const GooglePayTokenState = {
  UNTOKENIZED: 1,
  PENDING: 2,
  NEEDS_IDENTITY_VERIFICATION: 3,
  SUSPENDED: 4,
  ACTIVE: 5,
  FELICA_PENDING_PROVISIONING: 6,
} as const;

export type GooglePayTokenState = (typeof GooglePayTokenState)[keyof typeof GooglePayTokenState];

export const GooglePayCardNetwork = {
  AMEX: 1,
  DISCOVER: 2,
  MASTERCARD: 3,
  VISA: 4,
  INTERAC: 5,
  PRIVATE_LABEL: 6,
  EFTPOS: 7,
  MAESTRO: 8,
  ID: 9,
  QUICPAY: 10,
  JCB: 11,
  ELO: 12,
  MIR: 13,
} as const;

export type GooglePayCardNetwork = (typeof GooglePayCardNetwork)[keyof typeof GooglePayCardNetwork];

export const GooglePayTokenServiceProvider = {
  AMEX: 2,
  MASTERCARD: 3,
  VISA: 4,
  DISCOVER: 5,
  EFTPOS: 6,
  INTERAC: 7,
  OBERTHUR: 8,
  PAYPAL: 9,
  JCB: 13,
  ELO: 14,
  GEMALTO: 15,
  MIR: 16,
} as const;

export type GooglePayTokenServiceProvider =
  (typeof GooglePayTokenServiceProvider)[keyof typeof GooglePayTokenServiceProvider];

export interface GooglePayTokenInfo {
  network: GooglePayCardNetwork;
  tokenServiceProvider: GooglePayTokenServiceProvider;
  tokenState: GooglePayTokenState;
  dpanLastFour: string;
  fpanLastFour: string;
  issuerName: string;
  issuerTokenId: string;
  portfolioName: string;
  isDefaultToken: boolean;
}

export interface GooglePayUserAddress {
  name?: string | null;
  address1?: string | null;
  address2?: string | null;
  locality?: string | null; // Mountain View
  administrativeArea?: string | null; // CA
  countryCode?: string | null;
  postalCode?: string | null;
  phoneNumber?: string | null;
}

export interface GooglePayTokenStatus {
  tokenState: GooglePayTokenState;
  isSelected: boolean;
}

export interface GooglePayPushTokenizeRequest {
  /**
   * The Opaque Payment Card (OPC) binary data.
   */
  opaquePaymentCard: String;

  /**
   * The display name or nickname used to describe the payment card in the user interface.
   */
  displayName: String;

  /**
   * The last 4 digits for the payment card required to correctly display the card in Google Pay UI.
   */
  lastFour: String;

  /**
   * The card payment network.
   */
  network: GooglePayCardNetwork;

  /**
   * The TSP that should be used for the tokenization attempt.
   */
  tokenServiceProvider: GooglePayTokenServiceProvider;

  /**
   * The user's address.
   */
  userAddress?: GooglePayUserAddress | null;
}

export interface GooglePayTokenizeRequest {
  displayName: String;
  network: GooglePayCardNetwork;
  tokenServiceProvider: GooglePayTokenServiceProvider;
  tokenId?: string | null;
}

export interface GooglePayManager {
  /**
   * Get the ID of the active wallet, or `null` if there's no active wallet.
   *
   * @throws ApiException
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet?authuser=1#getactivewalletid)
   */
  getActiveWalletId(): Promise<string | null>;

  /**
   * Returns the token status for a token in the active wallet.
   *
   * The Push Provisioning API caches status values retrieved from the networks and makes a best effort to keep these
   * cached values up-to-date. However, in some cases the values returned here may be different from the network values.
   * For example, if a user has cleared data so that the token is no longer on the device, then `null` is returned.
   *
   * @throws ApiException
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet?authuser=1#gettokenstatus)
   */
  getTokenStatus(
    tokenServiceProvider: GooglePayTokenServiceProvider,
    tokenId: string
  ): Promise<GooglePayTokenStatus | null>;

  /**
   * Returns the name of the current Google Pay environment, for example: PROD, SANDBOX, or DEV.
   *
   * @throws ApiException
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet?authuser=1#getenvironment)
   */
  getEnvironment(): Promise<string>;

  /**
   * Get the stable hardware ID of the device.
   *
   * Each physical Android device has a stable hardware ID which is consistent between wallets for a given device. This
   * ID will change as a result of a factory reset.
   *
   * The stable hardware ID _may only_ be used for the following purposes:
   * - To encrypt inside OPC and provide back to Google Pay for push provisioning;
   * - To make a risk decision (without storing the value) before the start of a Push Provisioning flow.
   *
   * The stable hardware ID received through the client API _must not_ be used for the following purposes:
   * - Stored by the issuer locally or at the backend;
   * - Track user activity.
   *
   * The stable hardware ID may not be accessed by the issuer outside the Push Provisioning flow.
   *
   * @throws ApiException
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet?authuser=1#getstablehardwareid)
   */
  getStableHardwareId(): Promise<string>;

  /**
   * Get all tokens available in the wallet.
   *
   * The API only returns token details for tokens with metadata matching your app package name. You can check if your
   * tokens have this linking by tapping on the card in the Google Wallet app to see the card details view.
   *
   * @throws ApiException
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet?authuser=1#getactivewalletid)
   */
  listTokens(): Promise<GooglePayTokenInfo[]>;

  /**
   * Searches the wallet for a token and returns `true` if found, or `false` otherwise.
   *
   * @returns `true` if it finds a token with same last four FPAN digits as the identifier, as well as matches on the
   * other fields. False positives may be returned since the last four FPAN digits are not necessarily unique among
   * tokens.
   * @throws ApiException
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet?authuser=1#istokenized)
   */
  isTokenized(
    fpanLastFour: string,
    network: GooglePayCardNetwork,
    tokenServiceProvider: GooglePayTokenServiceProvider
  ): Promise<boolean>;

  /**
   * Open Google Pay app and reveal the card.
   *
   * **Note:** This is a UI-blocking operation. All consequent UI operations are suspended until this one is completed.
   *
   * @returns `true` if Google Pay app was opened, or `false` otherwise.
   * @throws ApiException
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet?authuser=1#viewtoken)
   */
  viewToken(tokenId: string, tokenServiceProvider: GooglePayTokenServiceProvider): Promise<boolean>;

  /**
   * Starts the push tokenization flow in which the issuer provides most or all card details needed for Google Pay to
   * get a valid token. Tokens added using this method are added to the active wallet.
   *
   * **Note:** This is a UI-blocking operation. All consequent UI operations are suspended until this one is completed.
   *
   * @returns The token ID, or `null` if tokenization wasn't completed.
   * @throws ApiException
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations?authuser=1#push_provisioning_operations)
   * @see [Sequence diagrams for Android Push Provisioning](https://developers.google.com/pay/issuers/apis/push-provisioning/android/integration-steps)
   */
  pushTokenize(request: GooglePayPushTokenizeRequest): Promise<string | null>;

  /**
   * Starts the manual tokenization flow in which the user needs to scan the card or enter all card details manually in
   * a form. Tokens added using this method are added to the active wallet.
   *
   * **Important:** This method is primarily used for activating tokens pending
   * [yellow path activation](https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations?authuser=1#resolving_yellow_path).
   * It can also be used as a fallback in error handling (e.g. if the app cannot retrieve an OPC because your server is
   * down). However, it should not be used as a substitute for a proper push provisioning integration.
   *
   * **Note:** This is a UI-blocking operation. All consequent UI operations are suspended until this one is completed.
   *
   * @returns The token ID, or `null` if tokenization wasn't completed.
   * @throws ApiException
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations?authuser=1#manual_provisioning)
   */
  tokenize(request: GooglePayTokenizeRequest): Promise<string | null>;

  /**
   * Brings up a dialog asking the user to confirm the intention to set the identified card as their selected (default)
   * card.
   *
   * **Note:** This is a UI-blocking operation. All consequent UI operations are suspended until this one is completed.
   *
   * @throws ApiException
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations?authuser=1#setting_the_default_token)
   */
  requestSelectToken(tokenId: string, tokenServiceProvider: GooglePayTokenServiceProvider): Promise<void>;

  /**
   * Brings up a dialog asking the user to confirm the deletion of the indicated token.
   *
   * Deleting the token does not affect the card-on-file on the user's Google account if one exists. To delete a
   * card-on-file, the user would need to go to the Google Payments Center or use the Google Wallet app.
   *
   * **Note:** This is a UI-blocking operation. All consequent UI operations are suspended until this one is completed.
   *
   * @throws ApiException
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations?authuser=1#token_deletion)
   */
  requestDeleteToken(tokenId: string, tokenServiceProvider: GooglePayTokenServiceProvider): Promise<void>;

  /**
   * Some issuers may need the active wallet ID before creating an opaque payment card for push provisioning. If the
   * wallet ID does not need to be included in the opaque payment card, simply let the {@link tokenize} and
   * {@link pushTokenize} manage wallet creation automatically.
   *
   * **Note:** This is a UI-blocking operation. All consequent UI operations are suspended until this one is completed.
   *
   * @throws ApiException
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations?authuser=1#create_wallet)
   */
  createWallet(): Promise<void>;

  /**
   * The Push Provisioning API will immediately call a listener whenever the following events occur:
   *
   * - The active wallet changes (by changing the active account);
   * - The selected card of the active wallet changes;
   * - Tokenized cards are added or removed from the active wallet;
   * - The status of a token in the active wallet changes.
   *
   * Registering for these broadcasts allows an app to re-query information about their digitized cards whenever the
   * user makes a change.
   *
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet?authuser=1#data_change_callbacks)
   */
  subscribe(listener: () => void): Unsubscribe;
}

/**
 * Manages tokenized cards in Google Pay.
 *
 * [Reading wallet state.](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet)
 *
 * @param eventBridge The underlying event bridge.
 * @param uiScheduler The callback that schedules an operation that blocks the UI.
 */
export function createGooglePayManager(eventBridge: EventBridge, uiScheduler: Scheduler): GooglePayManager {
  return {
    getActiveWalletId: () =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.GooglePayGetActiveWalletIdEvent' })
        .then(event => event.payload.walletId),

    getTokenStatus: (tokenServiceProvider, tokenId) =>
      eventBridge
        .requestAsync({
          type: 'org.racehorse.GooglePayGetTokenStatusEvent',
          payload: { tokenServiceProvider, tokenId },
        })
        .then(event => event.payload.status),

    getEnvironment: () =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.GooglePayGetEnvironmentEvent' })
        .then(event => event.payload.environment),

    getStableHardwareId: () =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.GooglePayGetStableHardwareIdEvent' })
        .then(event => event.payload.hardwareId),

    listTokens: () =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.GooglePayListTokensEvent' })
        .then(event => event.payload.tokenInfos),

    isTokenized: (fpanLastFour, network, tokenServiceProvider) =>
      eventBridge
        .requestAsync({
          type: 'org.racehorse.GooglePayIsTokenizedEvent',
          payload: { fpanLastFour, network, tokenServiceProvider },
        })
        .then(event => event.payload.isTokenized),

    viewToken: (tokenId, tokenServiceProvider) =>
      uiScheduler.schedule(() =>
        eventBridge
          .requestAsync({
            type: 'org.racehorse.GooglePayViewTokenEvent',
            payload: { tokenId, tokenServiceProvider },
          })
          .then(event => event.payload.isOpened)
      ),

    pushTokenize: request =>
      uiScheduler.schedule(() =>
        eventBridge
          .requestAsync({ type: 'org.racehorse.GooglePayPushTokenizeEvent', payload: request })
          .then(event => event.payload.tokenId)
      ),

    tokenize: request =>
      uiScheduler.schedule(() =>
        eventBridge
          .requestAsync({ type: 'org.racehorse.GooglePayTokenizeEvent', payload: request })
          .then(event => event.payload.tokenId)
      ),

    requestSelectToken: (tokenId, tokenServiceProvider) =>
      uiScheduler.schedule(() =>
        eventBridge
          .requestAsync({
            type: 'org.racehorse.GooglePayRequestSelectTokenEvent',
            payload: { tokenId, tokenServiceProvider },
          })
          .then(noop)
      ),

    requestDeleteToken: (tokenId, tokenServiceProvider) =>
      uiScheduler.schedule(() =>
        eventBridge
          .requestAsync({
            type: 'org.racehorse.GooglePayRequestDeleteTokenEvent',
            payload: { tokenId, tokenServiceProvider },
          })
          .then(noop)
      ),

    createWallet: () =>
      uiScheduler.schedule(() =>
        eventBridge.requestAsync({ type: 'org.racehorse.GooglePayCreateWalletEvent' }).then(noop)
      ),

    subscribe: listener =>
      eventBridge.subscribe('org.racehorse.GooglePayDataChangedEvent', () => {
        listener();
      }),
  };
}
