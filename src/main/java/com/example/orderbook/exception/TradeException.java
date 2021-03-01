package  com.example.orderbook.exception;

public class TradeException extends RuntimeException
{
    public TradeException(String s)
    {
        // Call constructor of parent Exception
        super(s);
    }
}