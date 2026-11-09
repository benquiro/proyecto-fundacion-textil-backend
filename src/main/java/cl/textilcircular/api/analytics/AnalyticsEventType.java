package cl.textilcircular.api.analytics;

public enum AnalyticsEventType {
    /** Someone searched or filtered the directory. */
    DIRECTORY_SEARCH,
    /** A business detail was opened. */
    BUSINESS_VIEW,
    /** A product detail was opened. */
    PRODUCT_VIEW,
    /** The contact button of a business was used. */
    CONTACT_CLICK
}
