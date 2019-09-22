package win.oakcsclub;

/**
 * for when the user does something wrong
 */
public class ClientErrorException extends RuntimeException {
    public ClientErrorException(String message){ super(message); }
}
