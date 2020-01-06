/**
 * Response status codes for a registration request.
 */
enum RegistrationResponseStatusCode {
    /** The registration procedure completes successfully */
    OK,
    /** The nickName is empty */
    INVALID_NICK_ERROR,
    /** The chosen nickName is already registered to the service */
    NICK_ALREADY_REGISTERED_ERROR,
    /** The password is invalid (a valid password must have at least 4 characters) */
    INVALID_PASSWORD_ERROR,
    /** The service encountered an internal error */
    INTERNAL_ERROR
}
