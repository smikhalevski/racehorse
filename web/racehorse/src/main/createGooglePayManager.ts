import { EventBridge } from './createEventBridge';

export enum GooglePayTokenState {
  UNTOKENIZED = 1,
  PENDING = 2,
  NEEDS_IDENTITY_VERIFICATION = 3,
  SUSPENDED = 4,
  ACTIVE = 5,
  FELICA_PENDING_PROVISIONING = 6,
}

export enum GooglePayCardNetwork {
  AMEX = 1,
  DISCOVER = 2,
  MASTERCARD = 3,
  VISA = 4,
  INTERAC = 5,
  PRIVATE_LABEL = 6,
  EFTPOS = 7,
  MAESTRO = 8,
  ID = 9,
  QUICPAY = 10,
  JCB = 11,
  ELO = 12,
  MIR = 13,
}

export enum GooglePayTokenServiceProvider {
  AMEX = 2,
  MASTERCARD = 3,
  VISA = 4,
  DISCOVER = 5,
  EFTPOS = 6,
  INTERAC = 7,
  OBERTHUR = 8,
  PAYPAL = 9,
  JCB = 13,
  ELO = 14,
  GEMALTO = 15,
  MIR = 16,
}

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
  name: string;
  address1: string;
  address2: string;
  locality: string; // Mountain View
  administrativeArea: string; // CA
  countryCode: string;
  postalCode: string;
  phoneNumber: string;
}

export interface GooglePayTokenStatus {
  tokenState: GooglePayTokenState;
  isSelected: boolean;
}

export interface GooglePayPushTokenizeRequest {
  opaquePaymentCard: String;
  displayName: String;
  lastFour: String;
  network: GooglePayCardNetwork;
  tokenServiceProvider: GooglePayTokenServiceProvider;
  userAddress: GooglePayUserAddress;
}

export interface GooglePayManager {
  /**
   * Get the ID of the active wallet, or `null` if there's no active wallet.
   *
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
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet?authuser=1#gettokenstatus)
   */
  getTokenStatus(
    tokenServiceProvider: GooglePayTokenServiceProvider,
    tokenId: string
  ): Promise<GooglePayTokenStatus | null>;

  /**
   * Returns the name of the current Google Pay environment, for example: PROD, SANDBOX, or DEV.
   *
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
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet?authuser=1#getstablehardwareid)
   */
  getStableHardwareId(): Promise<string>;

  /**
   * Get all tokens available in the wallet.
   *
   * The API only returns token details for tokens with metadata matching your app package name. You can check if your
   * tokens have this linking by tapping on the card in the Google Wallet app to see the card details view.
   *
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet?authuser=1#getactivewalletid)
   */
  listTokens(): Promise<GooglePayTokenInfo[]>;

  /**
   * Searches the wallet for a token and returns `true` if found, or `false` otherwise.
   *
   * @returns `true` if it finds a token with same last four FPAN digits as the identifier, as well as matches on the
   * other fields. False positives may be returned since the last four FPAN digits are not necessarily unique among
   * tokens.
   *
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
   * @returns `true` if Google Pay app was opened, or `false` otherwise.
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet?authuser=1#viewtoken)
   */
  viewToken(issuerTokenId: string, tokenServiceProvider: GooglePayTokenServiceProvider): Promise<boolean>;

  /**
   * Starts the push tokenization flow in which the issuer provides most or all card details needed for Google Pay to
   * get a valid token. Tokens added using this method are added to the active wallet.
   *
   * @returns The token ID.
   * @see [Android Push Provisioning API](https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations?authuser=1#push_provisioning_operations)
   * @see [Sequence diagrams for Android Push Provisioning](https://developers.google.com/pay/issuers/apis/push-provisioning/android/integration-steps)
   */
  pushTokenize(request: GooglePayPushTokenizeRequest): Promise<string>;

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
  subscribe(listener: () => void): () => void;
}

/**
 * Manages tokenized cards in Google Pay.
 *
 * [Reading wallet state.](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet)
 *
 * @param eventBridge The underlying event bridge.
 */
export function createGooglePayManager(eventBridge: EventBridge): GooglePayManager {
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

    viewToken: (issuerTokenId, tokenServiceProvider) =>
      eventBridge
        .requestAsync({
          type: 'org.racehorse.GooglePayViewTokenEvent',
          payload: { issuerTokenId, tokenServiceProvider },
        })
        .then(event => event.payload.opened),

    pushTokenize: request =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.GooglePayPushTokenizeEvent', payload: request })
        .then(event => event.payload.tokenId),

    subscribe: listener =>
      eventBridge.subscribe('org.racehorse.GooglePayDataChangedEvent', () => {
        listener();
      }),
  };
}
