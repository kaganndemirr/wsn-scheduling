package dk.smlaursen.TSNCF.scheduler;

public class IEEE802154 {
    private int macTsTxOffset;

    private int macTsMaxTx;

    private int macTsTxAckDelay;

    private int macTsMaxAck;

    private double ratio;

    public IEEE802154(){
        this.macTsTxOffset = 2120;
        this.macTsMaxTx = 4256;
        this.macTsTxAckDelay = 1000;
        this.macTsMaxAck = 2400;
        this.ratio = 1;
    }

    public IEEE802154(int controlPacketSize){
        int tempmacTsMaxTx = macTsMaxAck;
        macTsMaxTx = (int) (0.008 * controlPacketSize) / 250;
        ratio = macTsMaxAck / tempmacTsMaxTx;
        macTsTxOffset = (int) (macTsTxOffset * ratio);
        macTsTxAckDelay = (int) (macTsTxAckDelay * ratio);
        macTsMaxAck = (int) (macTsMaxAck * ratio);
        
    }

    public int getMacTsTxOffset() {
        return macTsTxOffset;
    }

    public int getMacTsTxAckDelay() {
        return macTsTxAckDelay;
    }

    public int getMacTsMaxAck() {
        return macTsMaxAck;
    }

    public int getMacTsMaxTx() {
        return macTsMaxTx;
    }
}
