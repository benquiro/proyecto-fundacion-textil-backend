package cl.textilcircular.api.outreach;

public enum ContentType {
    /** Short post; needs a body. */
    TEXT,
    /** Poster or photo; needs an image. */
    IMAGE,
    /** Long read; needs a title and a body. */
    ARTICLE,
    /** Workshop, fair or meeting; needs a title and a date, optionally a place. */
    EVENT
}
