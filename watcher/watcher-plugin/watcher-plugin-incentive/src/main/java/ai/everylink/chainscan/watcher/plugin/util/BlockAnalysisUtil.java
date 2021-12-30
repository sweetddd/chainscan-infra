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

    public static long getBlockHeight(String storage){
        String sub = storage.substring(2);
        ScaleCodecReader rdr  = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint32();
    }

    public static long getDifficulty(String storage){
        String sub = storage.substring(2 + 16);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint32();
    }

    public static long getBlockedFee(String storage){
        String sub = storage.substring(2 + 16 * 3);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint32();
    }

    public static long getStartTime(String storage){
        String sub = storage.substring(2 + 16 * 5);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint32();
    }

    public static String getBlockHash(String storage){
        String sub = storage.substring(2 + 16 * 6);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return new Hash256(rdr.readUint256()).toString();
    }

    public static int getTransactionCount(String storage){
        String sub = storage.substring(2 + 16 * 10);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint16();
    }

    public static String getTransactionHash(String storage){
        String sub = storage.substring(storage.length() - 64);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return new Hash256(rdr.readUint256()).toString();
    }

    public static long getSellerFee(String storage){
        String sub = storage.substring(storage.length() - 64 - 32);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint32();
    }

    public static long getBuyerFee(String storage){
        String sub = storage.substring(storage.length() - 64 - 64);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint32();
    }

    public static int getPrice(String storage){
        String sub = storage.substring(storage.length() - 64 - 96);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint16();
    }

    public static int getAmount(String storage){
        String sub = storage.substring(storage.length() - 64 - 128);
        ScaleCodecReader rdr = new ScaleCodecReader(readMessage(sub));
        return rdr.readUint16();
    }

    public static String getSellerAddress(String storage){
        String sub = storage.substring(storage.length() - 64 - 168, storage.length() - 64 - 128);
        return "0x" + sub;
    }

    public static String getBuyerAddress(String storage){
        String sub = storage.substring(storage.length() - 64 - 208, storage.length() - 64 - 168);
        return "0x" + sub;
    }

    public static String getTransactionType(String storage){
        String sub = storage.substring(storage.length() - 64 - 230, storage.length() - 64 - 208);
        return hexStringToString(sub);
    }

    public static String getCoinSymbol(String storage){
        String sub = storage.substring(0, storage.length() - 64 - 230);
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
