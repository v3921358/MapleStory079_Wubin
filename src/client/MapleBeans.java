/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

/**
 *
 * @author wubin
 */
public class MapleBeans {

    private final int number;
    private final int type;
    private final int pos;

    public MapleBeans(int pos, int type, int number) {
        this.pos = pos;
        this.number = number;
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public int getNumber() {
        return number;
    }

    public int getPos() {
        return pos;
    }

    public enum BeansType {
        開始打豆豆(0x00),
        暫停打豆豆(0x01),
        顏色求進洞(0x03),
        進洞旋轉(0x04),
        獎勵豆豆效果(0x05),
        未知效果(0x06),
        黃金狗(0x07),
        獎勵豆豆效果B(0x08),
        領獎npc(0x09);

        final byte type;

        BeansType(int type) {
            this.type = (byte) type;
        }

        public byte getType() {
            return type;
        }
    }
}
