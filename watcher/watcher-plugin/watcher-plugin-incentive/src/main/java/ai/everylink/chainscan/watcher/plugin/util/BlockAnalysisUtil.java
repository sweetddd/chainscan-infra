package ai.everylink.chainscan.watcher.plugin.util;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.types.Hash256;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * @Author
 * @Description 区块数据解析工具
 * @Date 2021/10/13 18:00
 **/
public class BlockAnalysisUtil {

    public static int getBlockHeight(String storage){
        String sub = storage.substring(2, 2 + 16);
        ScaleCodecReader rdr  = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint16();
    }

    public static long getDifficulty(String storage){
        String sub = storage.substring(2 + 16, 2 + 16 * 2);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint32();
    }

    public static long getBlockedFee(String storage){
        String sub = storage.substring(2 + 16 * 2, 2 + 16 * 3);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint32();
    }

    public static long getStartTime(String storage){
        String sub = storage.substring(2 + 16 * 3, 2 + 16 * 4);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint32();
    }

    public static String getBlockHash(String storage){
        String sub = storage.substring(2 + 16 * 4, 2 + 16 * 8);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return new Hash256(rdr.readUint256()).toString();
    }

    public static int getTransactionCount(String storage){
        String sub = storage.substring(2 + 16 * 8, 2 + 16 * 9);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint16();
    }

    public static String getTransactionHash(String storage){
        String sub = storage.substring(104 + 16 * 5, 104 + 16 * 5 + 64);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return new Hash256(rdr.readUint256()).toString();
    }

    public static long getSellerFee(String storage){
        String sub = storage.substring(104 + 16 * 4, 104 + 16 * 5);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint32();
    }

    public static long getBuyerFee(String storage){
        String sub = storage.substring(104 + 16 * 3, 104 + 16 * 4);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint32();
    }

    public static int getPrice(String storage){
        String sub = storage.substring(104 + 16 * 2, 104 + 16 * 3);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint16();
    }

    public static int getAmount(String storage){
        String sub = storage.substring(104 + 16 ,104 + 16 * 2);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint16();
    }

    public static String getSellerAddress(String storage){
        String sub = storage.substring(16 + 24 + 40, 16 + 24 + 80);
        return "0x" + sub;
    }

    public static String getBuyerAddress(String storage){
        String sub = storage.substring(16 + 24, 16 + 24 + 40);
        return "0x" + sub;
    }

    public static String getTransactionType(String storage){
        String sub = storage.substring(16 +2 , 16 + 24);
        return hexStringToString(sub);
    }

    public static String getCoinSymbol(String storage){
        String sub = storage.substring(0, 16);
        return hexStringToString(sub);
    }

    public static String hexStringToString(String s) {
        if (s == null || s.equals("")) {
            return null;
        }
        s = s.replace(" ", "");
        byte[] baKeyword = new byte[s.length() / 2];
        for (int i = 0; i < baKeyword.length; i++) {
            try {
                baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            s = new String(baKeyword, "UTF-8");
            new String();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return s;
    }


    public static byte[] readMessage(String input) {
        byte[] msg = new byte[0];
        try {
            msg = Hex.decodeHex(input);
        } catch (DecoderException e) {
            e.printStackTrace();
        }
        return msg;
    }

}
