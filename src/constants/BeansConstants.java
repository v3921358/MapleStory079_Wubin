/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package constants;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 *
 * @author wubin
 */
public class BeansConstants {

    private static BeansConstants instance = null;
    private static boolean CANLOG;
    private Properties itempb_cfg = new Properties();
    private final String 豆豆装备[];
    private final String 豆豆坐骑[];
    private final String 消耗品[];
    private final int 海洋帽子几率;//（百分比 = 固定值）
    private final String[] 黃金狗幾率;// = （1,2 = 出的概率）
    private final String 小白怪[];// > （1,2 = 出的概率  3,4 = 中奖概率）
    private final String 大白怪[];// > （1,2 = 出的概率  3,4 = 中奖概率）
    private final String 紫色怪[];// > （1,2 = 出的概率  3,4 = 中奖概率）
    private final String 粉色怪[];// > （1,2 = 出的概率  3,4 = 中奖概率）
    private final String 飛俠[];// > （1,2 = 出的概率  3,4 = 中奖概率）
    private final String 海盜[];// > （1,2 = 出的概率  3,4 = 中奖概率）
    private final String 法師[];// > （1,2 = 出的概率  3,4 = 中奖概率）
    private final String 戰士[];// > （1,2 = 出的概率  3,4 = 中奖概率）
    private final String 弓箭手[];// > （1,2 = 出的概率  3,4 = 中奖概率）
    private final String 女皇[];// > （1,2 = 出的概率  3,4 = 中奖概率）
    private final String 白怪獎勵[];// > （1,2 经验    3,4药水）
    private final String 色怪獎勵[];// > （1,2 经验    3,4药水  5,6装备）
    private final String 五職業獎勵[];// > （1,2 经验    3,4药水  5,6装备 7,8坐骑）
    private final String 女皇獎勵[];// > （1,2 经验    3,4药水  5,6装备 7,8坐骑）
    private final int 力度搞假;//大概范围在500-1000左右
    private final int 豆豆獎勵範圍;//1-10 * 豆豆奖励范围 = 给与奖励

    public BeansConstants() {
        try (InputStreamReader is = new FileReader("beans.properties")) {
            itempb_cfg.load(is);
        } catch (Exception e) {
            System.err.println(e);
        }
        豆豆装备 = itempb_cfg.getProperty("ddzb").split(",");
        豆豆坐骑 = itempb_cfg.getProperty("ddzq").split(",");
        消耗品 = itempb_cfg.getProperty("xhp").split(",");
        海洋帽子几率 = Integer.parseInt(itempb_cfg.getProperty("hymzjl"));
        黃金狗幾率 = itempb_cfg.getProperty("hjgjl").split(",");
        大白怪 = itempb_cfg.getProperty("dbg").split(",");
        小白怪 = itempb_cfg.getProperty("xbg").split(",");
        紫色怪 = itempb_cfg.getProperty("zsg").split(",");
        粉色怪 = itempb_cfg.getProperty("fsg").split(",");
        飛俠 = itempb_cfg.getProperty("fx").split(",");
        海盜 = itempb_cfg.getProperty("hd").split(",");
        法師 = itempb_cfg.getProperty("fs").split(",");
        戰士 = itempb_cfg.getProperty("zs").split(",");
        弓箭手 = itempb_cfg.getProperty("gjs").split(",");
        女皇 = itempb_cfg.getProperty("nh").split(",");
        白怪獎勵 = itempb_cfg.getProperty("bgjl").split(",");
        色怪獎勵 = itempb_cfg.getProperty("sgjl").split(",");
        五職業獎勵 = itempb_cfg.getProperty("wzyjl").split(",");
        女皇獎勵 = itempb_cfg.getProperty("nhjl").split(",");
        力度搞假 = Integer.parseInt(itempb_cfg.getProperty("ldgj"));
        豆豆獎勵範圍 = Integer.parseInt(itempb_cfg.getProperty("ddjlfw"));

    }

    public int get豆豆獎勵範圍() {
        return 豆豆獎勵範圍;
    }

    public int get力度搞假() {
        return 力度搞假;
    }

    public String[] get白怪獎勵() {
        return 白怪獎勵;
    }

    public String[] get色怪獎勵() {
        return 色怪獎勵;
    }

    public String[] get五職業獎勵() {
        return 五職業獎勵;
    }

    public String[] get女皇獎勵() {
        return 女皇獎勵;
    }

    public String[] get大白怪() {
        return 大白怪;
    }

    public String[] get小白怪() {
        return 小白怪;
    }

    public String[] get紫色怪() {
        return 紫色怪;
    }

    public String[] get粉色怪() {
        return 粉色怪;
    }

    public String[] get飛俠() {
        return 飛俠;
    }

    public String[] get海盜() {
        return 海盜;
    }

    public String[] get法師() {
        return 法師;
    }

    public String[] get戰士() {
        return 戰士;
    }

    public String[] get弓箭手() {
        return 弓箭手;
    }

    public String[] get女皇() {
        return 女皇;
    }

    public String[] get豆豆装备() {
        return 豆豆装备;
    }

    public String[] get豆豆坐骑() {
        return 豆豆坐骑;
    }

    public String[] get消耗品() {
        return 消耗品;
    }

    public int get海洋帽子几率() {
        return 海洋帽子几率;
    }

    public String[] get黄金狗几率() {
        return 黃金狗幾率;
    }

    public boolean isCANLOG() {
        return CANLOG;
    }

    public void setCANLOG(boolean CANLOG) {
        BeansConstants.CANLOG = CANLOG;
    }

    public static BeansConstants getInstance() {
        if (instance == null) {
            instance = new BeansConstants();
        }
        return instance;
    }
}
